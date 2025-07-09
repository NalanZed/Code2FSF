package org.zed.log;


import org.zed.llm.ModelMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LogManager {

    private static final String RESOURCE_DIR = "resources";
    private static final String LOG_DIR = RESOURCE_DIR + "/" +"log";
    private static final String TRANS_WORK_DIR = RESOURCE_DIR + "/" + "trans";
    private static final String LOG_FILE_SUFFIX = ".txt";
    private static final String ADDED_PRINT_CODES_DIR = TRANS_WORK_DIR + "/"+ "addedPrintCodes";
    private static final String TRANS_SOURCE_CODES_DIR = TRANS_WORK_DIR + "/"+ "sourceCodes";
    private static final String SUCC_DATASET_DIR = RESOURCE_DIR + "/" + "succDataset";
    private static final String FAILED_DATASET_DIR = RESOURCE_DIR + "/" + "failedDataset";


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
        String[] specs = msgContent.split("\n");
        int i = 0;
        while (i < specs.length) {
            if(specs[i].startsWith("T")){
                String[] TD = new String[2];
                TD[0] = specs[i].substring(specs[i].lastIndexOf(":")+1).trim();
                TD[1] = specs[i+1].substring(specs[i+1].lastIndexOf(":")+1).trim();
                TDs.add(TD);
                i += 2;
            }else {
                i++;
            }
        }
        return TDs;
    }
    public static String getLastestAssistantMsgFromLog(String logFilePath){
        String logString = file2String(logFilePath);
        int lastIndexOfAssisStart = logString.lastIndexOf("start role assistant") + "start role assistant".length();
        int lastIndexOfAssisEnd = logString.lastIndexOf("*end* role assistant");
        return logString.substring(lastIndexOfAssisStart + 1, lastIndexOfAssisEnd);
    }

    public static List<String[]> getLastestFSFFromLog(String logFilePath){
        String content = getLastestAssistantMsgFromLog(logFilePath);
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
        String program = logString.substring(logString.indexOf("public class"), logString.indexOf( "```\n"+
                "*end* role user"));
        return program;
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

    public static String codePath2SuccPath(String codePath) {
        //从文件路径中提取文件名（在Java程序中，即类名）
        String title = codePath.substring(codePath.lastIndexOf("/") + 1);
        return SUCC_DATASET_DIR  + "/" + title;
    }

    public static void saveHistoryTestcases(String codePath,List<String> testCases){
        int totalNum = testCases.size();
        int count = 0;
        System.out.println(codePath + "的测试用例历史记录如下，共[" + totalNum + "]个");
        for (String testcase : testCases) {
            System.out.println("------------------["+(++count) +"/"+totalNum+"]------------------");
            System.out.println(testcase);
        }
        System.out.println("----------------------------------------");
    }

//    public static void main(String[] args) {
//        String content = getLastestAssistantMsgFromLog(LOG_DIR + "/" + "deepseek-chat/"+"log-Abs.txt");
//        List<String[]> TDs = parseTD(content);
//        for (String[] TD : TDs) {
//            System.out.println("T: " + TD[0]);
//            System.out.println("D: " + TD[1]);
//        }
//    }

}
