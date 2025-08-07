package org.zed.trans;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TransFileOperator {
    public static final String TRANS_WORK_DIR = "resources/trans";
    public static final String SOURCE_CODES_DIR = TRANS_WORK_DIR + "/" + "sourceCodes";
    public static final String ONE_STATIC_MD_CODES_DIR = TRANS_WORK_DIR + "/" + "oneStaticMdCodes";
    public static final String ONE_NORMAL_MD_CODES_DIR = TRANS_WORK_DIR + "/" +"oneNormalMdCodes";
    public static final String MULTI_NORMAL_MD_CODES_DIR = TRANS_WORK_DIR + "/" +"multiNormalMdCodes";
    public static final String MULTI_STATIC_MD_CODES_DIR = TRANS_WORK_DIR + "/" +"multiStaticMdCodes";
    public static final String GENERATED_MAIN_METHOD_DIR = TRANS_WORK_DIR + "/" +"generatedMainMethod";
    public static final String UNKNOWN_CODES_DIR= TRANS_WORK_DIR + "/" +"unknownCodes";
    public static final String ADDED_STATIC_FLAG_DIR = ONE_NORMAL_MD_CODES_DIR + "/" + "addedStaticFlag";
    public final static String ADDED_PRINT_CODES_DIR = TRANS_WORK_DIR + "/" + "addedPrintCodes";
    public final static String HAS_NO_ARRAY_CODES_DIR = TRANS_WORK_DIR + "/" + "hasNoArrayCodes";
    public final static String TRANS_RUNNABLE_DIR = TRANS_WORK_DIR + "/" + "runnable";

    public static void classifySourceCodes() throws IOException {
        int count = 0;
        int num1Static = 0;
        int numMulStatic = 0;
        int num1Normal = 0;
        int numMulNormal = 0;
        int numHasMain = 0;
        int numOther = 0;
        File[] allFiles =  fetchAllJavaFilesInDir(SOURCE_CODES_DIR);
        for (File file : allFiles) {
            System.out.println("Processing the ["+ ++count +"]th program："+file.getName());
            String program = file2String(file.getAbsolutePath());
            String targetPath;

            //有main方法的程序直接放入GENERATED_MAIN_METHOD_DIR，等待进一步格式化
            if(hasMainMdProgram(program)) {
                targetPath = GENERATED_MAIN_METHOD_DIR + "/" + file.getName();
                numHasMain++;
            }
            //没有main方法，但是含有一个静态方法的程序移动到 ONE_STATIC_MD_CODES_DIR，需要进行main方法生成
            else if(countStaticMethodProgram(program) == 1 && countNormalMethodProgram(program) == 0){
                //将当前file复制到oneStaticMdCodes目录下
                targetPath = ONE_STATIC_MD_CODES_DIR + "/" + file.getName();
                num1Static++;
            }
            //没有main方法，且有多个静态方法的程序移动到multiStaticMdCodes目录下
            else if(countStaticMethodProgram(program) > 1 && countNormalMethodProgram(program) == 0){
                targetPath = MULTI_STATIC_MD_CODES_DIR + "/" + file.getName();
                numMulStatic++;
            }
            //没有main方法，没有静态方法，且有一个非静态方法的程序移动到oneNormalMdCodes目录下
            else if(countNormalMethodProgram(program) == 1 && countStaticMethodProgram(program) == 0){
                //将当前file复制到oneNormalMdCodes目录下
                targetPath = ONE_NORMAL_MD_CODES_DIR + "/" + file.getName();
                num1Normal++;
            }
            //没有main方法，没有静态方法, 且有多个非静态方法的程序移动到multiNormalMdCodes目录下
            else if(countNormalMethodProgram(program) > 1 && countStaticMethodProgram(program) == 0){
                targetPath = MULTI_NORMAL_MD_CODES_DIR + "/" + file.getName();
                numMulNormal++;
            }
            else{
                targetPath = UNKNOWN_CODES_DIR + "/" + file.getName();
                numOther++;
            }
            if(Files.exists(Paths.get(targetPath))){
                Files.delete(Paths.get(targetPath));
            }
            Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(targetPath));
        }
//        System.out.println("一共处理了" + count + "个程序");
//        System.out.println("自带main方法的程序有" + numHasMain + "个");
//        System.out.println("只有一个static方法的程序有" + num1Static + "个");
//        System.out.println("有多个static方法的程序有" + numMulStatic + "个");
//        System.out.println("只有一个非静态方法的程序有" + num1Normal + "个");
//        System.out.println("有多个非静态方法的程序有" + numMulNormal + "个");
//        System.out.println("其它未分类程序有" + numOther + "个");
    }
    /*
     * @description 把非static方法，加上 static 修饰符，并转移到 ONE_STATIC_MD_CODES_DIR 目录
     */
    public static void addStaticFlag4OneNormalMdInDefaultDir() throws IOException {
        int count = 0;
        String addedStaticFlagDir = ADDED_STATIC_FLAG_DIR;
        if(Files.exists(Paths.get(addedStaticFlagDir))) {
            Files.list(Paths.get(addedStaticFlagDir)).forEach(p -> {p.toFile().delete();});
        }else{
            Files.createDirectories(Paths.get(addedStaticFlagDir));
        }
        File[] files = fetchAllJavaFilesInDir(ONE_NORMAL_MD_CODES_DIR);
        for (File file : files) {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(file).getResult().get();
            //给所有非静态方法添加static标志
            cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                    .filter(md -> !md.isStatic())
                    .forEach(m -> {
                        NodeList<Modifier> modifiers = m.getModifiers();
                        modifiers.add(Modifier.staticModifier());
                        m.setModifiers(modifiers);
                    });
            // 构建输出路径
            Path outputPath = Paths.get(addedStaticFlagDir, file.getName());
            if(Files.exists(outputPath)){
                Files.delete(outputPath);
            }
            // 写入修改后的文件
            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                writer.write(cu.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //复制一份到ONE_STATIC_MD_CODES_DIR目录下
            Path oneStaticPath = Paths.get(ONE_STATIC_MD_CODES_DIR, file.getName());
            if(Files.exists(oneStaticPath)){
                Files.delete(oneStaticPath);
            }
            Files.copy(outputPath, oneStaticPath);
            count++;
        }
        System.out.println( "为"+ count + "个文件的非静态方法添加了static标志，处理完成！" );
    }
    public static String addStaticFlag2SNMP(String snmp){
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(snmp).getResult().get();
        //给所有非静态方法添加static标志
        cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                .filter(md -> !md.isStatic())
                .forEach(m -> {
                    NodeList<Modifier> modifiers = m.getModifiers();
                    modifiers.add(Modifier.staticModifier());
                    m.setModifiers(modifiers);
                });
        return cu.toString();
    }
    public static boolean hasMainMdProgram(String program){
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(program).getResult().get();
        return cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                .anyMatch(md -> md.getNameAsString().equals("main"));
    }
    public static long countStaticMethodProgram(String program){
        long count;
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(program).getResult().get();
        count = cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                .filter(md -> md.isStatic() && !md.getNameAsString().equals("main"))
                .count();
        return count;
    }
    public static long countNormalMethodProgram(String program){
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(program).getResult().get();
        long mdCount = cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream()
                .filter(md -> !md.isStatic())
                .count();
        return mdCount;
    }
    public static void initTransWorkDir() throws IOException {
        // 创建工作目录
        Files.createDirectories(Path.of(TRANS_WORK_DIR));
        Files.createDirectories(Path.of(SOURCE_CODES_DIR));
        Files.createDirectories(Path.of(ONE_STATIC_MD_CODES_DIR));
        Files.createDirectories(Path.of(ONE_NORMAL_MD_CODES_DIR));
        Files.createDirectories(Path.of(MULTI_NORMAL_MD_CODES_DIR));
        Files.createDirectories(Path.of(MULTI_STATIC_MD_CODES_DIR));
        Files.createDirectories(Path.of(GENERATED_MAIN_METHOD_DIR));
        Files.createDirectories(Path.of(UNKNOWN_CODES_DIR));
        Files.createDirectories(Path.of(ADDED_STATIC_FLAG_DIR));
        Files.createDirectories(Path.of(ADDED_PRINT_CODES_DIR));
        Files.createDirectories(Path.of(TRANS_RUNNABLE_DIR));
    }
    public static void deleteTransWorkDir() throws IOException {
        Files.deleteIfExists(Path.of(TRANS_WORK_DIR));
    }

    public static String file2String(String FilePath) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(FilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    /*
         从目录树中找出所有.java文件
     */
    public static File[] fetchAllJavaFilesInDir(String dir) throws IOException {
        Path path = Paths.get(dir);
        List<File> javaFiles = new ArrayList<>();
        if (Files.isDirectory(path)) {
            Files.walk(path)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> javaFiles.add(p.toFile()));
        } else {
            javaFiles.add(path.toFile());
        }
        return javaFiles.toArray(new File[0]);
    }

    /*
     从目录树中找出所有.java文件并删除
    */
    public static void deleteAllJavaFilesInDir(String dir) throws IOException {
        Path path = Paths.get(dir);
        Files.walk(path)
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> p.toFile().delete());
    }
    public static void cleanJavaCodesInTransWorkDir() throws IOException {
        deleteAllJavaFilesInDir(TRANS_WORK_DIR);
    }
    public static void cleanUnusableFilesInTrans() throws IOException {
        deleteDirectoryRecursively(Path.of(GENERATED_MAIN_METHOD_DIR));
        deleteDirectoryRecursively(Paths.get(ONE_NORMAL_MD_CODES_DIR));
    }

    public static void saveRunnablePrograms(String fileName,String program,int n) throws IOException {
        String className = fileName.substring(0, fileName.lastIndexOf("."));
        String dir = TRANS_RUNNABLE_DIR + "/" + className + "/" + String.valueOf(n);
        File dirF =Path.of(dir).toFile();
        if(!dirF.exists()){
            dirF.mkdirs();
        }
        Path path = Path.of(dir + "/" + fileName);
        File file = path.toFile();
        if(file.exists()){
            file.delete();
        }
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(program);
        }
    }
    public static String getAddedPrintCodesOfProgram(String fileName){
        String addedPrintCodesPath = ADDED_PRINT_CODES_DIR + "/" + fileName;
        return file2String(addedPrintCodesPath);
    }

    public static void deleteDirectoryRecursively(Path dir) throws IOException {
        // 确保目录存在
        if (!Files.exists(dir)) {
            return;
        }
        // 递归删除目录及内容（反向排序：先删文件再删目录）
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("删除失败: " + path + " - " + e.getMessage());
                    }
                });
    }

    public static void copyPrograms2TransSourceDir(String path) throws IOException {
        if(new File(path).isDirectory()){
            File[] files = fetchAllJavaFilesInDir(path);
            for (File file : files) {
                Path transP = Path.of(SOURCE_CODES_DIR, file.getName());
                if(transP.toFile().exists()){
                   transP.toFile().delete();
                }
                Files.copy(file.toPath(), transP);
            }
        }
        else{
            Path p = Path.of(path);
            Files.copy(p, Path.of(SOURCE_CODES_DIR, p.getFileName().toString()));
        }
    }

    public static boolean paramsContainsArrayType(String programPath){
        String code = file2String(programPath);
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            if(isMainMethod(method)){
                continue;
            }
            NodeList<Parameter> parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                Type type = parameter.getType();
                System.out.println(type.toString());
                if(type instanceof ArrayType){
                    return true;
                }else if(isCollectionType(type.toString())){
                    return true;
                }
            }
        }
        return false;
    }
    private static boolean isMainMethod(MethodDeclaration method) {
        return method.isPublic() &&
                method.isStatic() &&
                method.getNameAsString().equals("main") &&
                method.getType().toString().equals("void");
    }
    private static boolean isCollectionType(String typeName) {
        return typeName.equals("List") || typeName.equals("ArrayList")
                || typeName.equals("Set") || typeName.equals("HashSet")
                || typeName.equals("Collection") || typeName.equals("LinkedList");
    }
    public static void copyHasNoArrayPrograms() throws IOException {
        File[] files = fetchAllJavaFilesInDir(ADDED_PRINT_CODES_DIR);
        int count = 0;
        int hasNoArrayCount = 0;
        for (File file : files) {
            count++;
            System.out.println("[筛查不带数组程序]正在检查第"+ count +"个文件："+file.getName());
            if(!paramsContainsArrayType(file.getAbsolutePath())){
                Files.copy(file.toPath(), Paths.get(HAS_NO_ARRAY_CODES_DIR, file.getName()));
                hasNoArrayCount++;
            }
        }
        System.out.println("[筛查不带数组程序]共分析了"+ count +"个程序");
        System.out.println("[筛查不带数组程序]不含有数组的程序有"+ hasNoArrayCount + "个");
    }
    public static void saveAddedPrintCodes(String code, String path) throws IOException {
        File file = new File(path);
        if(Files.exists(file.toPath())) {
            Files.delete(file.toPath());
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(code);
        } catch (IOException e) {
            System.out.println("写入文件时发生错误: " + e.getMessage());
        }
    }

    public static boolean isSSMP(String program){
        if(countStaticMethodProgram(program) == 1){
            return true;
        }
        return false;
    }
    //只含有一个普通方法的程序
    public static boolean isSNMP(String program){
        if(countNormalMethodProgram(program) == 1){
            return true;
        }
        return false;
    }

    public static String trans2SSMP(String pureProgram){
        if(isSSMP(pureProgram)){
            return pureProgram;
        }
        if(isSNMP(pureProgram)){
            String transProgram = addStaticFlag2SNMP(pureProgram);
            if(isSSMP(transProgram)){
                return transProgram;
            }
        }
        System.out.println("转换为SSMP失败!");
        return null;
    }

}
