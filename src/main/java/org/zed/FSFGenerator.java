package org.zed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.trans.TransWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.zed.tcg.ExecutionEnabler.generateMainMdUnderExpr;
import static org.zed.tcg.ExecutionEnabler.insertMainMdInSSMP;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;

public class FSFGenerator {
    static final String YELLOW = "\u001B[33m";
    static final String RESET = "\u001B[0m";
    private static final String LLMS_CONFIG_DIR = "resources/config";

    public static HashMap<String, ModelConfig> modelConfigs;

    public static void initModelConfig(String modelConfigDir){
        FSFGenerator.modelConfigs = ModelConfig.GetAllModels(modelConfigDir);
    }

    public static ModelConfig initModel(String model){
        initModelConfig(LLMS_CONFIG_DIR);
        makeSureModelIsAvailable(model);
        return modelConfigs.get(model);
    }

    public static void checkArgsAndSetDefault(HashMap<String,String> argsMap) {
        if(!argsMap.containsKey("model")){
            argsMap.put("model", "deepseek-chat");
        }
        if(!argsMap.containsKey("input")){
            argsMap.put("input", "resources/dataset/Example.java");
        }
        if(!argsMap.containsKey("maxRounds")){
            argsMap.put("maxRounds", "1");
        }
    }

    //处理输入参数，写入HashMap方便读取
    public static HashMap<String,String> handleArgs(String[] args){
        HashMap<String,String> argsMap = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if ("--input".equals(args[i])) {
                argsMap.put("input", args[i + 1]);
            }
            else if("--model".equals(args[i])){
                argsMap.put("model", args[i + 1]);
            }
            else if("--maxRounds".equals(args[i])){
                argsMap.put("maxRounds", args[i + 1]);
            }
            else if("--inputDir".equals(args[i])){
                argsMap.put("inputDir", args[i + 1]);
            }
            else if("-t".equals(args[i])){
                argsMap.put("testMode", args[i + 1]);
            }
        }
        //检查输入参数，设置默认值
        checkArgsAndSetDefault(argsMap);
        return argsMap;
    }

    public static Result valid1Path(String ssmp,String mainMd,List<String> prePathConstrains,String T,String D) throws Exception {
        //给测试函数插桩
        String addedPrintProgram = addPrintStmt(ssmp);
        //组装可执行程序
        String runnableProgram = insertMainMdInSSMP(addedPrintProgram, mainMd);
        System.out.println("runnableProgram: " + runnableProgram);
        //拿到SpecUnit
        SpecUnit su = new SpecUnit(runnableProgram,T,D,prePathConstrains);
        Result r = callTBFV4J(su);
        System.out.println("验证返回 result: " + r);
        return r;
    }

    public static String FSFValidationTask(String ssmp,List<String[]> FSF){
        //对FSF中T的互斥性进行验证
        FSFValidationUnit fsfValidationUnit = new FSFValidationUnit(ssmp, FSF);
        Result exclusivityResult = null;
        try {
            exclusivityResult = callTBFV4J(fsfValidationUnit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(exclusivityResult == null || exclusivityResult.getStatus() == -1){
            //FSF解析失败，都没到验证完备性和互斥性那一步
            return "检查到FSF中T不满足要求导致无法解析，请严格审视我最初的要求，并重新生成FSF";
        }
        if(exclusivityResult.getStatus() == 2){
            return "检查到FSF中T不满足互斥性,具体是 Ti && Tj :[" + exclusivityResult.getCounterExample()+ "] 有解" +
                    exclusivityResult.getPathConstrain() + "，请重新生成FSF，确保T之间的互斥性";
        }
        if(exclusivityResult.getStatus() == 3){
            return "检查到FSF中T用 || 连接不具有完备性，即" + "(" + exclusivityResult.getCounterExample() + ")" + "有解" +
                    exclusivityResult.getPathConstrain() + "，请重新生成FSF，确保T的完整性";
        }
        return "SUCCESS";
    }

    public static String validATAndD(String ssmp, String T, String D, int maxRoundsOf1CoupleOfTD, List<String> historyTestcases, List<Result> finalResultsOfTDs) throws Exception {
        int countOfPathValidated = 0;
        Result r = null;
        String currentTD = "T: " + T + "\n" + "D: " + D;
        List<String> prePathConstrains = new ArrayList<>();

        if("Exception".equals(D)){
            System.out.println("D 为 Exception");
            if(validateExceptionPath(ssmp, T)){
                finalResultsOfTDs.add(new Result(3,"Exception路径符合预期",""));
                return "SUCCESS";
            }
            //需要提示LLM，重新生成该TD组
            return "经验证，对于" + "T :" + T + "，此时程序没有抛出异常，请思考后重新生成";
        }

        while(countOfPathValidated < maxRoundsOf1CoupleOfTD){
            //对一个TD下所有路径验证
            //生成main方法，即测试用例
            String mainMd = generateMainMdUnderExpr(T,prePathConstrains,ssmp);
            historyTestcases.add(mainMd);
            if(mainMd == null){
                System.err.println("参数生成失败！");
                return "ERROR";
            }
            r = valid1Path(ssmp,mainMd,prePathConstrains,T,D);
            if(r.getStatus() == 0){
                prePathConstrains.add(r.getPathConstrain());
                countOfPathValidated++;
                System.out.println(currentTD + "====>" + "第[" + countOfPathValidated + "]条路径测试完毕");
            }
            else break;
        }
        if(r.getStatus() == 0 && countOfPathValidated == maxRoundsOf1CoupleOfTD){
            System.out.println("超出最大路径验证次数" + maxRoundsOf1CoupleOfTD + "结束对当前TD组的验证");
            finalResultsOfTDs.add(r);
            return "PARTIALLY SUCCESS";
        }
        if(r.getStatus() == 3){
            System.out.println(currentTD + "====>" + "验证通过");
            finalResultsOfTDs.add(r);
            return "SUCCESS";
        }
        if(r.getStatus() == 1){
            System.err.println(currentTD + "====>" + "验证过程出错！");
            finalResultsOfTDs.add(r);
            return "ERROR";
        }
        if(r.getStatus() == 2){
            System.out.println(currentTD + "\n" + "验证未通过, 需要重新生成");
            System.out.println("反例如下：\n" + r.getCounterExample());
            return "当变量赋值为"+r.getCounterExample()+"时，与" + currentTD + "相违背，请结合这个反例重新生成！";
        }
        if(r.getStatus() == -1){
            System.out.println(currentTD + "\n" + "验证过程中验证器报错");
            return "对" + currentTD + " 验证过程中出错，" + r.getCounterExample() + ", 请参考报错信息重新生成";
        }
        return "ERROR";
    }

    public static boolean isTotallyVerified(List<Result> finalResultsOfEveryCoupleOfTD){
        //对整个验证任务的评价，不应该局限在最后一个验证任务结果上
        //正确的方法是，要记录所有TD组验证的结果,逐个遍历
        boolean totallyVerified = true;
        for(Result res : finalResultsOfEveryCoupleOfTD){
            if(res.getStatus() == 1){
                System.out.println("validation task failed!");
                return false;
            }
            if(res.getStatus() == 0){
                totallyVerified = false;
            }
        }
        return totallyVerified;
    }

    public static boolean runConversations(int maxRounds, ModelConfig mc, String inputFilePath) throws Exception {
        String modelName = mc.getModelName();
        ModelPrompt fsfPrompt = ModelPrompt.generateCode2FSFPrompt(modelName,inputFilePath);
        String logPath = LogManager.codePath2LogPath(inputFilePath, modelName);
        String pureProgram = LogManager.file2String(inputFilePath);
        //确保是ssmp
        String ssmp = TransWorker.trans2SSMP(pureProgram);
        if(ssmp == null || ssmp.isEmpty()){
            System.out.println("转换为单静态方法程序失败，无法进行后续操作");
            return false;
        }
        List<String> historyTestcases = new ArrayList<>();
        int maxRoundsOf1CoupleOfTD = 5;
        List<String[]> FSF;
        int count = 1;
        while(count <= maxRounds){
            boolean regenerateFlag = false; //标记是否需要重新生成FSF
            System.out.println("["+ modelName +"]"+"正在进行第"+count+"轮对话");
            make1RoundConversation(fsfPrompt,mc);
            System.out.println("第"+count+"轮对话完成");
            try{
                FSF = LogManager.getLastestFSFFromLog(logPath);
                String FSFFormWrongMsg = "FSF生成的格式不符合我在前面说明的那些规范，请重新生成";
                if(!isTheFSFFormOK(FSF)){
                    ModelMessage msg = new ModelMessage("user", FSFFormWrongMsg);
                    fsfPrompt.addMessage(msg);
                    LogManager.appendMessage(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel());
                    continue;
                }
            }catch (Exception e){
                System.out.println("对话生成FSF失败，跳过本次任务");
                return false;
            }
            //对FSF中T的互斥性进行验证
            String FSFValidationResult = FSFValidationTask(ssmp,FSF);
            if(!FSFValidationResult.equals("SUCCESS")){
                ModelMessage msg = new ModelMessage("user", FSFValidationResult);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel());
                continue;
            }

            count++;
            String T = "";
            String D = "";
            List<Result> finalResultsOfEveryCoupleOfTD = new ArrayList<>();//记录每个TD对的验证结果
            //对每一个TD进行验证
            for(String[] td : FSF) {
                T = td[0];
                D = td[1];
                String currentTD = "T: " + T + "\n" + "D: " + D;
                System.out.println(YELLOW + "正在进行验证的TD对为：" + currentTD + RESET);
                String validationTDResult = validATAndD(ssmp, T, D, maxRoundsOf1CoupleOfTD,
                                                                    historyTestcases, finalResultsOfEveryCoupleOfTD);
                if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
                    continue;
                }
                if(validationTDResult.equals("ERROR")){
                    System.err.println(currentTD + "\n" + "========>验证过程出现严重错误，跳过本次任务");
                    return false;
                }
                regenerateFlag = true;
                ModelMessage msg = new ModelMessage("user", validationTDResult);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(inputFilePath,msg,fsfPrompt.getModel());
                break;
            }
            if(regenerateFlag){
                //因为某些原因，需要重新进行一轮对话生成FSF
                continue;
            }
            String verifyType = isTotallyVerified(finalResultsOfEveryCoupleOfTD) ? "totally verified!" : "partially verified!";
            System.out.println(inputFilePath + "is" + verifyType);
            //记录历史测试用例
            LogManager.saveHistoryTestcases(inputFilePath,historyTestcases);
            return true;
        }
        System.err.println("对话轮数超过最大值" + maxRounds + "，任务失败!");
        return false;
    }

    private static boolean isTheFSFFormOK(List<String[]> fsf) {
//        for(String[] couple : fsf){
//            String T = couple[0];
//            String D = couple[1];
//            if(T.contains("?")) return false;
//        }
        return true;
    }

    private static boolean validateExceptionPath(String ssmp, String t) {
        String mainMd = generateMainMdUnderExpr(t,null,ssmp);
        if(mainMd == null){
            System.out.println("输入约束条件[" + t + "]下生成测试用例失败, 默认为异常路径");
            return true;
        }
        try {
            Result result = valid1Path(ssmp, mainMd, null, t, "Exception");
            if(result == null){
                System.out.println("验证过程发生错误，没有返回result");
                return false;
            }
            if(result.getStatus() == 0){
                return true;
            }
            if(result.getStatus() == 1){
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static void make1RoundConversation(ModelPrompt prompt,ModelConfig mc){
        ModelClient client = new ModelClient(mc);
        try {
            //调用DeepSeekApi得到回复
            ModelResponse response = client.call(prompt);
            //将json格式的回复转化为对象chatResponse
            //这里指定第一个choice做记录，因为对话中只可能收到一个choice
            ModelMessage respMsg = response.getChoices().get(0).getMessage();
            //将回复msg写入deepseekRequest
            prompt.getMessages().add(respMsg);
            //将回复写入日志
            LogManager.appendMessage(prompt.getCodePath(), respMsg,prompt.getModel());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runConversationForDir(int maxRounds, ModelConfig mc, String inputDir) throws Exception {
        // 遍历输入目录下的所有文件
        int taskCount = 0;
        String[] filePaths = LogManager.fetchSuffixFilePathInDir(inputDir,".java");
        int totalTaskNum = filePaths.length;
        for (String filePath : filePaths) {

            System.out.println("Processing file: " + filePath + " (" + (++taskCount) + "/" + totalTaskNum + ")");
            String canNotHandleFilePath = LogManager.codePath2FailedPath(filePath);
            String handledFilePath = LogManager.codePath2SuccPath(filePath);
//            if(Files.exists(Path.of(canNotHandleFilePath))){
//                System.out.println("文件已存在于failedDataset目录中，跳过");
//                continue;
//            }
            if(Files.exists(Path.of(handledFilePath))){
                System.out.println("文件已存在于succDataset目录中，跳过");
                continue;
            }
            try{
                boolean succ = runConversations(maxRounds, mc, filePath);
                if(succ) {
                    System.out.println("将成功的代码保存到 SuccDataset 目录下");
                    LogManager.copyFileToSuccDataset(filePath);
                } else {
                    System.err.println("将失败的代码保存到 FailedDataset 目录下");
                    LogManager.copyFileToFailedDataset(filePath);
                }
            }catch (IOException e){
                System.err.println("Error during runConversations for file: " + filePath);
                System.err.println("将失败的代码保存到 resources/failedDataset 目录下");
                LogManager.copyFileToFailedDataset(filePath);
            }
        }
    }

    public static boolean FSFReview(ModelConfig mc,String inputFilePath) throws IOException {
        String modelName = mc.getModelName();
        int maxRounds = 10;
        int failOnMutualExclusivityTimes = 0;
        int failOnCompletenessTimes = 0;
        String FSFReviewLogDir = "resources/log/FSFReview";
        ModelPrompt fsfPrompt = null;
        try {
            fsfPrompt = ModelPrompt.generateCode2FSFPrompt(modelName,inputFilePath);
        } catch (IOException e) {
            System.out.println("生成prompt失败");
            return false;
        }
        String logPath = LogManager.codePath2DiyLogPath(inputFilePath, modelName,FSFReviewLogDir);
        String pureProgram = LogManager.file2String(inputFilePath);
        //确保是ssmp
        String ssmp = TransWorker.trans2SSMP(pureProgram);
        if(ssmp == null || ssmp.isEmpty()){
            System.out.println("转换为单静态方法程序失败，无法进行后续操作");
            return false;
        }
        List<String[]> FSF;
        int count = 0;
        while(count <= maxRounds){
            System.out.println("["+ modelName +"]"+"正在进行第"+(++count)+"轮对话");
            ModelClient client = new ModelClient(mc);
            try {
                //调用DeepSeekApi得到回复
                ModelResponse response = client.call(fsfPrompt);
                //将json格式的回复转化为对象chatResponse
                //这里指定第一个choice做记录，因为对话中只可能收到一个choice
                ModelMessage respMsg = response.getChoices().get(0).getMessage();
                //将回复msg写入deepseekRequest
                fsfPrompt.getMessages().add(respMsg);
                //将回复写入日志
                LogManager.appendMessageInDiyDir(fsfPrompt.getCodePath(), respMsg,fsfPrompt.getModel(),FSFReviewLogDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("第"+count+"轮对话完成");
            try{
                FSF = LogManager.getLastestFSFFromLog(logPath);
            }catch (Exception e){
                System.out.println("对话生成FSF失败，跳过本次任务");
                return false;
            }
            //对FSF中T的互斥性进行验证
            FSFValidationUnit fsfValidationUnit = new FSFValidationUnit(ssmp, FSF);
            Result exclusivityResult = null;
            try {
                exclusivityResult = callTBFV4J(fsfValidationUnit);
            } catch (IOException e) {
                System.out.println("callTBFV4J 验证工作返回异常!");
                return false;
            }
            if(exclusivityResult == null){
                System.out.println("异常终止！");
                LogManager.appendFSFReviewSummary(inputFilePath, "异常终止", fsfPrompt.getModel(),FSFReviewLogDir);
                return false;
            }

            if(exclusivityResult.getStatus() == 2){
                String exclusivityWrongMsg = "检查到FSF中T不满足互斥性,具体是 Ti && Tj :[" + exclusivityResult.getCounterExample()+ "] 有解" +
                        exclusivityResult.getPathConstrain() + "，请重新生成FSF，确保T之间的互斥性";
                ModelMessage msg = new ModelMessage("user", exclusivityWrongMsg);
                fsfPrompt.addMessage(msg);
                try {
                    LogManager.appendMessageInDiyDir(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel(),FSFReviewLogDir);
                } catch (IOException e) {
                    System.out.println("记录日志失败");
                    return false;
                }
                failOnMutualExclusivityTimes++;
                continue;
            }
            if(exclusivityResult.getStatus() == 3){
                String msgStr = "检查到FSF中T用 || 连接不具有完备性，即" + "(" + exclusivityResult.getCounterExample() + ")" + "有解" +
                        exclusivityResult.getPathConstrain() + "，请重新生成FSF，确保T的完整性";
                ModelMessage msg = new ModelMessage("user", msgStr);
                fsfPrompt.addMessage(msg);
                try {
                    LogManager.appendMessageInDiyDir(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel(),FSFReviewLogDir);
                } catch (IOException e) {
                    System.out.println("记录日志失败");
                    return false;
                }
                failOnMutualExclusivityTimes++;
                continue;
            }
            String summary = "mutual exclusivity fail times = " + failOnMutualExclusivityTimes + "\n" +
                    "completeness fail times = " + failOnCompletenessTimes + "\n" +
                    "total conversation times = " + count;
            try {
                LogManager.appendFSFReviewSummary(inputFilePath,summary,mc.getModelName(),FSFReviewLogDir);
            } catch (IOException e) {
                System.out.println("记录日志失败");
                return false;
            }
            return true;
        }
        System.out.println("对话轮数超过最大值" + maxRounds + "，任务失败!");
        return true;
    }
    public static void FSFReviewOnDir(ModelConfig mc, String inputDir) throws Exception {
        // 遍历输入目录下的所有文件
        int taskCount = 0;
        String[] filePaths = LogManager.fetchSuffixFilePathInDir(inputDir,".java");
        int totalTaskNum = filePaths.length;
        for (String filePath : filePaths) {
            System.out.println("Processing file: " + filePath + " (" + (++taskCount) + "/" + totalTaskNum + ")");
            boolean checkOK = FSFReview(mc, filePath);
            if(!checkOK){
                LogManager.appendFSFReviewSummary("neverSuccess.java",filePath,mc.getModelName(),"log/FSFReview");
            }
        }
    }
    public static void makeSureModelIsAvailable(String model) {
        if(!modelConfigs.containsKey(model)){
            System.out.println("Model " + model + " is not available. Please check your model configuration.");
            System.out.println("\u001B[33m**当前支持的模型有:\u001B[0m");
            for(String m : modelConfigs.keySet()){
                System.out.println("\u001B[33m**\t" + m + "\u001B[0m" );
            }
            System.exit(1);
        }
    }

    public static Result callTBFV4J(SpecUnit su) throws IOException {
        Result res =null;

        String suJson = new ObjectMapper().writeValueAsString(su);
//        ProcessBuilder pb = new ProcessBuilder("python3", "resources/dynamic_testing.py", "--specunit",suJson);
        ProcessBuilder pb = new ProcessBuilder("python3", "resources/z3_validation_runner.py", "--specunit",suJson);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder errorInfo = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            if(line.startsWith("result:")){
                String resultJson = line.substring("result:".length()).trim();
                res = new Result(resultJson);
            }
            System.out.println(line);
        }

        // 读取错误信息
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while((line = errReader.readLine()) != null){
            System.err.println("Error: " + line);
            errorInfo.append(line).append("\n");
        }

        // 等待进程结束
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(res == null && !errorInfo.toString().isEmpty()){
            System.out.println("没有收到TBFV给出的 result");
            res = new Result(-1,"z3验证器验证过程中报错：\n" + errorInfo, "");
        }
        return res;
    }
    public static Result callTBFV4J(FSFValidationUnit fu) throws IOException {
        Result res =null;
        String fuJson = fu.toJson();
        ProcessBuilder pb = new ProcessBuilder("python3", "resources/z3_validation_runner.py", "--fu",fuJson);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = reader.readLine()) != null){
            if(line.startsWith("FSF validation result:")){
                String resultJson = line.substring("FSF validation result:".length()).trim();
                res = new Result(resultJson);
            }
            System.out.println(line);
        }

        if(res == null){
            System.out.println("没有收到TBFV给出的 result");
        }

        // 读取错误信息
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while((line = errReader.readLine()) != null){
            System.err.println("Error: " + line);
        }

        // 等待进程结束
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        HashMap<String,String> argsMap = FSFGenerator.handleArgs(args);
        int maxRounds = Integer.parseInt(argsMap.get("maxRounds"));
        String inputFilePath = argsMap.get("input");
        String inputDir = argsMap.get("inputDir");
        String model = argsMap.get("model");
        String testMode = argsMap.get("testMode");

        //执行测试程序
        if(testMode != null && testMode.equals("2")){
//            testMain2();
        }
        ModelConfig mc = initModel(model);
        //先清理一下旧日志
        LogManager.cleanLogOfModel(model);

        if(inputDir == null || inputDir.isEmpty()){
            runConversations(maxRounds, mc, inputFilePath);
        }
        else{
            inputDir = TransWorker.pickSSMPCodes(inputDir);
            runConversationForDir(maxRounds, mc, inputDir);
        }
    }

}
