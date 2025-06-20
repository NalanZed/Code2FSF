package org.zed.tcg;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.zed.SpecUnit;
import org.zed.trans.ExecutionPathPrinter;
import org.zed.trans.TransFileOperator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zed.tcg.TestCaseAutoGenerator.generateParamsDefUnderT;
import static org.zed.trans.TransFileOperator.*;

public class ExecutionEnabler {

    public static String insertMainMdInSSMP(String ssmp,String mainMd) throws IOException {
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(ssmp).getResult().get();
        String className = cu.getTypes().get(0).getNameAsString();
        Optional<ClassOrInterfaceDeclaration> classOpt = cu.getClassByName(className);
        MethodDeclaration mainMethodDec = parser.parseMethodDeclaration(mainMd).getResult().get();
        classOpt.get().addMember(mainMethodDec);
        return cu.toString();
    }


    public static String generateMainMdUnderT(SpecUnit su) throws Exception {
        String program = su.getProgram();
        String T = su.getT();
        return generateMainMdUnderT(T,program);
    }
    public static String generateMainMdUnderT(String T, String program) throws Exception {
        //1. 解析program
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(program).getResult().get();
        String className = cu.getTypes().get(0).getNameAsString();
        MethodDeclaration md = getFirstStaticMethod(program);
        if(md==null){
            throw new Exception(className + "没有可检验的static方法");
        }
        List<Parameter> parameters = md.getParameters();

        //2. 组装main函数定义的开头
        StringBuilder builder = new StringBuilder();
        builder.append("public static void main(String[] args) {\n");

        //3.拿到根据T生成的 param 以及 value
        HashMap<String, String> testCaseMap = generateParamsDefUnderT(T, md);

        //4. 根据 testCase来组装main函数中的调用 static 方法前的参数定义
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String value = testCaseMap.get(parameter.getNameAsString());
                if("char".equals(parameter.getTypeAsString())){
                    value = "'"+value+"'";
                    System.out.println(value);
                }
                builder.append(parameter.getTypeAsString())
                        .append(" ")
                        .append(parameter.getNameAsString())
                        .append(" = ")
                        .append(value)
                        .append(";\n");
            }
        }
        //5.组装函数调用语句
        if (!md.getType().isVoidType()) {
            builder.append("    ").append(md.getType()).append(" result = ");
        }

        builder.append(className).append(".").append(md.getNameAsString()).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) builder.append(", ");//第一个参数后加,
            builder.append(parameters.get(i).getNameAsString());
        }
        builder.append(");\n");
        builder.append("}");

        return builder.toString();
    }
    public static String generateMainMdUnderT(String T, File javaFile) throws Exception {
        String program = TransFileOperator.file2String(javaFile.getAbsolutePath());
        return generateMainMdUnderT(T, program);
    }


    public static String generateMainMethodWithoutT(String program) throws Exception {
        return generateMainMdUnderT("true",program);
    }

    public static MethodDeclaration getFirstStaticMethod(String program){
        JavaParser parser = new JavaParser();
        // 解析Java文件
        CompilationUnit cu = parser.parse(program).getResult().get();

        // 获取类名
        String className = cu.getTypes().get(0).getNameAsString();
        Optional<ClassOrInterfaceDeclaration> classOpt = cu.getClassByName(className);
        if (classOpt.isEmpty()) {
            return null;
        }

        // 查找第一个静态方法（非main方法）
        Optional<MethodDeclaration> staticMethodOpt = cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isStatic() && !m.getNameAsString().equals("main"))
                .findFirst();
        return staticMethodOpt.get();
    }

//    public static String prepareForAutoVerification(String program,String fileName) throws Exception {
//        String printedProgram =  ExecutionPathPrinter.addPrintStatementsWithJavaParser(program);
//        String path = ADDED_PRINT_CODES_DIR + "/" + fileName;
//        TransFileOperator.saveAddedPrintCodes(printedProgram, path);
//        return printedProgram;
//    }

}
