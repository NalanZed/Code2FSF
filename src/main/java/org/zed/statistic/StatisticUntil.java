package org.zed.statistic;

import org.zed.log.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.zed.log.LogManager.getCategoriesInDatasetDir;
import static org.zed.log.LogManager.getClassifiedProgramsNames;

public class StatisticUntil {

    public static void statisticSpecgenCSV(String csvFilePath,String sourceDataset) throws IOException {
        HashMap<String, HashSet<String>> classifiedProgramsNames = new HashMap<>();
        classifiedProgramsNames = getClassifiedProgramsNames(sourceDataset);
        Set<String> categoriesInDatasetDir = getCategoriesInDatasetDir(sourceDataset);
        HashMap<String,Integer[]> categoryCounts = new HashMap<>();
        //0: total, 1: success, 2: failed
        for(String category : categoriesInDatasetDir) {
            categoryCounts.put(category, new Integer[]{0, 0, 0});
        }
        //读取CSV文件
        List<String> lines = Files.readAllLines(Path.of(csvFilePath));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length < 2) continue; // 确保有足够的列
            String className = parts[0].substring(parts[0].indexOf("-") + 1, parts[0].lastIndexOf("-"));
            String category = "";
            for (String cat : classifiedProgramsNames.keySet()) {
                if (classifiedProgramsNames.get(cat).contains(className)) {
                    category = cat;
                    break;
                }
            }
            categoryCounts.get(category)[0]++;
            if (parts[1].trim().equals("1")) {
                categoryCounts.get(category)[1]++;
            } else {
                categoryCounts.get(category)[2]++;
            }
        }
        //输出统计结果
        System.out.println("Category,Total,Success,Failed,successProb");
        for (String category : categoriesInDatasetDir) {
            Integer[] counts = categoryCounts.get(category);
            if (counts[0] == null) counts[0] = 0;
            if (counts[1] == null) counts[1] = 0;
            if (counts[2] == null) counts[2] = 0;
            System.out.println(category + "," + counts[0] + "," + counts[1] + "," + counts[2] + "," + (counts[1] * 100.0 / counts[0]) + "%");
        }
    }

    public static void classifyLogsOfSpecgen(String datasetDir,String logDir) throws IOException {
        HashMap<String, HashSet<String>> classifiedProgramsNames = new HashMap<>();
        classifiedProgramsNames = getClassifiedProgramsNames(datasetDir);
        Set<String> categoriesInDatasetDir = getCategoriesInDatasetDir(datasetDir);
        //创建各类别目录
        for(String category : categoriesInDatasetDir) {
            Path categoryPath = Path.of(logDir, category);
            if (!Files.exists(categoryPath)) {
                Files.createDirectories(categoryPath);
            }
        }
        //遍历日志文件
        HashMap<String, HashSet<String>> finalClassifiedProgramsNames = classifiedProgramsNames;
        Files.list(Path.of(logDir)).filter(path -> (path.toString().endsWith(".txt") && path.toString().contains("log")) ).forEach(path -> {
            String fileName = path.getFileName().toString();
            String className = fileName.substring(fileName.indexOf("-") + 1, fileName.lastIndexOf("-"));
            String category = "";
            for (String cat : finalClassifiedProgramsNames.keySet()) {
                if (finalClassifiedProgramsNames.get(cat).contains(className)) {
                    category = cat;
                    break;
                }
            }
            if (!category.isEmpty()) {
                try {
                    Files.move(path, Path.of(logDir, category, fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void specgenLogSuccessFailClassify(String logDir,String csvFilePath) throws IOException {
        String logSuccDir = logDir + "/succ";
        String logFailedDir = logDir + "/failed";
        //读取metadata.csv文件
        try {
            Files.createDirectories(Path.of(logSuccDir));
            Files.createDirectories(Path.of(logFailedDir));
        } catch (IOException e) {
            System.out.println("创建日志分类目录失败");
            return;
        }
        //读取每行
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Path.of(csvFilePath));
        } catch (IOException e) {
            System.out.println("读取metadata.csv文件失败");
            return;
        }
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length < 2) continue; // 确保有足够的列
            String succFlag = parts[1].trim();
            if(succFlag.equals("1")){
                //复制到logSuccDir
                String logFilePath = logDir + "/" + parts[0];
                String targetPath = logSuccDir + "/" + parts[0];
                if(!Files.exists(Path.of(logFilePath))){
                    continue;
                }
                Files.move(Path.of(logFilePath), Path.of(targetPath), REPLACE_EXISTING);
            }else{
                //复制到logFailedDir
                String logFilePath = logDir + "/" + parts[0];
                String targetPath = logFailedDir + "/" + parts[0];
                if(!Files.exists(Path.of(logFilePath))){
                    continue;
                }
                Files.move(Path.of(logFilePath), Path.of(targetPath), REPLACE_EXISTING);
            }
        }
    }

    public static void keepOnlyOneLogPerClass(String logDir) throws IOException {
        //遍历日志文件
        HashSet<String> seenClasses = new HashSet<>();
        Files.list(Path.of(logDir)).filter(path -> (path.toString().endsWith(".txt") && path.toString().contains("log")) ).forEach(path -> {
            //如果是空文件，直接删除
            try {
                if (Files.size(path) == 0) {
                    Files.delete(path);
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            String fileName = path.getFileName().toString();
            String className = fileName.substring(fileName.indexOf("-") + 1, fileName.lastIndexOf("-"));
            if (seenClasses.contains(className)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                seenClasses.add(className);
            }
        });
    }

    public static void doSomeStatistic(String categoryLogDir){
        String succDir = categoryLogDir + "/succ";
        String failedDir = categoryLogDir + "/failed";
        //统计succDir下的日志文件数量
        File[] succFiles = LogManager.fetchTxtFileInDir(succDir);
        int succCount = succFiles.length;
        File[] failedFiles = LogManager.fetchTxtFileInDir(failedDir);
        int failedCount = failedFiles.length;
        int totalCount = succCount + failedCount;
        double successProb = totalCount == 0 ? 0.0 : (succCount * 100.0 / totalCount);
        //写入 summary.txt
        String summaryFilePath = categoryLogDir + "/summary.txt";
        String summaryContent = "Total: " + totalCount + "\n" +
                "Success: " + succCount + "\n" +
                "Failed: " + failedCount + "\n" +
                "Success Rate: " + successProb + "%\n";
        try {
            Files.writeString(Path.of(summaryFilePath), summaryContent, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("写入summary.txt失败");
        }
    }

    public static void specgenLogClassifyAndStatistic(String experimentDir,String datasetName,String logName) throws IOException {
        String datasetDir = experimentDir + "/" + datasetName;
        String logsDir = experimentDir + "/" + logName;
        String csvFilePath = logsDir + "/metadata.csv";

        //日志按程序类别分类
        try {
            classifyLogsOfSpecgen(datasetDir,logsDir);
        } catch (IOException e) {
            System.out.println("日志按程序类别分类失败");
            return;
        }
        //拿到所有分类
        Set<String> categories = null;
        categories = getCategoriesInDatasetDir(datasetDir);
        if(categories.isEmpty()) {
            System.out.println("无子分类!");
            //日志按成功与否分类
            try {
                keepOnlyOneLogPerClass(logsDir);
            } catch (IOException e) {
                System.out.println("保留每个类的一个日志失败");
                return;
            }
            specgenLogSuccessFailClassify(logsDir,csvFilePath);
            doSomeStatistic(logsDir);
            return;
        }else {
            //日志按成功与否分类
            for(String category : categories) {
                try {
                    keepOnlyOneLogPerClass(logsDir + "/" + category);
                } catch (IOException e) {
                    System.out.println("每个类日志仅保留一份失败, 当前所操作的分类 =" + category);
                    continue;
                }
                String categoryLogDir = logsDir + "/" + category;
                specgenLogSuccessFailClassify(categoryLogDir,csvFilePath);
                doSomeStatistic(categoryLogDir);
            }
        }
    }

    public static int calcuConversationalRounds(String logFilePath){
        //有几个 start role assistant 就是几轮
        int rounds = 0;
        try {
            List<String> lines = Files.readAllLines(Path.of(logFilePath));
            for(String line : lines) {
                if(line.contains("start role assistant")) {
                    rounds++;
                }
            }
            return rounds;
        } catch (IOException e) {
            System.out.println("读取日志文件失败");
            return -1;
        }
    }
    public static int calcuConversationalRoundsForSpecgen(String logFilePath){
        //有几个public class 就是几轮
        int rounds = 0;
        try {
            List<String> lines = Files.readAllLines(Path.of(logFilePath));
            for(String line : lines) {
                if(line.contains("public class")) {
                    rounds++;
                }
            }
            return rounds > 10 ? 10 : rounds;
        } catch (IOException e) {
            System.out.println("读取日志文件失败");
            return -1;
        }
    }

    public static double recordAverageConversationalRoundsForSpecgen(String logDir) {
        File[] succLogFiles = LogManager.fetchTxtFileInDir(logDir + "/succ");
        File[] failedLogFiles = LogManager.fetchTxtFileInDir(logDir + "/failed");
        File[] logFiles = new File[succLogFiles.length + failedLogFiles.length];
        System.arraycopy(succLogFiles, 0, logFiles, 0, succLogFiles.length);
        System.arraycopy(failedLogFiles, 0, logFiles, succLogFiles.length, failedLogFiles.length);
        if (logFiles.length == 0) return -0.01;
        int totalRounds = 0;
        int validFiles = 0;
        for (File logFile : logFiles) {
            int rounds = calcuConversationalRoundsForSpecgen(logFile.getAbsolutePath());
            if (rounds != -1) {
                totalRounds += rounds;
                validFiles++;
            }
        }
        if (validFiles == 0) return -0.01;
        double averageRounds = totalRounds * 1.0 / validFiles;
        return averageRounds;
    }



    public static double recordAverageConversationalRounds(String logDir){
        File[] logFiles = LogManager.fetchTxtFileInDir(logDir);
        if(logFiles.length == 0) return -0.01;
        int totalRounds = 0;
        int validFiles = 0;
        for(File logFile : logFiles) {
            int rounds = calcuConversationalRounds(logFile.getAbsolutePath());
            if(rounds != -1) {
                totalRounds += rounds;
                validFiles++;
            }
        }
        if(validFiles == 0) return -0.01;

        double averageRounds = totalRounds * 1.0 / validFiles;
//        String summaryFilePath = logDir + "/conversationRounds.txt";
//        String content = "Total rounds: "+ totalRounds +
//                "\nValid Files: " + validFiles +
//                "\nAverage Conversational Rounds: " + averageRounds + "\n";
//        try {
//            Files.writeString(Path.of(summaryFilePath), content, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
//        } catch (IOException e) {
//            System.out.println("conversationRounds.txt失败");
//        }
        return averageRounds;
    }

    public static void main(String[] args) throws IOException {
//        String experimentDir = "/Users/jiazedong/Desktop/cf可用数据集合/实验记录/0826-sg-gpt-BSSM/";
//        String datasetName = "0826-dataset-BSSM";
//        String logName = "logs-082604-sg-gpt-BSSM";
//        specgenLogClassifyAndStatistic(experimentDir,datasetName,logName);

//        String experimentDir = "/Users/jiazedong/Desktop/cf可用数据集合/实验记录/code2fsf-0909-experiments/code2fsf-conversational";
//        String datasetName = "";
//        String logName = "082703-NL";
//        specgenLogClassifyAndStatistic(experimentDir,datasetName,logName);

        String experimentDir = "/Users/jiazedong/Desktop/cf可用数据集合/实验记录/code2fsf-0909-experiments/specgen";
//        String type = "Branched";
//        String type = "Sequential";
        String type = "Single-path-Loop";
//        String type = "Multi-path-Loop";
//        String type = "NestedLoop";
        double avgRounds = 0.0;
        String logDir = experimentDir + "/082601-sg-gpt/" + type;
        avgRounds += recordAverageConversationalRoundsForSpecgen(logDir);

        logDir = experimentDir + "/082602-sg-gpt/"+ type;
        avgRounds += recordAverageConversationalRoundsForSpecgen(logDir);
        logDir = experimentDir + "/082603-sg-gpt/"+ type;
        avgRounds += recordAverageConversationalRoundsForSpecgen(logDir);
        System.out.println("avgRounds:" + avgRounds);
        avgRounds /= 3.0;
        System.out.println("avgRounds:" + avgRounds);


//        String logDir1 = experimentDir + "/" + "080502-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080503-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080504-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080601-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080603-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080604-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080605-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080607-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080608-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        logDir1 = experimentDir + "/" + "080610-conversational" + "/" + type + "/gpt-4o";
//        avgRounds += recordAverageConversationalRounds(logDir1);
//        System.out.println(avgRounds);


//        String logDir2 = "resources/experiment/080502-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir2);
//        String logDir3 = ""+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir3);
//        String logDir4 = "resources/experiment/080502-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir4);
//        String logDir5 = "resources/experiment/081105-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir5);
//        String logDir6 = "resources/experiment/081106-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir6);
//        String logDir7 = "resources/experiment/081107-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir7);
//        String logDir8 = "resources/experiment/081108-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir8);
//        String logDir9 = "resources/experiment/081109-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir9);
//        String logDir0 = "resources/experiment/081110-evolution/"+ type + "/gpt-4o";
//        recordAverageConversationalRounds(logDir0);
    }
}
