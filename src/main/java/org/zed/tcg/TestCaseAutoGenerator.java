package org.zed.tcg;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import org.mvel2.MVEL;
import org.zed.trans.TransFileOperator;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.zed.log.LogManager.file2String;
import static org.zed.trans.ExecutionPathPrinter.addPrintStatementsWithJavaParser;

public class TestCaseAutoGenerator {

    //为函数生成符合 T 要求的参数赋值，Map<key,value>，key为参数名，value就是具体赋值
    public static HashMap<String,String> generateParamsDefUnderT(String T, MethodDeclaration md) throws Exception {
        HashMap<String,String> testCase = new HashMap<>();
        List<Parameter> params = md.getParameters();
        if (params == null || params.isEmpty()) {
           return testCase;
        }
        Object[] values = generateAcceptableValue(T, params);
        if (values == null || values.length == 0) {
            throw new Exception("generateAcceptableValue 没有正确为参数赋值");
        }
        for(int i = 0 ; i < values.length ; i++){
            testCase.put(params.get(i).getNameAsString(),Objects.toString(values[i]));
        }
        return testCase;
    }

    public static Object[] generateAcceptableValue(String T,
                                                   List<Parameter> parameters) {
        // 生成可接受的case
        List<Object> values = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();
        for (Parameter p : parameters) {
            String paramName = p.getName().toString();
            variableNames.add(paramName);
        }
        //暴力生成测试用例
        int maxCount = 1000;
        boolean isOK = false;
        while(--maxCount >= 0) {
            for (Parameter p : parameters) {
                Type type = p.getType();
                Object o = generateRandomValue(type);
                values.add(o);
            }
            if(isAcceptableCase(T,variableNames,values)){
                isOK = true;
                break;
            }else{
                values.clear();
            }
        }
        if(isOK == false){
            System.out.println("生成随机值失败!");
            return null;
        }
        return values.toArray(new Object[0]);
    }
    /**
     * 求解逻辑表达式
     * @param expression 逻辑表达式（如 "x > 0 && y < 0"）
     * @param variableNames 变量名数组（如 ["x", "y"]）
     * @param variableValues 变量值数组（如 [5, -2]）
     * @return 表达式计算结果（true/false）
     * @throws IllegalArgumentException 如果变量名和值数量不匹配，或表达式非法
     */
    public static boolean evaluateLogicExpression(
            String expression,
            String[] variableNames,
            Object[] variableValues) {

        // 校验参数
        if (variableNames == null || variableValues == null || variableNames.length != variableValues.length) {
            throw new IllegalArgumentException("变量名和变量值数量不匹配");
        }

        // 构造变量上下文
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < variableNames.length; i++) {
            context.put(variableNames[i], variableValues[i]);
        }

        // 执行表达式
        try {
            Object result = MVEL.eval(expression, context);
            if (result instanceof Boolean) {
                return (boolean) result;
            } else {
                throw new IllegalArgumentException("MVEL.eval 返回值异常");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("表达式解析失败: " + e.getMessage(), e);
        }
    }
    public static boolean isAcceptableCase( String T,
                                            List<String> variableNames,
                                            List<Object> variableValues) {

        return evaluateLogicExpression(T,variableNames.toArray(new String[0]),variableValues.toArray(new Object[0])); // 默认接受所有表达式
    }
    public static List<Parameter> getParamsOfOneStaticMethod(String program){
        CompilationUnit cu = new JavaParser().parse(program).getResult().get();
        String className = cu.getTypes().get(0).getNameAsString();
        Optional<MethodDeclaration> staticMethodOpt = cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isStatic() && !m.getNameAsString().equals("main"))
                .findFirst();
        if (staticMethodOpt.isEmpty()) {
            System.out.println("未找到静态方法，跳过: " + className);
            return null;
        }
        MethodDeclaration staticMethod = staticMethodOpt.get();
        List<Parameter> parameters = staticMethod.getParameters();
        return parameters;
    }
    public static String generateRandomValue(Type type) {
        String typeName = type.asString();
        switch (typeName) {
            case "int": case "short": case "byte": case "long":
                return randomIntGen();
            case "float": return randomFloatGen();
            case "double": return randomDoubleGen();
            case "boolean": return "false";
            case "char": return randomCharGen();
            case "int[]": return randomIntArrayGen();
            case "char[]": return randomCharArrayGen();
            case "String": return randomStringGen();
            case "double[]": return randomDoubleArrayGen();
            default:
                if (typeName.endsWith("[]")) {
                    return "new " + typeName.replace("[]", "[0]");
                }
                return "null";
        }
    }
    public static String randomIntGen(){
        int n = ThreadLocalRandom.current().nextInt(-500,500);
        return String.valueOf(n);
    }
    public static String randomFloatGen(){
        float n =  ThreadLocalRandom.current().nextFloat();
        return String.valueOf(n);
    }
    public static String randomDoubleGen(){
        double n = ThreadLocalRandom.current().nextDouble(0.0, 10.0);
        return String.valueOf(n);
    }
    public static String randomCharGen() {
        int choice = ThreadLocalRandom.current().nextInt(4); // 0: A-Z, 1: a-z, 2: 0-9
        char c = switch (choice) {
            case 0 -> (char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1);
            case 1 -> (char) ThreadLocalRandom.current().nextInt('a', 'z' + 1);
            case 2 -> (char) ThreadLocalRandom.current().nextInt('0', '9' + 1);
            case 3 -> (char) ThreadLocalRandom.current().nextInt('z' + 1, '~' + 1);
            default -> throw new IllegalStateException();
        };
        return String.valueOf(c);
    }
    public static String randomIntArrayGen(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < 10; i++) {
            String s = randomIntGen();
            sb.append(s);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
    public static String randomCharArrayGen(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < 10; i++) {
            String s = randomCharGen();
            sb.append(s);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
    public static String randomDoubleArrayGen(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < 10; i++) {
            String s = randomDoubleGen();
            sb.append(s);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
    public static String randomStringGen(){
        StringBuilder sb = new StringBuilder();
        int len = ThreadLocalRandom.current().nextInt(0, 10);
        for (int i = 0; i < len; i++) {
            sb.append(randomCharGen());
        }
        return sb.toString();
    }
    public static void printParamsValues(Parameter[] parameters,Object[] values) {
        for(int i = 0; i < parameters.length; i++){
            System.out.println("param: " + parameters[i].getName() + " , " +"value: " + String.valueOf(values[i]));
        }
    }


    public static void main(String[] args) {
        String program = file2String("resources/dataset/ChangeCase.java");
        String s = addPrintStatementsWithJavaParser(program);
        System.out.println(s);
    }
}