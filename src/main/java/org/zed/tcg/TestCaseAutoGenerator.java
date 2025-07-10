package org.zed.tcg;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import org.mvel2.MVEL;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.zed.log.LogManager.file2String;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;

public class TestCaseAutoGenerator {

    //为函数生成符合 T 要求的参数赋值，Map<key,value>，key为参数名，value就是具体赋值
    public static HashMap<String,String> generateParamsDefUnderExpr(String expr, MethodDeclaration md){
        HashMap<String,String> testCase = new HashMap<>();
        Object[] values;
        List<Parameter> params = md.getParameters();
        if (params == null || params.isEmpty()) {
           return testCase;
        }
        try{
            values = generateAcceptableValue(expr, params);
            if (values == null || values.length == 0) {
                throw new Exception("generateAcceptableValue 没有正确为参数赋值");
            }
        } catch (Exception e) {
            System.out.println("生成参数赋值时异常！");
            return null;
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
        int maxCount = 50000;
        boolean isOK = false;
        while(--maxCount >= 0) {
            for (Parameter p : parameters) {
                Type type = p.getType();
                String o = generateRandomValue(type);
                collectValue(type, values, o);
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
    public static void collectValue(Type type, List<Object> values,String value) {
        String typeName = type.asString();
        switch (typeName) {
            case "int": case "short": case "byte": case "long":
                values.add(Integer.parseInt(value));
                break;
            case "float":
                values.add(Float.parseFloat(value));
                break;
            case "double":
                values.add(Double.parseDouble(value));
                break;
            case "boolean":
                values.add(Boolean.parseBoolean(value));
                break;
            case "char":
                values.add(Character.toString(value.charAt(0)));
                break;
            case "int[]":
                values.add(value);
                break;
            case "char[]":
                values.add(value);
                break;
            case "String":
                values.add(value);
                break;
            case "double[]":
                values.add(value);
                break;
            default:
                if (typeName.endsWith("[]")) {
                    values.add("new " + typeName.replace("[]", "[0]"));
                } else {
                    values.add(value);
                }
        }
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
            case "boolean": return randomBooleanGen();
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

    private static String randomBooleanGen() {
        int randomInt = ThreadLocalRandom.current().nextInt();
        if(randomInt % 2 == 0){
            return "true";
        }else {
            return "false";
        }
    }

    public static String randomIntGen(){
        int n = ThreadLocalRandom.current().nextInt(-120,120);
        return String.valueOf(n);
    }
    public static String randomFloatGen(){
        float n =  ThreadLocalRandom.current().nextFloat();
        return String.valueOf(n);
    }
    public static String randomDoubleGen(){
        int choice = ThreadLocalRandom.current().nextInt(2); // 0: A-Z, 1: a-z, 2: 0-9
        int sign = 1;
        if(choice == 0){
            sign = -1;
        }else if(choice == 1){
            sign = 1;
        }
        double n = ThreadLocalRandom.current().nextDouble(0.0, 10.0);
//        n = Math.round(n * 1000.0) / 1000.0;
        n = n * sign;
        return String.valueOf(n);
    }
    public static String randomCharGen() {
        int choice = ThreadLocalRandom.current().nextInt(8); // 0: A-Z, 1: a-z, 2: 0-9
        char c = switch (choice) {
            case 0 -> (char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1);
            case 1 -> (char) ThreadLocalRandom.current().nextInt('a', 'z' + 1);
            case 2 -> (char) ThreadLocalRandom.current().nextInt('0', '9' + 1);
            case 3 -> (char) ThreadLocalRandom.current().nextInt('Z' + 1, 'a' - 1);
            case 4 -> (char) ThreadLocalRandom.current().nextInt('z' + 1, '~');
            case 5 -> (char) ThreadLocalRandom.current().nextInt('9' + 1, 'A' - 1);
            case 6 -> (char) ThreadLocalRandom.current().nextInt('!', '0' - 1);
            case 7 -> (char) '/';
            default -> throw new IllegalStateException();
        };
        if(c == '\\'){
            return randomCharGen();
        }else{
            return String.valueOf(c);
        }
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
        String s = addPrintStmt(program);
        System.out.println(s);
    }
}