package org.zed;

import com.github.javaparser.ast.body.Parameter;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.solver.Z3Solver;
import org.zed.tcg.ExecutionEnabler;
import org.zed.tcg.TestCaseAutoGenerator;
import org.zed.trans.ExecutionPathPrinter;
import org.zed.trans.TransWorker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.zed.solver.Z3Solver.callZ3Solver;
import static org.zed.tcg.ExecutionEnabler.generateMainMdUnderExpr;
import static org.zed.tcg.ExecutionEnabler.insertMainMdInSSMP;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;
import static org.zed.trans.ExecutionPathPrinter.stmtHasLoopStmt;
import static org.zed.trans.TransWorker.pickSSMPCodes;

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
            argsMap.put("maxRounds", "5");
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

    public static Result validate1Path(String ssmp, String mainMd, List<String> prePathConstrains, String T, String D) throws Exception {
        //给测试函数插桩
        String addedPrintProgram = addPrintStmt(ssmp);
        //组装可执行程序
        String runnableProgram = insertMainMdInSSMP(addedPrintProgram, mainMd);
        System.out.println("runnableProgram: " + runnableProgram);
        //拿到SpecUnit
        SpecUnit su = new SpecUnit(runnableProgram,T,D,prePathConstrains);
        Result r = Z3Solver.callZ3Solver(su);
        System.out.println("verification result: " + r);
        return r;
    }

    public static String FSFValidationTask(String ssmp,List<String[]> FSF){
        //对FSF中T的互斥性进行验证
        FSFValidationUnit fsfValidationUnit = new FSFValidationUnit(ssmp, FSF);
        //互斥性以及完备性
        Result exclusivityAndCompltenessResult = null;
        try {
            exclusivityAndCompltenessResult = callZ3Solver(fsfValidationUnit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(exclusivityAndCompltenessResult == null || exclusivityAndCompltenessResult.getStatus() == -1){
            //FSF解析失败，都没到验证完备性和互斥性那一步
            return "It was found that T in the FSF does not meet the requirements and causes a parsing failure. Please carefully review my original instructions and regenerate the FSF accordingly.";
        }
        if(exclusivityAndCompltenessResult.getStatus() == -2){
            //FSF解析失败，都没到验证完备性和互斥性那一步
            return "There exists " + exclusivityAndCompltenessResult.getCounterExample() + " in the FSF, and it is a unsatisfiable，please regenerate the FSF，avoiding contains this kind of unsatisfiable T!";
        }
        if(exclusivityAndCompltenessResult.getStatus() == 2){
            return "It was found that T in the FSF does not satisfy the mutual exclusivity requirement,especially, Ti && Tj :[" + exclusivityAndCompltenessResult.getCounterExample()+ "] is satisfiable assigned as " +
                    exclusivityAndCompltenessResult.getPathConstrain() + "，please regenerate FSF，making sure the mutual exclusivity of FSF";
        }
        if(exclusivityAndCompltenessResult.getStatus() == 3){
            return "The generated FSF lacks completeness，specifically," + "(" + exclusivityAndCompltenessResult.getCounterExample() + ")" + "is satisfiable assigned as " +
                    exclusivityAndCompltenessResult.getCounterExample() + "，please regenerate the FSF，making sure the completeness of FSF";
        }
        return "SUCCESS";
    }

    public static Result checkBadFormFSF(List<String[]> FSF, String ssmp){
        Result r = new Result();
        List<String> vars = fetchUnknownVarInFSF(FSF, ssmp);
        if(vars.isEmpty()){
            r.setStatus(0);
        }else{
            r.setStatus(1);
            StringBuilder varsList = new StringBuilder();
            for(String var : vars){
                varsList.append(var).append(",");
            }
            r.setCounterExample(varsList.toString());
        }
        return r;
    }

    public static String validateATAndD(String ssmp, String T, String D, int maxRoundsOf1CoupleOfTD, List<String> historyTestcases, List<Result> finalResultsOfTDs) throws Exception {
        int countOfPathValidated = 0;
        Result r = null;
        String currentTD = "T: " + T + "\t" + "D: " + D;
        List<String> prePathConstrains = new ArrayList<>();

        if(D.contains("Exception")){
            System.out.println("D 为 Exception");
            String vepr = validateExceptionPath(ssmp, T);
            if(vepr.contains("SUCCESS")){
                finalResultsOfTDs.add(new Result(3,"Exception路径符合预期",""));
                return "SUCCESS";
            }
            //需要提示LLM，重新生成该TD组
            return "Under T :" + T + "，" + "specifically when the variables are assigned like the main method showing: " + vepr + "No exception was thrown by the program. Think again and regenerate";
        }

        while(countOfPathValidated < maxRoundsOf1CoupleOfTD){
            //对一个TD下所有路径验证
            //生成main方法，即测试用例

            String mainMd = generateMainMdUnderExpr(T,prePathConstrains,ssmp);
            historyTestcases.add(mainMd);
            if(mainMd == null || mainMd.isEmpty() || mainMd.startsWith("ERROR")){
                System.err.println("generate testcase failed！");
                return "ERROR: generate testcase under constrains " + T  + "failed!";
            }
            r = validate1Path(ssmp,mainMd,prePathConstrains,T,D);
            if(r == null){
                return "ERROR: unexpected error occurred during validation of " + currentTD + ", please check the log for details!";
            }
            if(r.getStatus() == 0){
                prePathConstrains.add(r.getPathConstrain());
                countOfPathValidated++;
                System.out.println(currentTD + "====>" + "The path [" + countOfPathValidated + "]verified successfully!");
            }
            else break;
        }
        if(r.getStatus() == -2){
            System.out.println(currentTD + "\n" + "verification failed, there is an exception thrown by the program!");
            return "Unexpected exception thrown by the program under T: " + T + ", please regenerate the FSF according to this exception!" + r.getCounterExample();
        }
        if(r.getStatus() == -1){
            System.out.println(currentTD + "\n" + "verification failed, please check the log for details!");
            return "Some errors merged while verifying" + currentTD +", "  + r.getCounterExample() + ", please regenerate the FSF!";
        }
        if(r.getStatus() == 0 && countOfPathValidated == maxRoundsOf1CoupleOfTD){
            System.out.println("The verified paths is over " + maxRoundsOf1CoupleOfTD + ", end of validation for " + currentTD);
            finalResultsOfTDs.add(r);
            return "PARTIALLY SUCCESS";
        }
        if(r.getStatus() == 3){
            System.out.println(currentTD + "====>" + "Verification success!");
            finalResultsOfTDs.add(r);
            return "SUCCESS";
        }
        if(r.getStatus() == 1){
            System.err.println(currentTD + "====>" + "Verificator error！");
            finalResultsOfTDs.add(r);
            return "ERROR: Verificator error！";
        }
        if(r.getStatus() == 2){
            System.out.println(currentTD + "\n" + "Verification failed!");
            System.out.println("the counterexample is ：\n" + r.getCounterExample());
            HashMap<String, String> map = TestCaseAutoGenerator.analyzeModelFromZ3Solver(r.getCounterExample(), ssmp);
            if(map.isEmpty()){
                System.out.println("No counterexample or something wrong happened while parsing counterexample!");
                return "ERROR: Something wrong happened while parsing counterexample!!";
            }
            StringBuilder counterExampleMsg = new StringBuilder();
            for(Map.Entry<String, String> entry : map.entrySet()){
                counterExampleMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\t");
            }
            return "When the variables are assigned as "+counterExampleMsg+"，the output of the program violates " + currentTD + "，please regenerate the FSF according this counterexample！";
        }

        return "ERROR: unknown error occurred during validation of " + currentTD + ", please check the log for details!";
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
            System.err.println("Change the program to ssmp failed!");
            LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Change the program to ssmp failed!"
            + "\n" + "Total conversation rounds: [0]");
            return false;
        }
        List<String> historyTestcases = new ArrayList<>();
        int maxRoundsOf1CoupleOfTD = 20;
        List<String[]> FSF;
        int count = 0;
        while(count < maxRounds){

            System.out.println("\n["+ modelName +"]"+"Current conversation round is "+(++count));
            make1RoundConversation(fsfPrompt,mc);
            System.out.println("conversation round " + count + " is over!\n");
            try{
                FSF = LogManager.getLastestFSFFromLog(logPath);
                if(FSF == null || FSF.isEmpty()){
                    ModelMessage retryMsg = new ModelMessage("user", "There is no FSF in history conversation, retry the conversation, please regenerate the FSF for given program.");
                    LogManager.appendMessage(fsfPrompt.getCodePath(),retryMsg,mc.getModelName());
                    continue;
                }
            }catch (Exception e){
                System.out.println("Conversation failed to generate FSF!");
                LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Generating FSF Failed!"
                        + "\n" + "Current conversation round is: [" + count + "]");
                return false;
            }

            //对FSF中常量如 Integer.MAX_VALUE 等进行替换
            TestCaseAutoGenerator.substituteConstantValueInFSF(FSF);
            //对FSF的形式进行检查
            Result formResult = checkBadFormFSF(FSF,ssmp);
            if(formResult.getStatus() == 1){
                String msgContent = "There exist variables which do not belong to input params of the program:" + formResult.getCounterExample() + " please regenerate the FSF, avoiding these variables!";
                System.out.println(msgContent);
                ModelMessage msg = new ModelMessage("user", msgContent);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel());
                continue;
            }

            //对FSF中T的互斥性进行验证
            String FSFValidationResult = FSFValidationTask(ssmp,FSF);
            if(!FSFValidationResult.equals("SUCCESS")){
                ModelMessage msg = new ModelMessage("user", FSFValidationResult);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(fsfPrompt.getCodePath(), msg, fsfPrompt.getModel());
                continue;
            }
            String T = "";
            String D = "";
            List<Result> finalResultsOfEveryCoupleOfTD = new ArrayList<>();//记录每个TD对的验证结果
            boolean regenerateFlag = false; //标记是否需要重新生成FSF
            //对每一个TD进行验证
            for(String[] td : FSF) {
                T = td[0];
                D = td[1];
                String currentTD = "T: " + T + "\n" + "D: " + D;
                System.out.println(YELLOW + "the current T&D is：" + currentTD + RESET);
                String validationTDResult = validateATAndD(ssmp, T, D, maxRoundsOf1CoupleOfTD,
                                                                    historyTestcases, finalResultsOfEveryCoupleOfTD);
                if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
                    continue;
                }
                if(validationTDResult.equals("ERROR")){
                    String errorInfo = "Some error unhandled happened in validation stage，please check the log!";
                    System.err.println(currentTD + "\t" + errorInfo);
                    LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + errorInfo +
                            "\n" + "Current conversation round is: [" + count + "]");
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
            String verifyType = isTotallyVerified(finalResultsOfEveryCoupleOfTD) ? "totally verified!" : "Iteration_N verified!";
            System.out.println(inputFilePath + " is " + verifyType);
            LogManager.appendCode2FSFRemark(logPath,"Validation SUCCESS--" + verifyType
                    +  "\n" + "Current conversation round is: [" + count + "]");
            return true;
        }
        System.err.println("Conversation rounds number is over the maxRounds:" + maxRounds + "，task failed!");
        LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Conversation rounds number is over the maxRounds"+ "\n"
                + "Current conversation round is: [" + count + "]");
        return false;
    }

    private static String validateExceptionPath(String ssmp, String t) {
        String mainMd = generateMainMdUnderExpr(t,null,ssmp);
        if(mainMd == null || mainMd.isEmpty() || mainMd.startsWith("ERROR")){
            System.out.println("输入约束条件[" + t + "]下生成测试用例失败, 默认为异常路径");
            return "SUCCESS";
        }
        try {
            Result result = validate1Path(ssmp, mainMd, null, t, "Exception");
            if(result == null){
                System.out.println("验证过程发生错误，没有返回result");
                return "ERROR: No result returned during validation!";
            }
            if(result.getStatus() == 0){
                return "SUCCESS";
            }
            if(result.getStatus() == 1){
                return mainMd;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "FAILED";
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
        int countSucc = 0, countFail = 0, countException = 0, countTotal = 0;
        for (String filePath : filePaths) {
            System.out.println("Processing file: " + filePath + " (" + (++taskCount) + "/" + totalTaskNum + ")");
            String canNotHandleFilePath = LogManager.codePath2FailedPath(filePath);
            String handledFilePath = LogManager.codePath2SuccPath(filePath);
            countTotal++;
            if(Files.exists(Path.of(canNotHandleFilePath))){
                System.out.println("文件已存在于failedDataset目录中，跳过");
                countFail++;
                continue;
            }
            if(Files.exists(Path.of(handledFilePath))){
                System.out.println("文件已存在于succDataset目录中，跳过");
                countSucc++;
                continue;
            }
            try{
                boolean succ = runConversations(maxRounds, mc, filePath);
                if(succ) {
                    System.out.println("将成功的代码保存到 SuccDataset 目录下");
                    LogManager.copyFileToSuccDataset(filePath);
                    countSucc++;
                } else {
                    System.err.println("将失败的代码保存到 FailedDataset 目录下");
                    LogManager.copyFileToFailedDataset(filePath);
                    countFail++;
                }
            }catch (Exception e){
                System.err.println("Error during runConversations for file: " + filePath);
                System.err.println("将失败的代码保存到 resources/exceptionDataset 目录下");
                countException++;
                LogManager.copyFileToExceptionDataset(filePath);
            }
        }
        System.out.println("共处理程序 " + countTotal + " 个， " + "成功 " + countSucc + " 个 , " + "失败 " + countFail + "个" +
                "，异常 " + countException + " 个");
    }

    public static boolean FSFReview(ModelConfig mc,String inputFilePath) throws IOException {
        String modelName = mc.getModelName();
        int maxRounds = 10;
        int failOnMutualExclusivityTimes = 0;
        int failOnCompletenessTimes = 0;
        String FSFReviewLogDir = "resources/log/FSFReview";
        ModelPrompt fsfPrompt;
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
                exclusivityResult = callZ3Solver(fsfValidationUnit);
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

    //获取到 FSF 的 所有 T 中的未知变量（即，非入参变量）
    public static List<String> fetchUnknownVarInFSF(List<String[]> FSF, String ssmp){
        List<String> unKnownVars = new ArrayList<>();
        //拿到params
        List<Parameter> paramList = ExecutionEnabler.getParamsOfOneStaticMethod(ssmp);
        if(paramList == null || paramList.isEmpty()){
            return unKnownVars;
        }
        Set<String> params = new HashSet<>(paramList.size());
        for(Parameter p : paramList){
            params.add(p.getNameAsString());
        }
//        params.add("return_value");
//        params.add("Exception");
        //遍历FSF中的变量
        Set<String> varsInFSF = new HashSet<>();
        for(String[] td : FSF){
            String T = td[0];
            String D = td[1];
            if(T.contains("//")){
                T = T.substring(0,T.indexOf("//"));
            }
//            if(D.contains("//")){
//                D = D.substring(0,D.indexOf("//"));
//            }
            try{
                varsInFSF.addAll(ExecutionPathPrinter.extractVariablesInLogicalExpr(T));
//                varsInFSF.addAll(ExecutionPathPrinter.extractVariablesInLogicalExpr(D));
            }catch(Exception e){
                unKnownVars.add("Bad Form!");
                return unKnownVars;
            }
        }
        for(String var : varsInFSF){
            if(!params.contains(var)){
                unKnownVars.add(var);
            }
        }

        return unKnownVars;
    }

    public static void main(String[] args) throws Exception {
        HashMap<String,String> argsMap = FSFGenerator.handleArgs(args);
        int maxRounds = Integer.parseInt(argsMap.get("maxRounds"));
        String inputFilePath = argsMap.get("input");
        String inputDir = argsMap.get("inputDir");
        String model = argsMap.get("model");

        boolean initLogSucc = LogManager.initLogWorkDirs();
        if(!initLogSucc) return;

        ModelConfig mc = initModel(model);
        //先清理一下旧日志
        LogManager.cleanLogOfModel(model);


        if(inputDir == null || inputDir.isEmpty()){
            runConversations(maxRounds, mc, inputFilePath);
        }
        else{
            inputDir = pickSSMPCodes(inputDir);
            runConversationForDir(maxRounds, mc, inputDir);
        }
    }

    public static void  countHasLoopNumAndElseNum(String programDir) throws IOException {
        String[] filePaths = LogManager.fetchSuffixFilePathInDir(programDir, ".java");
        Path loopDir = Paths.get("resources/dataset/hasLoop");
        Path noLoopDor = Paths.get("resources/dataset/noLoop");
        int countLoop = 0,noLoop = 0;
        if(!Files.exists(loopDir)){
            Files.createDirectories(loopDir);
        }
        if(!Files.exists(noLoopDor)){
            Files.createDirectories(noLoopDor);
        }
        for(String path : filePaths){
            Path sourceFile = Paths.get(path);
            String code = LogManager.file2String(path);
            String ssmp = TransWorker.trans2SSMP(code);
            boolean hasLoop = ExecutionPathPrinter.ssmpHasLoopStmt(ssmp);
            if(hasLoop){
                Path targetFile = loopDir.resolve(sourceFile.getFileName());
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                countLoop++;
            }else{
                Path targetFile = noLoopDor.resolve(sourceFile.getFileName());
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                noLoop++;
            }
        }
        System.out.println("countLoop:" + countLoop);
        System.out.println("noLoop:" + noLoop);
    }

    public void testApp4() throws Exception {
        String resourceDir ="resources/dataset/AllCodes0721";
        ModelConfig modelConfig = new ModelConfig();
        String SSMPDir = pickSSMPCodes(resourceDir);
        runConversationForDir(1, modelConfig, SSMPDir);

    }

}
