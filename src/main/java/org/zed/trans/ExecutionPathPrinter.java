package org.zed.trans;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExecutionPathPrinter {

    public static String addPrintStmt(String code){
        String c1 = addPrintStmtAtMethodBegin(code);
        String c2 = addPrintStmtForIfStmt(c1);
        String c3 = addPrintStmtForAssignStmt(c2);
        String c4 = addPrintStmtForVariableDeclarationExpr(c3);
        String c5 = addPrintStmtForReturnStmt(c4);
//        String c6 = addPrintStmtForForLoopStmt(c5);
        return c5;
    }

    public static String addPrintStmtForIfStmt(String code){
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();

        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public IfStmt visit(IfStmt ifStmt, Void arg) {
                // 1. 首先处理嵌套的if语句（递归处理then和else部分）
                if (ifStmt.getThenStmt() instanceof IfStmt) {
                    ifStmt.setThenStmt(visit(ifStmt.getThenStmt().asIfStmt(), arg));
                }
                else if (ifStmt.getThenStmt() instanceof BlockStmt) {
                    BlockStmt thenBlock = ifStmt.getThenStmt().asBlockStmt();
                    NodeList<Statement> newStatements = new NodeList<>();
                    for (Statement stmt : thenBlock.getStatements()) {
                        if (stmt instanceof IfStmt) {
                            newStatements.add(visit(stmt.asIfStmt(), arg));
                        } else {
                            newStatements.add(stmt);
                        }
                    }
                    thenBlock.setStatements(newStatements);
                }

                 //处理else部分中的嵌套if
//                if (ifStmt.getElseStmt().isPresent()) {
//                    Statement elseStmt = ifStmt.getElseStmt().get();
//                    if (elseStmt instanceof IfStmt) {
//                        ifStmt.setElseStmt(visit(elseStmt.asIfStmt(), arg));
//                    }
//                    else if (elseStmt instanceof BlockStmt) {
//                        BlockStmt elseBlock = elseStmt.asBlockStmt();
//                        NodeList<Statement> newStatements = new NodeList<>();
//                        for (Statement stmt : elseBlock.getStatements()) {
//                            if (stmt instanceof IfStmt) {
//                                newStatements.add(visit(stmt.asIfStmt(), arg));
//                            } else {
//                                newStatements.add(stmt);
//                            }
//                        }
//                        elseBlock.setStatements(newStatements);
//                    }
//                }
                // 2. 然后处理当前if语句的插桩（只处理最外层的if-elseif-else链）
                return handleIfElseChain(ifStmt);
            }
        }, null);

        return cu.toString();
    }

    public static String addPrintStmtForForLoopStmt(String code) {
        StringBuilder result = new StringBuilder();
        String[] lines = code.split("\n");

        Pattern forPattern = Pattern.compile("for\\((.*?);(.*?);(.*?)\\)");
        boolean insideForLoop = false;
        String loopCondition = "";

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine.trim();

            Matcher forMatcher = forPattern.matcher(line);

            // 检测 for 循环语句
            if (forMatcher.find()) {
                insideForLoop = true;
                loopCondition = forMatcher.group(2).trim();
                result.append(rawLine).append("\n");

                // 添加进入循环的变量状态打印
                Matcher varMatcher = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b").matcher(loopCondition);
                while (varMatcher.find()) {
                    String var = varMatcher.group(1);
                    if (!var.equals("true") && !var.equals("false")) {
                        result.append("System.out.println(\"" + var + " = \" + " + var + ");\n");
                    }
                }
                result.append("System.out.println(\"Entering loop with condition: ")
                        .append(loopCondition)
                        .append(" is evaluated as: \" + (")
                        .append(loopCondition)
                        .append("));\n");
                continue;
            }

            // 检测循环结束括号
            if (insideForLoop && line.equals("}")) {
                result.append(rawLine).append("\n");
                result.append("System.out.println(\"Exiting loop, condition no longer holds: ")
                        .append(loopCondition)
                        .append(" is evaluated as: \" + (")
                        .append(loopCondition)
                        .append("));\n");
                insideForLoop = false;
                continue;
            }

            // 默认情况直接追加原始代码行
            result.append(rawLine).append("\n");
        }
        return result.toString();
    }


    public static String addPrintStmtForAssignStmt(String code){
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        // 使用 ModifierVisitor 遍历并修改 AST
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(ExpressionStmt stmt, Void arg) {
                Expression expr = stmt.getExpression();

                // 处理赋值语句（如 x = 5;）
                if (expr.isAssignExpr()) {
                    AssignExpr assignExpr = expr.asAssignExpr();
                    String op = assignExpr.getOperator().asString();
                    String varName = assignExpr.getTarget().toString();
                    Expression value = assignExpr.getValue();
                    if(op.equals("+=")){
                        value = new BinaryExpr(new NameExpr(varName), value, BinaryExpr.Operator.PLUS);
                    }
                    //要把强制类型转换去掉,避免在validation环节将类型转换误认为一个变量
                    String valueStr = value.toString().replace("(char)","").replace("(long)","")
                            .replace("(int)","").replace("(double)","").replace("(float)","");
                    EnclosedExpr enclosedExpr = new EnclosedExpr(new NameExpr(varName)); //避免 varName含有&&、||、%等操作符，导致拼接字符串时报错
                    // 生成打印语句（格式：System.out.println("变量名: " + 变量名 + ", 当前值: " + 值);）
                    Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                            new NameExpr("System.out"),
                            "println",
                            NodeList.nodeList(new BinaryExpr(
                                    new StringLiteralExpr(varName + " = " + valueStr + ", current value of " + varName + ": "),
                                    enclosedExpr,
                                    BinaryExpr.Operator.PLUS
                            ))
                    ));

                    //找到父blockStatement
                    Optional<Node> parentNode = stmt.getParentNode();
                    if(parentNode.isEmpty()){
                        return super.visit(stmt, arg);
                    }
                    if(parentNode.get() instanceof BlockStmt){
                        int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                        ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,printStmt);
                    }else if(parentNode.get() instanceof SwitchEntry){
                        int index = ((SwitchEntry) parentNode.get()).getStatements().indexOf(stmt);
                        ((SwitchEntry) parentNode.get()).addStatement(index+1,printStmt);
                    }
                }
                return super.visit(stmt, arg);
            }
        }, null);

        // 返回插桩后的代码
        return cu.toString();
    }

    public static String addPrintStmtForVariableDeclarationExpr(String code){

        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        // 使用 ModifierVisitor 遍历并修改 AST
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(ExpressionStmt stmt, Void arg) {
                Expression expr = stmt.getExpression();
                // 处理变量声明并初始化（如 int x = 5;）
                if (expr.isVariableDeclarationExpr()) {
                    VariableDeclarationExpr varDecl = expr.asVariableDeclarationExpr();
                    // BlockStmt block = new BlockStmt();
                    // block.addStatement(stmt);

                    // 为每个变量生成打印语句
                    varDecl.getVariables().forEach(var -> {
                        if (var.getInitializer().isPresent()) {
                            String varName = var.getNameAsString();
//                            String op = var.getInitializer().get().isAssignExpr() ? var.getInitializer().get().asAssignExpr().getOperator().asString() : "=";
                            String value = var.getInitializer().get().isAssignExpr() ? var.getInitializer().get().asAssignExpr().getValue().toString() : var.getInitializer().get().toString();
                            Expression val = new EnclosedExpr(var.getInitializer().get());
                            Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                                    new NameExpr("System.out"),
                                    "println",
                                    NodeList.nodeList(new BinaryExpr(
                                            new StringLiteralExpr(varName + " " + "=" + " " + value + ", current value of " + varName + ": "),
                                            new NameExpr(val.toString()),
                                            BinaryExpr.Operator.PLUS
                                    ))
                            ));

                            Optional<Node> parentNode = stmt.getParentNode();
//                            System.out.println(parentNode.toString());

                            if(parentNode.isPresent() && parentNode.get() instanceof BlockStmt){
                                int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                                ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,printStmt);
                            }
                        }
                    });
                }
                return super.visit(stmt, arg);
            }
        }, null);

        return cu.toString();
    }

    public static String addPrintStmtAtMethodBegin(String code){
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        // 使用 ModifierVisitor 遍历并修改 AST
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(MethodDeclaration md, Void arg) {
                if(md.isStatic() && !md.getNameAsString().equals("main")){
                    for(Parameter param : md.getParameters()) {
                        String type = param.getType().toString();
                        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                                new NameExpr("System.out"),
                                "println",
                                NodeList.nodeList(new BinaryExpr(
                                        new StringLiteralExpr("Function input " + type + " " + "parameter " + param.getName() + " = "),
                                        new NameExpr(param.getNameAsString()),
                                        BinaryExpr.Operator.PLUS
                                ))
                        ));
                        // 将打印语句插入到方法体的开头
                        if (md.getBody().isPresent()) {
                            BlockStmt body = md.getBody().get();
                            body.addStatement(0, printStmt);
                        }
                    }
                }
                return super.visit(md, arg);
            }
        }, null);

        return cu.toString();
    }

    public static String addPrintStmtForReturnStmt(String code) {
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(ReturnStmt stmt, Void arg) {
                Optional<Node> parentNode = stmt.getParentNode();
                if(parentNode.isEmpty()){
                    return super.visit(stmt, arg);
                }
                if(parentNode.get() instanceof BlockStmt){
                    //return expr;
                    //这里插桩的是 return_value = expr, current value of return_value: expr
                    int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                    Statement printStmt = generatePathPrintStmt(stmt);
                    ((BlockStmt) parentNode.get()).addStatement(index,printStmt);
                }else if(parentNode.get() instanceof SwitchEntry){
                    //return expr;
                    //这里插桩的是 return_value = expr, current value of return_value: expr
                    int index = ((SwitchEntry) parentNode.get()).getStatements().indexOf(stmt);
                    Statement printStmt = generatePathPrintStmt(stmt);
                    ((SwitchEntry) parentNode.get()).addStatement(index,printStmt);
                }
                return super.visit(stmt, arg);
            }
        }, null);
        return cu.toString();
    }
    public static BlockStmt generatePathPrintBlock(IfStmt ifStmt){
        //0. 没有用{}的先加{}
        Statement thenStmt = ifStmt.getThenStmt();
        if (!thenStmt.isBlockStmt()) {
            BlockStmt newBlock = new BlockStmt();
            newBlock.addStatement(thenStmt);
            ifStmt.setThenStmt(newBlock);
        }
        //1. 获取 condition
        Expression condition = ifStmt.getCondition();
        condition = new EnclosedExpr(condition);
        //2. 创建插桩语句
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Evaluating if condition: " + condition + " is evaluated as: "),
                        condition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        //3. 插入到ifStmt对应的thenStmt中
        thenStmt = ifStmt.getThenStmt();
        BlockStmt newBlock = thenStmt.asBlockStmt();
        newBlock.addStatement(0,printStmt);
        return newBlock;
    }
    public static BlockStmt generatePathPrintBlock(WhileStmt whileStmt){
        //1. 确保while 的 body 被 {} 围起来
        if(!whileStmt.getBody().isBlockStmt()){
            Statement body = whileStmt.getBody();
            BlockStmt blockStmt = new BlockStmt();
            blockStmt.addStatement(body);
            whileStmt.setBody(blockStmt);
        }
        //2. 获取condition，构造print statement
        Expression condition = whileStmt.getCondition();
        condition = new EnclosedExpr(condition);
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Entering loop with condition: " + condition + " is evaluated as: "),
                        condition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        //3. 把print statement 插入到while body里
        BlockStmt printBlock = whileStmt.getBody().asBlockStmt();
        printBlock.addStatement(0,printStmt);
        return printBlock;
    }
    public static Statement generatePathPrintStmt(ReturnStmt returnStmt){
        // 获取 return 的表达式（如果有）
        Optional<Expression> returnExpr = returnStmt.getExpression();
        //如果是常量，打印语句变量名 固定为 return_value
        String returnValueName = returnExpr.get().toString();

        // 2. 生成打印语句
        EnclosedExpr enclosedExpr = new EnclosedExpr(new NameExpr(returnValueName));
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("return_value = " + returnValueName + " , current value of return_value : "),
                        enclosedExpr,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        // 3. 将打印语句和原 return 语句包装成 BlockStmt
        return printStmt;
    }
    private static IfStmt handleIfElseChain(IfStmt ifStmt) {
        List<Expression> preIfConditions = new ArrayList<>();
        //1. 对第一个IfStmt生成print block
        BlockStmt pb = generatePathPrintBlock(ifStmt);
        ifStmt.setThenStmt(pb);

        //2. 把当前condition记录一下
        Expression condition = ifStmt.getCondition();
        condition = new EnclosedExpr(condition);  //condition 都用 括号 包起来
        preIfConditions.add(condition);

        //3. 如果有 else if，迭代处理 else if, 并记录历史 condition
        //迭代的过程可以看作是一个链表的双指针遍历
        Optional<Statement> childElseStmt = ifStmt.getElseStmt();
        IfStmt parentIfStmt = ifStmt;
        while (childElseStmt.isPresent() && childElseStmt.get().isIfStmt()) {
            //记录当前 condition
            Expression c = childElseStmt.get().asIfStmt().getCondition();
            c = new EnclosedExpr(c);
            preIfConditions.add(c);
            //生成 pathPrintBlock
            BlockStmt pathPrintBlock = generatePathPrintBlock(childElseStmt.get().asIfStmt());
            //替换掉childElseStmt的thenStmt
            IfStmt elseIfStmt = childElseStmt.get().asIfStmt();
            elseIfStmt.setThenStmt(pathPrintBlock);
            //父 IfStmt 更新子 ElseStmt 引用
            parentIfStmt.setElseStmt(elseIfStmt);
            //调整父子指针，以便进行循环
            parentIfStmt = parentIfStmt.getElseStmt().get().asIfStmt();
            childElseStmt = parentIfStmt.getElseStmt();
        }
        //4. 处理最后的 else 语句
        //4.1 没有elseStmt时，初始化一个，不是Block，改造成Block
        if(childElseStmt.isEmpty()) {
            BlockStmt b = new BlockStmt();
            childElseStmt = Optional.of(b);
        }
        if(!childElseStmt.get().isBlockStmt()) {
            BlockStmt b = new BlockStmt();
            b.addStatement(childElseStmt.get());
            childElseStmt = Optional.of(b);
        }
        BlockStmt elseBlock = childElseStmt.orElseThrow().asBlockStmt();
        //5.2 用 || 连接 所有 ifConditions并取反 作为else 的Condition
        // 5.2.1 合并条件： (cond1) || (cond2) || ...
        Expression combined = preIfConditions.get(0);
        for (int i = 1; i < preIfConditions.size(); i++) {
            combined = new BinaryExpr(combined, preIfConditions.get(i), BinaryExpr.Operator.OR);
        }
        //5.2.2 取反，UnaryExpr 是一元表达式，LOGICAL_COMPLEMENT是操作符 （ ! ）
        if(preIfConditions.size()>1){
            combined = new EnclosedExpr(combined);
        }
        Expression elseCondition = new UnaryExpr(combined, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
        Statement printElseStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Evaluating if condition: " + elseCondition + " is evaluated as: "),
                        elseCondition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        //5.3 将打印语句插入到else里
        elseBlock.addStatement(0,printElseStmt);
        //5.4 插入更新好的 else 语句块到 上一个 IfStmt中
        parentIfStmt.setElseStmt(elseBlock);

        return ifStmt;
    }

    public static void main(String[] args) {
        String dir = "resources/dataset/someBench/";
        String testFileName = "MyPower";
        String testFileNameJava = testFileName+".java";
        String testFilePath = dir + "/" + testFileNameJava;

        String pureCode = TransFileOperator.file2String(testFilePath);
        String targetCode = addPrintStmt(pureCode);
//        String targetCode = addPrintStmtForReturnStmt(pureCode);
//        String targetCode = addPrintStmtAtMethodBegin(pureCode);
//        String targetCode = addPrintStmtForForLoopStmt(pureCode);
//        String targetCode = addPrintStmtForAssignStmt(pureCode);
        System.out.println(targetCode);
    }
}
