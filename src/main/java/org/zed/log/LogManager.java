package org.zed.log;


import org.zed.llm.ModelMessage;
import org.zed.llm.ModelPrompt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LogManager {

    public static final String RESOURCE_DIR = "resources";
    public static final String LOG_DIR = RESOURCE_DIR + "/" +"log";
    public static final String CODE_GEN_LOG_DIR = LOG_DIR + "/" + "codegen";
    public static final String FSF_REVIEW_LOG_DIR = LOG_DIR + "/" +"FSFReview";
    public static final String TRANS_WORK_DIR = RESOURCE_DIR + "/" + "trans";
    public static final String LOG_FILE_SUFFIX = ".txt";
    public static final String ADDED_PRINT_CODES_DIR = TRANS_WORK_DIR + "/"+ "addedPrintCodes";
    public static final String TRANS_SOURCE_CODES_DIR = TRANS_WORK_DIR + "/"+ "sourceCodes";
    public static final String SUCC_DATASET_DIR = RESOURCE_DIR + "/" + "succDataset";
    public static final String FAILED_DATASET_DIR = RESOURCE_DIR + "/" + "failedDataset";
    public static final String Exception_DATASET_DIR = RESOURCE_DIR + "/" + "exceptionDataset";
    public static final String RUNNABLE_DIR = RESOURCE_DIR + "/" + "runnable";
    public static final String EXPERIMENT_DIR = RESOURCE_DIR + "/" + "experiment";

    private static List<String> needInitDirs = new ArrayList<>();

    public static boolean initLogWorkDirs(){
        needInitDirs.add(RESOURCE_DIR);
        needInitDirs.add(LOG_DIR);
        needInitDirs.add(CODE_GEN_LOG_DIR);
        needInitDirs.add(FSF_REVIEW_LOG_DIR);
        needInitDirs.add(TRANS_WORK_DIR);
        needInitDirs.add(ADDED_PRINT_CODES_DIR);
        needInitDirs.add(TRANS_SOURCE_CODES_DIR);
        needInitDirs.add(SUCC_DATASET_DIR);
        needInitDirs.add(FAILED_DATASET_DIR);
        needInitDirs.add(Exception_DATASET_DIR);
        needInitDirs.add(RUNNABLE_DIR);
        needInitDirs.add(EXPERIMENT_DIR);
        for (String dir : needInitDirs){
            if(new File(dir).exists()){
                continue;
            }
            //创建该目录
            try {
                Files.createDirectories(Path.of(dir));
            } catch (IOException e) {
                System.out.println("创建工作目录 " + dir + " 时出现异常");
                return false;
            }
        }
        return true;
    }

    public static void appendMessage(String codePath, ModelMessage msg, String model) throws IOException {
        String logFilePath = codePath2LogPath(codePath,model);
        java.nio.file.Path outputPath = java.nio.file.Paths.get(logFilePath);
        java.nio.file.Files.createDirectories(outputPath.getParent());
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("start role " + msg.getRole());
            writer.newLine();
            writer.write(msg.getContent());
            writer.newLine();
            writer.write("*end* role " + msg.getRole());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //自由指定log目录位置
    public static void appendMessageInDiyDir(String codePath, ModelMessage msg, String model,String diy) throws IOException {
        String logFilePath = codePath2DiyLogPath(codePath,model,diy);
        java.nio.file.Path outputPath = java.nio.file.Paths.get(logFilePath);
        java.nio.file.Files.createDirectories(outputPath.getParent());
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("start role " + msg.getRole());
            writer.newLine();
            writer.write(msg.getContent());
            writer.newLine();
            writer.write("*end* role " + msg.getRole());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String codePath2CodeReviewLogPath(String codePath, String model) {
        //从文件路径中提取文件名（在Java程序中，即类名）
        String logTitle = codePath.substring(codePath.lastIndexOf("/") + 1, codePath.lastIndexOf("."));
        logTitle = "log" + "-" + logTitle;
        return FSF_REVIEW_LOG_DIR  + "/" +model + "/" + logTitle + LOG_FILE_SUFFIX;
    }

    public static void appendFSFReviewSummary(String codePath, String summary, String model, String diy) throws IOException {
        String logFilePath = codePath2DiyLogPath(codePath,model,diy);
        java.nio.file.Path outputPath = java.nio.file.Paths.get(logFilePath);
        java.nio.file.Files.createDirectories(outputPath.getParent());
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("start log summary" );
            writer.newLine();
            writer.write(summary);
            writer.newLine();
            writer.write("*end* log summary");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendCode2FSFRemark(String logFilePath, String content) throws IOException {
        java.nio.file.Path outputPath = java.nio.file.Paths.get(logFilePath);
        java.nio.file.Files.createDirectories(outputPath.getParent());
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("start role remark");
            writer.newLine();
            writer.write(content);
            writer.newLine();
            writer.write("*end* role remark" );
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String file2String(String FilePath) {
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(FilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String codePath2LogPath(String codePath,String model){
        //从文件路径中提取文件名（在Java程序中，即类名）
        String logTitle = codePath.substring(codePath.lastIndexOf("/") + 1, codePath.lastIndexOf("."));
        logTitle = "log" + "-" + logTitle;
        return LOG_DIR  + "/" +model + "/" + logTitle + LOG_FILE_SUFFIX;
    }
    public static String codePath2DiyLogPath(String codePath,String model,String diy){
        //从文件路径中提取文件名（在Java程序中，即类名）
        String logTitle = codePath.substring(codePath.lastIndexOf("/") + 1, codePath.lastIndexOf("."));
        logTitle = "log" + "-" + logTitle;
        return diy  + "/" +model + "/" + logTitle + LOG_FILE_SUFFIX;
    }

    public static String codePath2FailedPath(String codePath){
        //从文件路径中提取文件名（在Java程序中，即类名）
        String title = codePath.substring(codePath.lastIndexOf("/") + 1);
        return FAILED_DATASET_DIR  + "/" + title;
    }
    public static String codePath2AddedPrintPath(String codePath){
        //从文件路径中提取文件名（在Java程序中，即类名）
        String fileName = codePath.substring(codePath.lastIndexOf("/") + 1);
        return ADDED_PRINT_CODES_DIR  + "/" + fileName;
    }

    //仅删除单个模型产生的日志文件
    public static void cleanLogOfModel(String model){
        String[] logFiles = fetchSuffixFilePathInDir(LOG_DIR + model + "/", LOG_FILE_SUFFIX);
        //将logFiles对应的文件删除
        if(logFiles != null){
            for (String logFile : logFiles) {
                File file = new File(logFile);
                if(file.exists()){
                    file.delete();
                }
            }
        }
    }

    /*
     从目录树中找出所有.java文件
 */
    public static void deleteAllJavaFilesInDir(String dir) throws IOException {
        Path path = Paths.get(dir);
        Files.walk(path)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> p.toFile().delete());
    }

    //删除Log目录下所有模型的日志文件
    public static void cleanLogOfModel(){
        String[] logFiles = fetchSuffixFilePathInDir(LOG_DIR, LOG_FILE_SUFFIX);
        if(logFiles != null){
            for (String logFile : logFiles) {
                File file = new File(logFile);
                if(file.exists()){
                    file.delete();
                }
            }
        }
    }

    public static java.io.File[] fetchTxtFileInDir(String dir) {
        java.io.File file = new java.io.File(dir);
        return file.listFiles((d, name) -> name.endsWith(".txt"));
    }

    public static String[] fetchSuffixFilePathInDir(String inputDir,String suffix) {
            List<String> javaFiles = new ArrayList<>();
            fetchSuffixFilesRecursive(new java.io.File(inputDir), javaFiles,suffix);
            return javaFiles.toArray(new String[0]);
    }

    public static void copy2TransSourceDir(String codePath) throws IOException {
        Path p = new File(codePath).toPath();
        // 添加对输入路径的检查
        if (!Files.exists(p)) {
            throw new IOException("源文件不存在: " + codePath);
        }
        if (Files.isDirectory(p)) {
            throw new IOException("输入路径是目录而非文件: " + codePath);
        }
        Path dir = Paths.get(TRANS_SOURCE_CODES_DIR);
        Files.createDirectories(dir);  // 确保目录存在
        Files.copy(p, dir.resolve(p.getFileName()), REPLACE_EXISTING);
    }

    private static void fetchSuffixFilesRecursive(java.io.File dir, List<String> javaFiles,String suffix) {
        if (dir.isDirectory()) {
            for (java.io.File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    fetchSuffixFilesRecursive(file, javaFiles,suffix);
                } else if (file.getName().endsWith(suffix)) {
                    javaFiles.add(file.getPath());
                }
            }
        }
    }
    public static List<String[]> parseTD(String msgContent) {
        List<String[]> TDs = new ArrayList<>();
        /*
            由于LLM生成的结果格式为
            T1:
            D1:
            T2:
            D2:
            这里先将其按行分割，再逐行记录到TD中，再加入到TDs中
         */
        msgContent = msgContent.substring(msgContent.indexOf("```") + 3, msgContent.lastIndexOf("```")).trim();
        String[] specs = msgContent.split("\n");
        int i = 0;
        while (i < specs.length) {
            if(specs[i].startsWith("T")){
                String[] TD = new String[2];
                TD[0] = specs[i].substring(specs[i].indexOf(":")+1).trim();
                if(TD[0].contains("//")){
                    TD[0] = TD[0].substring(0, TD[0].indexOf("//"));
                }
                while(TD[0].endsWith("||") || TD[0].endsWith("&&") || TD[0].endsWith("+") || TD[0].endsWith("-") || TD[0].endsWith("*") || TD[0].endsWith("/")){
                    i++;
                    if(i >= specs.length) break;
                    TD[0] += " " + specs[i].trim();
                    if(TD[0].contains("//")){
                        TD[0] = TD[0].substring(0, TD[0].indexOf("//"));
                    }
                }
                TD[1] = specs[i+1].substring(specs[++i].indexOf(":")+1).trim();
                if(TD[1].contains("//")){
                    TD[1] = TD[1].substring(0, TD[1].indexOf("//"));
                }
                while(TD[1].endsWith("||") || TD[1].endsWith("&&") || TD[1].endsWith("+") || TD[1].endsWith("-") || TD[1].endsWith("*") || TD[1].endsWith("/")){
                    i++;
                    if(i >= specs.length) break;
                    TD[1] += " " + specs[i].trim();
                    if(TD[1].contains("//")){
                        TD[1] = TD[1].substring(0, TD[1].indexOf("//"));
                    }
                }
                TDs.add(TD);
            }else {
                i++;
            }
        }
        return TDs;
    }
    public static String getLastestAssistantMsgFromLog(String logFilePath){
        String logString = file2String(logFilePath);
        if(!logString.contains("start role assistant") || !logString.contains("*end* role assistant")){
            return "";
        }
        int lastIndexOfAssisStart = logString.lastIndexOf("start role assistant") + "start role assistant".length();
        int lastIndexOfAssisEnd = logString.lastIndexOf("*end* role assistant");
        if(lastIndexOfAssisEnd <= lastIndexOfAssisStart){
            return "";
        }
        return logString.substring(lastIndexOfAssisStart + 1, lastIndexOfAssisEnd);
    }

    public static List<String[]> getLastestFSFFromLog(String logFilePath){
        String content = getLastestAssistantMsgFromLog(logFilePath);
        if(content.isEmpty()){
            return new ArrayList<>();
        }
        return parseTD(content);
    }
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
    public static String getProgramFromLog(String logFilePath){
        String logString = file2String(logFilePath);
        if(logString.contains("public class")){
            String program = logString.substring(logString.indexOf("public class"), logString.indexOf( "```\n"+
                    "*end* role user"));
            return program;
        }
        if(logString.contains("class")){
            String program = logString.substring(logString.indexOf("class"), logString.indexOf( "```\n"+
                    "*end* role user"));
            return program;
        }
        return "";
    }

    public static void copyFileToSuccDataset(String filePath) throws IOException {
        File file = new File(filePath);
        String name = file.getName();
        String succFilePath = SUCC_DATASET_DIR + "/" + name;
        Files.copy(Path.of(filePath), Path.of(succFilePath), REPLACE_EXISTING);
    }
    public static void copyFileToFailedDataset(String filePath) throws IOException {
        File file = new File(filePath);
        String name = file.getName();
        String succFilePath = FAILED_DATASET_DIR + "/" + name;
        Files.copy(Path.of(filePath), Path.of(succFilePath), REPLACE_EXISTING);
    }
    public static void copyFileToExceptionDataset(String exceptionFile) throws IOException {
        File file = new File(exceptionFile);
        String name = file.getName();
        String succFilePath = Exception_DATASET_DIR + "/" + name;
        Files.copy(Path.of(exceptionFile), Path.of(succFilePath), REPLACE_EXISTING);
    }

    public static String codePath2SuccPath(String codePath) {
        //从文件路径中提取文件名（在Java程序中，即类名）
        String title = codePath.substring(codePath.lastIndexOf("/") + 1);
        return SUCC_DATASET_DIR  + "/" + title;
    }

    public static void saveHistoryTestcases(String logFilePath,List<String> testCases) throws IOException {
        if(testCases == null || testCases.isEmpty()) return;
        int totalNum = testCases.size();
        int count = 0;
        appendCode2FSFRemark(logFilePath ,"的测试用例历史记录如下，共[" + totalNum + "]个");
//        for (String testcase : testCases) {
//            appendCode2FSFRemark(logFilePath,"------------------["+(++count) +"/"+totalNum+"]------------------");
////            System.out.println(testcase);
////            if(!testcase.isEmpty()){
////                appendCode2FSFRemark(logFilePath,testcase);
////            }
//        }
        appendCode2FSFRemark(logFilePath,"----------------------------------------");
    }

    public static boolean saveACodeGenMsg(ModelMessage msg,String model,String className){
        String logFilePath = CODE_GEN_LOG_DIR + "/" + model + "/" + className + ".txt";
        java.nio.file.Path outputPath = java.nio.file.Paths.get(logFilePath);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            System.out.println("记录CodeGenLog时失败");
            return false;
        }
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("start role " + msg.getRole());
            writer.newLine();
            writer.write(msg.getContent());
            writer.newLine();
            writer.write("*end* role " + msg.getRole());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getClassNameInCodeGenPrompt(ModelPrompt prompt){
        String className = "";
        List<ModelMessage> msgs = prompt.getMessages();
        String content = msgs.get(msgs.size()-1).getContent();
        if(content.contains("public static class")){
            className = content.substring(content.indexOf("public static class") + "public static class".length(),
                    content.indexOf("{")).trim();
        }
        if(className.isEmpty() && content.contains("public class")){
            className = content.substring(content.indexOf("public class") + "public class".length(),
                    content.indexOf("{")).trim();
        }
        if(className.isEmpty()){
            System.err.println("生成的代码中没有类名, 记录日志失败！");
            return className;
        }
        return className;
    }

    public static void saveACodeGenPrompt(ModelPrompt prompt){
        List<ModelMessage> msgs = prompt.getMessages();
        String content = msgs.get(msgs.size()-1).getContent();
        String className = getClassNameInCodeGenPrompt(prompt);
        System.out.println("className = " + className);
        for (ModelMessage msg : msgs) {
            boolean succ = LogManager.saveACodeGenMsg(msg, prompt.getModel(),className);
            if (!succ) {
                break;
            }
        }
    }

    public static Set<String> getClassNameOfCategory(String anyCategoryDir){
        Set<String> calssNames = new HashSet<>();
        java.io.File dir = new java.io.File(anyCategoryDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在或不是一个目录: " + anyCategoryDir);
            return calssNames;
        }
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".java"));
        if (files != null) {
            for (java.io.File file : files) {
                String fileName = file.getName();
                String className = fileName.substring(0, fileName.lastIndexOf("."));
                calssNames.add(className);
            }
        } else {
            System.err.println("目录中没有找到任何.java文件: " + anyCategoryDir);
        }

        return calssNames;
    }

    public static void classifyHandledCodeFiles(String targetDir){
        String classifiedDir = "resources/dataset/程序归类_0722";
        String branchDir = classifiedDir + "/Branched";
        String singlePathLoopDir = classifiedDir + "/Single-path-Loop";
        String multiPathLoopDir = classifiedDir + "/Multi-path-Loop";
        String NestedLoopDir = classifiedDir + "/Nested-Loop";
        String SequentialDir = classifiedDir + "/Sequential";

        Set<String> branchedSet = getClassNameOfCategory(branchDir);
        Set<String> singlePathLoopSet =getClassNameOfCategory(singlePathLoopDir);
        Set<String> multiPathLoopSet = getClassNameOfCategory(multiPathLoopDir);
        Set<String> nestedLoopSet = getClassNameOfCategory(NestedLoopDir);
        Set<String> sequentialSet = getClassNameOfCategory(SequentialDir);

        String newBranchedDir = targetDir + "/Branched";
        String newSinglePathLoopDir = targetDir + "/Single-path-Loop";
        String newMultiPathLoopDir = targetDir + "/Multi-path-Loop";
        String newNestedLoopDir = targetDir + "/Nested-Loop";
        String newSequentialDir = targetDir + "/Sequential";

        //创建新的分类目录
        try {
            Files.createDirectories(Path.of(newBranchedDir));
            Files.createDirectories(Path.of(newSinglePathLoopDir));
            Files.createDirectories(Path.of(newMultiPathLoopDir));
            Files.createDirectories(Path.of(newNestedLoopDir));
            Files.createDirectories(Path.of(newSequentialDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] strings = fetchSuffixFilePathInDir(targetDir, ".java");
        for (String filePath : strings) {
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
            if(branchedSet.contains(fileName)){
                System.out.println("Branched: " + fileName);
                try {
                    Files.copy(Path.of(filePath), Path.of(newBranchedDir + "/" + fileName + ".java"), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(singlePathLoopSet.contains(fileName)){
                System.out.println("Single-path Loop: " + fileName);
                try {
                    Files.copy(Path.of(filePath), Path.of(newSinglePathLoopDir + "/" + fileName + ".java"), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(multiPathLoopSet.contains(fileName)){
                System.out.println("Multi-path Loop: " + fileName);
                try {
                    Files.copy(Path.of(filePath), Path.of(newMultiPathLoopDir + "/" + fileName + ".java"), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(nestedLoopSet.contains(fileName)){
                System.out.println("Nested Loop: " + fileName);
                try {
                    Files.copy(Path.of(filePath), Path.of(newNestedLoopDir + "/" + fileName + ".java"), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(sequentialSet.contains(fileName)){
                System.out.println("Sequential: " + fileName);
                try {
                    Files.copy(Path.of(filePath), Path.of(newSequentialDir + "/" + fileName + ".java"), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                System.out.println("Unknown category: " + fileName);
            }
        }
    }

    public static void moveClassifiedCode() throws IOException {
        String sourceDir = "resources/dataset/4-shot";
        String targetDir = "resources/dataset/4-shot-classified";
        int brsucc = 0, brfailed = 0;
        int musucc = 0, mufailed = 0;
        int nlsucc = 0, nlfailed = 0;
        int sesucc = 0, sefailed = 0;
        int splsucc = 0, splfailed = 0;

        String[] strings = fetchSuffixFilePathInDir(sourceDir, ".java");
        for (String filePath : strings) {
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
            if(filePath.contains("Branched") && filePath.contains("succDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Branched/" + "succDataset/" + fileName + ".java"), REPLACE_EXISTING);
                brsucc++;
            }
            else if(filePath.contains("Branched") && filePath.contains("failedDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Branched/" + "failedDataset/" + fileName + ".java"), REPLACE_EXISTING);
                brfailed++;
            }
            else if(filePath.contains("Multi-path-Loop") && filePath.contains("succDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Multi-path-Loop/" + "succDataset/" + fileName + ".java"), REPLACE_EXISTING);
                musucc++;
            }
            else if(filePath.contains("Multi-path-Loop") && filePath.contains("failedDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Multi-path-Loop/" + "failedDataset/" + fileName + ".java"), REPLACE_EXISTING);
                mufailed++;
            }
            else if(filePath.contains("Nested-Loop") && filePath.contains("succDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Nested-Loop/" + "succDataset/" + fileName + ".java"), REPLACE_EXISTING);
                nlsucc++;
            }
            else if(filePath.contains("Nested-Loop") && filePath.contains("failedDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Nested-Loop/" + "failedDataset/" + fileName + ".java"), REPLACE_EXISTING);
                nlfailed++;
            }
            else if(filePath.contains("Sequential") && filePath.contains("succDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Sequential/" + "succDataset/" + fileName + ".java"), REPLACE_EXISTING);
                sesucc++;
            }
            else if(filePath.contains("Sequential") && filePath.contains("failedDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Sequential/" + "failedDataset/" + fileName + ".java"), REPLACE_EXISTING);
                sefailed++;
            }
            else if(filePath.contains("Single-path-Loop") && filePath.contains("succDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Single-path-Loop/" + "succDataset/" + fileName + ".java"), REPLACE_EXISTING);
                splsucc++;
            }
            else if(filePath.contains("Single-path-Loop") && filePath.contains("failedDataset")){
                Files.copy(Path.of(filePath),Path.of(targetDir+ "/Single-path-Loop/" + "failedDataset/" + fileName + ".java"), REPLACE_EXISTING);
                splfailed++;
            }
        }

        System.out.println("total = " + (brsucc + brfailed) + "\t\t" + "brsucc = " + brsucc + "\t\t" + "brfailed = " + brfailed + "\t\t" + "succRate = " + (brsucc * 100.0 / (brsucc + brfailed)) + "%");
        System.out.println("total = " + (musucc + mufailed) + "\t\t" + "musucc = " + musucc + "\t\t" + "mufailed = " + mufailed + "\t\t" + "succRate = " + (musucc * 100.0 / (musucc + mufailed)) + "%");
        System.out.println("total = " + (nlsucc + nlfailed) + "\t\t" + "nlsucc = " + nlsucc + "\t\t" + "nlfailed = " + nlfailed + "\t\t" + "succRate = " + (nlsucc * 100.0 / (nlsucc + nlfailed)) + "%");
        System.out.println("total = " + (sesucc + sefailed) + "\t\t" + "sesucc = " + sesucc + "\t\t" + "sefailed = " + sefailed + "\t\t" + "succRate = " + (sesucc * 100.0 / (sesucc + sefailed)) + "%");
        System.out.println("total = " + (splsucc + splfailed) + "\t\t" + "splsucc = " + splsucc + "\t\t" + "splfailed = " + splfailed + "\t\t" + "succRate = " + (splsucc * 100.0 / (splsucc + splfailed)) + "%");

    }

    public static Set<String> getCategoriesInDatasetDir(String datasetDir){
        Set<String> categories = new HashSet<>();
        java.io.File dir = new java.io.File(datasetDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在或不是一个目录: " + datasetDir);
            return categories;
        }
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if(!file.isDirectory()){
                    continue;
                }
                String categoryName = file.getName();
                categories.add(categoryName);
            }
        } else {
            System.err.println("no categroies dir: " + datasetDir);
        }
        return categories;
    }

    public static void printFSF(List<String[]> FSF){
        for (int i = 0; i < FSF.size(); i++) {
            String T = FSF.get(i)[0];
            String D = FSF.get(i)[1];
            System.out.println("T"+ i + ": " + T);
            System.out.println("D"+ i + ": " + D + "\n");
        }
    }

    public static void clearCurrentExperimentTmpFiles(String experimentName,String category,String modelName) throws IOException {
        System.out.println("Clearing the temporary files of current experiment: " + experimentName + " in category: " + category + " for model: " + modelName);
        File[] files = fetchAllJavaFilesInDir(SUCC_DATASET_DIR);
        for (File file : files) {
            Files.delete(file.toPath());
        }
        files = fetchAllJavaFilesInDir(FAILED_DATASET_DIR);
        for (File file : files) {
            Files.delete(file.toPath());
        }
        files = fetchAllJavaFilesInDir(Exception_DATASET_DIR);
        for (File file : files) {
            Files.delete(file.toPath());
        }
        files = fetchTxtFileInDir(LOG_DIR + "/" + modelName);
        if(files != null){
            for (File file : files) {
                Files.delete(file.toPath());
            }
        }
    }

    public static void collectExperimentRecords(String category,String experimentName,String modelName){
        //创建实验记录目录
        String experimentDir = getExperimentLogPath(experimentName,category);
        try {
            Files.createDirectories(Path.of(experimentDir));
        } catch (IOException e) {
            System.out.println("创建实验记录目录失败: " + experimentDir);
            return;
        }
        //将failedDataset、succDataset、exceptionDataset复制过来
        String experimentSuccDatasetDir = experimentDir + "/succDataset";
        String experimentFailedDatasetDir = experimentDir + "/failedDataset";
        String experimentExceptionDatasetDir = experimentDir + "/exceptionDataset";
        try {
            Files.createDirectories(Path.of(experimentSuccDatasetDir));
            Files.createDirectories(Path.of(experimentFailedDatasetDir));
            Files.createDirectories(Path.of(experimentExceptionDatasetDir));
        } catch (IOException e) {
            System.out.println("创建实验记录数据集目录失败");
            e.printStackTrace();
            return;
        }
        try {
            File[] files = fetchAllJavaFilesInDir(SUCC_DATASET_DIR);
            for (File file : files) {
                Files.copy(file.toPath(), Path.of(experimentSuccDatasetDir + "/" + file.getName()), REPLACE_EXISTING);
            }
            files = fetchAllJavaFilesInDir(FAILED_DATASET_DIR);
            for (File file : files) {
                Files.copy(file.toPath(), Path.of(experimentFailedDatasetDir + "/" + file.getName()), REPLACE_EXISTING);
            }
            files = fetchAllJavaFilesInDir(Exception_DATASET_DIR);
            for (File file : files) {
                Files.copy(file.toPath(), Path.of(experimentExceptionDatasetDir + "/" + file.getName()), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("复制实验记录失败");
            e.printStackTrace();
        }
        //把log文件复制过来
        String logDir = LOG_DIR + "/" + modelName;
        String experimentLogDir = experimentDir + "/" + modelName;
        try {
            Files.createDirectories(Path.of(experimentLogDir));
        } catch (IOException e) {
            System.out.println("创建实验记录日志目录失败: " + experimentLogDir);
            return;
        }
        try {
            File[] logFiles = fetchTxtFileInDir(logDir);
            if(logFiles != null){
                for (File logFile : logFiles) {
                    Files.copy(logFile.toPath(), Path.of(experimentLogDir +"/" + logFile.getName()), REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            System.out.println("复制实验记录失败");
            e.printStackTrace();
        }
        //创建summary.txt文件
        String summaryFilePath = experimentDir + "/summary.txt";
        Path summaryPath = Path.of(summaryFilePath);
        try {
            if(Files.exists(summaryPath)) {
                Files.delete(summaryPath); //如果存在则删除
            }
            Files.createFile(summaryPath);
        } catch (IOException e) {
            System.out.println("创建实验记录summary.txt失败");
            e.printStackTrace();
        }
        int succNum = 0, failedNum = 0, exceptionNum = 0, totalNum = 0;
        int succRate = 0;
        try {
            succNum = fetchAllJavaFilesInDir(experimentSuccDatasetDir).length;
            failedNum = fetchAllJavaFilesInDir(experimentFailedDatasetDir).length;
            exceptionNum = fetchAllJavaFilesInDir(experimentExceptionDatasetDir).length;
            totalNum = succNum + failedNum + exceptionNum;
            succRate =  (int)((float)succNum / (float)totalNum * 10000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //向summary.txt中写入信息
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                summaryPath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write("experimentName: " + experimentName);
            writer.newLine();
            writer.write("model: " + modelName);
            writer.newLine();
            writer.write("category: " + category);
            writer.newLine();
            writer.write("total number: " + totalNum);
            writer.newLine();
            writer.write("success number: " + succNum);
            writer.newLine();
            writer.write("success rate: " + (float)succRate / 100 + "%");
            writer.newLine();
            writer.write("failed number: " + failedNum);
            writer.newLine();
            writer.write("exception number: " + exceptionNum);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("写入实验记录summary.txt失败");
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
//        classifyHandledCodeFiles("resources/dataset/4-shot/failedDataset");
//        classifyHandledCodeFiles("resources/dataset/4-shot/succDataset");
//        moveClassifiedCode();
//        Set<String> categoriesInDatasetDir = getCategoriesInDatasetDir("resources/dataset/0801-dataset");
//        for (String category : categoriesInDatasetDir) {
//            System.out.println(category);
//        }
        clearCurrentExperimentTmpFiles("080101-conversational","Single-path-Loop","deepseek-chat");
    }

    public static String getExperimentLogPath(String experimentName,String category) {
        return  EXPERIMENT_DIR + "/" + experimentName + "/" + category ;
    }
}
