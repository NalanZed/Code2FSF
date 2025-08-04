package org.zed.trans;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.zed.log.LogManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionPathPrinter {

    public static String addPrintStmt(String code){
        String c0 = removeAllComments(code);
        String c1 = addPrintStmtAtMethodBegin(c0);
        String c2 = addPrintStmtForIfStmt(c1);
        String c3 = addPrintStmtForAssignStmt(c2);
        String c4 = addPrintStmtForVariableDeclarationExpr(c3);
        String c5 = addPrintStmtForReturnStmt(c4);
        String c6 = addPrintStmtForForLoopStmt(c5);
        String c7 = addPrintForWhileLoopStmt(c6);
        return c7;
    }

    public static String removeAllComments(String code){
        CompilationUnit cu = StaticJavaParser.parse(code);
        cu.getAllContainedComments().forEach(Comment::remove);
        return cu.toString();
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
                // 2. 然后处理当前if语句的插桩（只处理最外层的if-elseif-else链）
                return handleIfElseChain(ifStmt);
            }
        }, null);

        return cu.toString();
    }

    public static String addPrintStmtForForLoopStmt(String code) {
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(ForStmt forStmt, Void arg) {
                // 确保 for 的 body 被 {} 包围
                if (!forStmt.getBody().isBlockStmt()) {
                    Statement body = forStmt.getBody();
                    BlockStmt blockStmt = new BlockStmt();
                    blockStmt.addStatement(body);
                    forStmt.setBody(blockStmt);
                }
                // 在for循环上方打印初始化语句
                List<Statement> initialStmts = generateInitialStmtsOfForLoop(forStmt);
                if(!initialStmts.isEmpty()) {
                    Optional<Node> parentNode = forStmt.getParentNode();
                    if(parentNode.isEmpty()) {
                        return super.visit(forStmt, arg);
                    }
                    for(Statement s : initialStmts){
                        if(parentNode.get() instanceof BlockStmt) {
                            int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(forStmt);
                            ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index, s);
                        }
                    }
                }

                //在循环体内打印 update 语句，这样可以获取到赋值
                List<Statement> updateStmts = generateUpdateStmtOfForLoop(forStmt);
                if(!updateStmts.isEmpty()) {
                    for(Statement s : updateStmts){
                        int length = forStmt.getBody().asBlockStmt().getStatements().size();
                        forStmt.getBody().asBlockStmt().addStatement(length,s);
                    }
                }
                Optional<Node> parentNode = forStmt.getParentNode();
                if(parentNode.isEmpty()) {
                    return super.visit(forStmt, arg);
                }
                Statement enterLoopStmt = generateEnteringLoopPrintStmt(forStmt);
                //在循环体内打印Entering loop condition
                forStmt.getBody().asBlockStmt().addStatement(0,enterLoopStmt);
                //打印退出循环的语句 Exiting loop condition
                Statement exitLoopStmt = generateExitingLoopPrintStmt(forStmt);
                if(parentNode.get() instanceof BlockStmt) {
                    int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(forStmt);
                    ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,exitLoopStmt);
//                    ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index,enterLoopStmt);
                }
                return super.visit(forStmt, arg);
            }
        },null);
        return cu.toString();
    }

    public static String addPrintForWhileLoopStmt(String code) {
        CompilationUnit cu = new JavaParser().parse(code).getResult().get();
        // 使用 ModifierVisitor 遍历并修改 AST
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(WhileStmt whileStmt, Void arg) {
                // 确保 while 的 body 被 {} 包围
                if (!whileStmt.getBody().isBlockStmt()) {
                    Statement body = whileStmt.getBody();
                    BlockStmt blockStmt = new BlockStmt();
                    blockStmt.addStatement(body);
                    whileStmt.setBody(blockStmt);
                }
                // 获取 condition，构造 print statement
                Statement enterLoopStmt = generateEnteringLoopPrintStmt(whileStmt);
                whileStmt.getBody().asBlockStmt().addStatement(0,enterLoopStmt);
                Statement exitLoopStmt = generateExitingLoopPrintStmt(whileStmt);
                Optional<Node> parentNode = whileStmt.getParentNode();
                if(parentNode.get() instanceof BlockStmt){
                    int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(whileStmt);
                    ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,exitLoopStmt);
//                    ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index,enterLoopStmt);
                }
                return super.visit(whileStmt, arg);
            }
        }, null);

        return cu.toString();
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
                    Expression innerValue = value;
                    //去掉括号包装
                    while(innerValue.isEnclosedExpr()){
                        innerValue = innerValue.asEnclosedExpr().getInner();
                    }
                    //处理三目运算符
                    if(innerValue.isConditionalExpr()){
                        Statement[] conditionPrintStmts = generateConditionExprPrintStmt(varName,innerValue.asConditionalExpr());
                        Statement printStmtTrue = conditionPrintStmts[0];
                        Statement printStmtFalse = conditionPrintStmts[1];

                        //找到父blockStatement
                        Optional<Node> parentNode = stmt.getParentNode();
                        if(parentNode.isEmpty()){
                            return super.visit(stmt, arg);
                        }
                        if(parentNode.get() instanceof BlockStmt){
                            int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                            ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,printStmtTrue);
                            ((BlockStmt) parentNode.get()).asBlockStmt().addStatement(index+1,printStmtFalse);
                        }else if(parentNode.get() instanceof SwitchEntry){
                            int index = ((SwitchEntry) parentNode.get()).getStatements().indexOf(stmt);
                            ((SwitchEntry) parentNode.get()).addStatement(index+1,printStmtTrue);
                            ((SwitchEntry) parentNode.get()).addStatement(index+1,printStmtFalse);
                        }
                    }
                    else {
                        value = switch (op) {
                            case "+=" -> new BinaryExpr(new NameExpr(varName), value, BinaryExpr.Operator.PLUS);
                            case "-=" -> new BinaryExpr(new NameExpr(varName), value, BinaryExpr.Operator.MINUS);
                            case "/=" -> new BinaryExpr(new NameExpr(varName), value, BinaryExpr.Operator.DIVIDE);
                            case "*=" -> new BinaryExpr(new NameExpr(varName), value, BinaryExpr.Operator.MULTIPLY);
                            default -> value;
                        };
                        //要把强制类型转换去掉,避免在validation环节将类型转换误认为一个变量
                        String valueStr = value.toString().replace("(char)","").replace("(long)","")
                                .replace("(int)","").replace("(double)","").replace("(float)","");
                        if(valueStr.startsWith("-")){
                            valueStr = valueStr.substring(1);
                            valueStr = "-" + "(" + valueStr + ")";
                        }else{
                            valueStr = "(" + valueStr + ")";
                        }
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
                           value = "(" + value + ")";
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
                                        new EnclosedExpr(new NameExpr(param.getNameAsString())),
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
                // 获取 return 的表达式（如果有）
                Optional<Expression> returnExpr = stmt.getExpression();
                System.out.println(returnExpr.get());
                Expression innerValue = returnExpr.get();
                while(innerValue.isEnclosedExpr()){
                    innerValue = innerValue.asEnclosedExpr().getInner();
                }
                //三目运算符的处理
                if(innerValue.isConditionalExpr()){
                    Statement[] conditionPrintStmts = generateConditionExprPrintStmt("return_value",innerValue.asConditionalExpr());
                    Statement printStmtTrue = conditionPrintStmts[0];
                    Statement printStmtFalse = conditionPrintStmts[1];
                    if(parentNode.get() instanceof BlockStmt){
                        int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                        ((BlockStmt) parentNode.get()).addStatement(index,printStmtFalse);
                        ((BlockStmt) parentNode.get()).addStatement(index,printStmtTrue);
                    }else if(parentNode.get() instanceof SwitchEntry){

                        int index = ((SwitchEntry) parentNode.get()).getStatements().indexOf(stmt);
                        ((SwitchEntry) parentNode.get()).addStatement(index,printStmtFalse);
                        ((SwitchEntry) parentNode.get()).addStatement(index,printStmtTrue);
                    }
                }
                else{
                    Statement printStmt = generateReturnValuePrintStmt(stmt);
                    if(parentNode.get() instanceof BlockStmt){
                        //return expr;
                        //这里插桩的是 return_value = expr, current value of return_value: expr
                        int index = ((BlockStmt) parentNode.get()).asBlockStmt().getStatements().indexOf(stmt);
                        ((BlockStmt) parentNode.get()).addStatement(index,printStmt);
                    }else if(parentNode.get() instanceof SwitchEntry){
                        //return expr;
                        //这里插桩的是 return_value = expr, current value of return_value: expr
                        int index = ((SwitchEntry) parentNode.get()).getStatements().indexOf(stmt);
                        ((SwitchEntry) parentNode.get()).addStatement(index,printStmt);
                    }
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

    public static Statement generateEnteringLoopPrintStmt(WhileStmt whileStmt){
        //获取condition，构造print statement
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
        return printStmt;
    }

    public static Statement generateEnteringLoopPrintStmt(ForStmt forStmt){
        //获取condition，构造print statement
        Expression condition = forStmt.getCompare().get();
        condition = new EnclosedExpr(condition);
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Entering forloop with condition: " + condition + " is evaluated as: "),
                        condition,
//                        new NameExpr("true"),
                        BinaryExpr.Operator.PLUS
                ))
        ));
        return printStmt;
    }

    public static List<Statement> generateInitialStmtsOfForLoop(ForStmt forStmt){
        //获取condition，构造print statement
        List<Statement> pstmts = new ArrayList<>();
        Statement printStmt = null;
        List<Expression> initializations = forStmt.getInitialization();
        for(Expression expr : initializations){
            if(expr.isAssignExpr()){
                AssignExpr assignExpr = expr.asAssignExpr();
                String varName = assignExpr.getTarget().toString();
                String value = assignExpr.getValue().toString();
                value = "(" + value + ")";
                printStmt = new ExpressionStmt(new MethodCallExpr(
                        new NameExpr("System.out"),
                        "println",
                        NodeList.nodeList(new BinaryExpr(
                                new StringLiteralExpr(varName + " = " + value + ", current value of " + varName + ": "),
                                new EnclosedExpr(expr),
                                BinaryExpr.Operator.PLUS
                        ))
                ));
                pstmts.add(printStmt);
            }
            else if(expr.isVariableDeclarationExpr()){
                VariableDeclarationExpr varDecl = expr.asVariableDeclarationExpr();
                for (VariableDeclarator var : varDecl.getVariables()) {
                    String varName = var.getNameAsString();
                    String value = var.getInitializer().isPresent() ? var.getInitializer().get().toString() : "undefined";
                    value = "(" + value + ")";
                    printStmt = new ExpressionStmt(new MethodCallExpr(
                            new NameExpr("System.out"),
                            "println",
                            NodeList.nodeList(new BinaryExpr(
                                    new StringLiteralExpr(varName + " = " + value + ", current value of " + varName + ": "),
                                    new NameExpr("\"out of forloop area, can't see it!\""),
                                    BinaryExpr.Operator.PLUS
                            ))
                    ));
                    pstmts.add(printStmt);
                }
            }
        }
        return pstmts;
    }

    public static Statement generateExitingLoopPrintStmt(WhileStmt whileStmt){
        //获取condition，构造print statement
        Expression condition = whileStmt.getCondition();
        condition = new EnclosedExpr(condition);
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Exiting loop, condition no longer holds: " + condition + " is evaluated as: "),
                        condition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        return printStmt;
    }

    public static List<Statement> generateUpdateStmtOfForLoop(ForStmt forStmt){
        List<Statement > updateStmts = new ArrayList<>();
        List<Expression> update = forStmt.getUpdate();
        Statement printStmt = null;
        if(!update.isEmpty()){
            for(Expression expr : update){
                if(expr.isUnaryExpr()){
                    UnaryExpr unaryExpr = expr.asUnaryExpr();
                    String varName = unaryExpr.getExpression().toString();
                    String operator = unaryExpr.getOperator().asString();
                    String expandAssignExpr = "";
                    if(operator.equals("++")){
                        expandAssignExpr = varName + " = " + "(" + varName + " + 1" + ")";
                    }else if(operator.equals("--")){
                        expandAssignExpr = varName + " = " + "(" + varName + " - 1" + ")";
                    }
                    printStmt = new ExpressionStmt(new MethodCallExpr(
                            new NameExpr("System.out"),
                            "println",
                            NodeList.nodeList(new BinaryExpr(
                                    new StringLiteralExpr( expandAssignExpr + ", current value of " + varName + ": "),
                                    unaryExpr.getExpression(),
                                    BinaryExpr.Operator.PLUS
                            ))
                    ));
                }
                else if(expr.isAssignExpr()){
                    AssignExpr assignExpr = expr.asAssignExpr();
                    String varName = assignExpr.getTarget().toString();
                    String value = assignExpr.getValue().toString();
                    printStmt = new ExpressionStmt(new MethodCallExpr(
                            new NameExpr("System.out"),
                            "println",
                            NodeList.nodeList(new BinaryExpr(
                                    new StringLiteralExpr(varName + " = " + value + ", current value of " + varName + ": "),
                                    assignExpr.getValue(),
                                    BinaryExpr.Operator.PLUS
                            ))
                    ));
                }
                updateStmts.add(printStmt);
            }
        }
        return updateStmts;
    }

    public static Statement generateExitingLoopPrintStmt(ForStmt forStmt){
        //获取condition，构造print statement
        Expression condition = forStmt.getCompare().get();
        condition = new EnclosedExpr(condition);
        Statement printStmt = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Exiting forloop, condition no longer holds: " + condition + " is evaluated as: "),
                        new NameExpr("false"),
                        BinaryExpr.Operator.PLUS
                ))
        ));
        return printStmt;
    }

    //三目运算符的printStmt生成
    public static Statement[] generateConditionExprPrintStmt(String varName, ConditionalExpr expr){
        Expression condition = expr.getCondition();
        condition = new EnclosedExpr(condition);
        Expression negCondition = new UnaryExpr(condition, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
        Expression thenExpr = expr.getThenExpr();
        thenExpr = new EnclosedExpr(thenExpr);
        Expression elseExpr = expr.getElseExpr();
        elseExpr = new EnclosedExpr(elseExpr);
        Statement printStmtTrue = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Under condition " + varName + " = " + thenExpr + ", condition is " + ": "),
                        condition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        Statement printStmtFalse = new ExpressionStmt(new MethodCallExpr(
                new NameExpr("System.out"),
                "println",
                NodeList.nodeList(new BinaryExpr(
                        new StringLiteralExpr("Under condition " + varName + " = " + elseExpr + ", condition is " + ": "),
                        negCondition,
                        BinaryExpr.Operator.PLUS
                ))
        ));
        return new Statement[]{printStmtTrue, printStmtFalse};
    }

    public static Statement generateReturnValuePrintStmt(ReturnStmt returnStmt){
        // 获取 return 的表达式（如果有）
        Optional<Expression> returnExpr = returnStmt.getExpression();
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

    public static Set<String> extractVariablesInLogicalExpr(String javaExpression) throws Exception {
        String wrappedCode = "class Temp { void method() { boolean result = " + javaExpression + "; } }";
        CompilationUnit cu = new JavaParser().parse(wrappedCode).getResult().get();
        Set<String> variables = new HashSet<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(NameExpr n, Void arg) {
                variables.add(n.getNameAsString());  // 直接变量名（如 a, b）
                super.visit(n, arg);
            }

            @Override
            public void visit(BinaryExpr n, Void arg) {
                // 处理比较运算符（>、<、&&、|| 等）
                Expression left = n.getLeft();
                Expression right = n.getRight();

                // 递归检查左右子表达式
                left.accept(this, arg);
                right.accept(this, arg);
                super.visit(n, arg);
            }
        }, null);


        return variables;
    }

    /**
     * 检查代码中所有方法是否包含循环，返回方法名与结果的映射。
     *
     * @param ssmp Java 代码字符串
     * @return Map<方法名, 是否包含循环>
     */
    public static boolean ssmpHasLoopStmt(String ssmp) {
        Map<String, Boolean> result = new HashMap<>();
        try {
            CompilationUnit cu = new JavaParser().parse(ssmp).getResult().get();
            // 遍历所有方法
            MethodDeclaration md = cu.findFirst(MethodDeclaration.class).get();
            return containsLoop(md);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean containsLoop(MethodDeclaration method) {

        return method.getBody()
                .map(body -> body.getStatements().stream()
                        .anyMatch(ExecutionPathPrinter::stmtHasLoopStmt))
                .orElse(false);
    }

    public static boolean stmtHasLoopStmt(Statement stmt){
        if(isLoopStatement(stmt)){
            return true;
        }
        boolean b = false;
        if(stmt instanceof IfStmt){
            Statement thenStmt = ((IfStmt) stmt).getThenStmt();
            if(thenStmt instanceof BlockStmt){
                NodeList<Statement> childStmts = ((BlockStmt) thenStmt).getStatements();
                for(Statement childStmt : childStmts){
                    b = b || stmtHasLoopStmt(childStmt);
                }
            }
            if(b) return b;
            if(((IfStmt) stmt).getElseStmt().isPresent()){
                Statement elseStmt = ((IfStmt) stmt).getElseStmt().get();
                b = b || stmtHasLoopStmt(elseStmt);
            }
            if(b) return b;
        }
        if(stmt instanceof BlockStmt){
            NodeList<Statement> childStmts = ((BlockStmt) stmt).getStatements();
            for(Statement childStmt : childStmts){
                b = b || stmtHasLoopStmt(childStmt);
            }
        }
        return b;
    }

    public static boolean isLoopStatement(Statement stmt) {
        return stmt instanceof ForStmt ||
                stmt instanceof WhileStmt ||
                stmt instanceof DoStmt ||
                stmt instanceof ForEachStmt;
    }

    public static void main(String[] args) throws Exception {
        String dir = "resources/dataset/someBench/";
        String testFileName = "Return100_Mutant5";
        String testFileNameJava = testFileName+".java";
        String testFilePath = dir + "/" + testFileNameJava;
        String ssmp = TransWorker.trans2SSMP(LogManager.file2String(testFilePath));
//        System.out.println(ssmpHasLoopStmt(ssmp));
        System.out.println(addPrintStmtForAssignStmt(ssmp));
//        String pureCode = TransFileOperator.file2String(testFilePath);
//        String targetCode = addPrintStmt(pureCode);
//        String T = "!(a+b < f && e) > b && c < (d ? g : u) && a == return_value";
//        Set<String> strings = extractVariablesInLogicalExpr(T);
//        for (String s : strings) {
//            System.out.println(s);
//        }
//        System.out.println(targetCode);
    }

}
