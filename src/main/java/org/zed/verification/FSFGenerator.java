package org.zed.verification;

import com.github.javaparser.ast.body.Parameter;
import org.zed.TBFV.TBFVResult;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.TBFV.Z3Solver;
import org.zed.tcg.ExecutionEnabler;
import org.zed.tcg.TestCaseAutoGenerator;
import org.zed.trans.ExecutionPathPrinter;
import org.zed.trans.TransWorker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.zed.TBFV.Z3Solver.callZ3Solver;
import static org.zed.tcg.ExecutionEnabler.generateMainMdUnderExpr;
import static org.zed.tcg.ExecutionEnabler.insertMainMdInSSMP;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;
import static org.zed.trans.TransWorker.pickSSMPCodes;

public class FSFGenerator {

    static final String YELLOW = "\u001B[33m";
    static final String RESET = "\u001B[0m";
    private static final String LLMS_CONFIG_DIR = "resources/config";

    //定一个枚举类型内部类
    public enum TBFVValidationResultType {
        SUCCESS,
        REGENERATE,
        UNEXPECTED_ERROR,
        OVERTIME_ERROR
    }

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
        if(!argsMap.containsKey("maxRounds")){
            argsMap.put("maxRounds", "1");
        }
    }

    //处理输入参数，写入HashMap方便读取
    public static HashMap<String,String> handleArgs(String[] args){
        HashMap<String,String> argsMap = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if ("--input".equals(args[i]) || "-i".equals(args[i])) {
                argsMap.put("input", args[i + 1]);
            }
            else if("--model".equals(args[i]) || "-m".equals(args[i])){
                argsMap.put("model", args[i + 1]);
            }
            else if("--maxRounds".equals(args[i]) || "-r".equals(args[i])){
                argsMap.put("maxRounds", args[i + 1]);
            }
            else if("--inputDir".equals(args[i]) || "-d".equals(args[i])){
                argsMap.put("inputDir", args[i + 1]);
            }else if("--experimentName".equals(args[i]) || "-en".equals(args[i])) {
                argsMap.put("experimentName", args[i + 1]);
            }
        }
        //检查输入参数，设置默认值
        checkArgsAndSetDefault(argsMap);
        return argsMap;
    }

    public static TBFVResult validate1Path(String ssmp, String mainMd, List<String> prePathConstrains, String T, String D) throws Exception {
        //给测试函数插桩
        String addedPrintProgram = addPrintStmt(ssmp);
        //组装可执行程序
        String runnableProgram = insertMainMdInSSMP(addedPrintProgram, mainMd);
        System.out.println("runnableProgram: " + runnableProgram);
        //拿到SpecUnit
        SpecUnit su = new SpecUnit(runnableProgram,T,D,prePathConstrains);
        TBFVResult r = Z3Solver.callZ3Solver(su);
        System.out.println("verification result: " + r);
        return r;
    }

    public static String FSFValidationTask(String ssmp,List<String[]> FSF){
        //对FSF中T的互斥性进行验证
        FSFValidationUnit fsfValidationUnit = new FSFValidationUnit(ssmp, FSF);
        //互斥性以及完备性
        TBFVResult exclusivityAndCompltenessTBFVResult = null;
        try {
            exclusivityAndCompltenessTBFVResult = callZ3Solver(fsfValidationUnit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(exclusivityAndCompltenessTBFVResult == null || exclusivityAndCompltenessTBFVResult.getStatus() == -1){
            //FSF解析失败，都没到验证完备性和互斥性那一步
            return "It was found that T in the FSF does not meet the requirements and causes a parsing failure. Please carefully review my original instructions and regenerate the FSF accordingly.";
        }
        if(exclusivityAndCompltenessTBFVResult.getStatus() == -2){
            //FSF解析失败，都没到验证完备性和互斥性那一步
            return "There exists " + exclusivityAndCompltenessTBFVResult.getCounterExample() + " in the FSF, and it is a unsatisfiable，please regenerate the FSF，avoiding contains this kind of unsatisfiable T!";
        }
        if(exclusivityAndCompltenessTBFVResult.getStatus() == 2){
            return "It was found that T in the FSF does not satisfy the mutual exclusivity requirement,especially, Ti && Tj :[" + exclusivityAndCompltenessTBFVResult.getCounterExample()+ "] is satisfiable assigned as " +
                    exclusivityAndCompltenessTBFVResult.getPathConstrain() + "，please regenerate FSF，making sure the mutual exclusivity of FSF";
        }
        if(exclusivityAndCompltenessTBFVResult.getStatus() == 3){
            return "The generated FSF lacks completeness，specifically," + "(" + exclusivityAndCompltenessTBFVResult.getCounterExample() + ")" + "is satisfiable assigned as " +
                    exclusivityAndCompltenessTBFVResult.getCounterExample() + "，please regenerate the FSF，making sure the completeness of FSF";
        }
        return "SUCCESS";
    }

    public static TBFVResult checkBadFormFSF(List<String[]> FSF, String ssmp){
        TBFVResult r = new TBFVResult();
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

    public static String validateATAndD(String ssmp, String T, String D, int maxRoundsOf1CoupleOfTD, List<String> historyTestcases, List<TBFVResult> finalResultsOfTDS) throws Exception {
        int countOfPathValidated = 0;
        TBFVResult r = null;
        String currentTD = "T: " + T + "\t" + "D: " + D;
        List<String> prePathConstrains = new ArrayList<>();

        if(D.contains("Exception")){
            System.out.println("D 为 Exception");
            String vepr = validateExceptionPath(ssmp, T);
            if(vepr.contains("SUCCESS")){
                finalResultsOfTDS.add(new TBFVResult(3,"Exception路径符合预期",""));
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
        if(r.getStatus() == -3){
            System.out.println("Timeout error occurred during validation of " + currentTD);
            return "OVERTIME ERROR!";
        }
        if(r.getStatus() == -2){
            System.out.println(currentTD + "\n" + "verification failed, there is an exception thrown by the program!");
            return "Unexpected exception thrown by the program under T: " + T + ", please regenerate the FSF according to this exception!" + r.getCounterExample();
        }
        if(r.getStatus() == -1){
            System.out.println(currentTD + "\n" + "verification failed, please check the log for details!");
            return "Some errors occurred while verifying" + currentTD +", "  + r.getCounterExample() + ", please regenerate the FSF!";
        }
        if(r.getStatus() == 0 && countOfPathValidated == maxRoundsOf1CoupleOfTD){
            System.out.println("The verified paths is over " + maxRoundsOf1CoupleOfTD + ", end of validation for " + currentTD);
            finalResultsOfTDS.add(r);
            return "PARTIALLY SUCCESS";
        }
        if(r.getStatus() == 3){
            System.out.println(currentTD + "====>" + "Verification success!");
            finalResultsOfTDS.add(r);
            return "SUCCESS";
        }
        if(r.getStatus() == 1){
            System.err.println(currentTD + "====>" + "Verificator error！");
            finalResultsOfTDS.add(r);
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

    public static TBFVValidationResultType validateFSF(List<String[]> FSF,String ssmp,ModelPrompt prompt,String logPath,int maxRoundsOf1CoupleOfTD,List<String> historyTestcases,List<TBFVResult> finalResultsOfEveryCoupleOfTD) throws Exception {
        String T = "";
        String D = "";
        int count = 0;
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
            if(validationTDResult.equals("OVERTIME ERROR!")){
                System.err.println(currentTD + "\n" + "======>overtime error, please check the log for details!");
                LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + validationTDResult +
                        "\n" + "Current conversation round is: [" + count + "]");
                return TBFVValidationResultType.OVERTIME_ERROR;
            }
            if(validationTDResult.equals("ERROR")){
                String errorInfo = "Some error unhandled happened in validation stage，please check the log!";
                System.err.println(currentTD + "\t" + errorInfo);
                LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + errorInfo +
                        "\n" + "Current conversation round is: [" + count + "]");
                return TBFVValidationResultType.UNEXPECTED_ERROR;
            }
            ModelMessage msg = new ModelMessage("user", validationTDResult);
            prompt.addMessage(msg);
            LogManager.appendMessage(logPath,msg);
            return TBFVValidationResultType.REGENERATE;
        }
        return TBFVValidationResultType.SUCCESS;
    }

    public static boolean isTotallyVerified(List<TBFVResult> finalResultsOfEveryCoupleOfTD){
        //对整个验证任务的评价，不应该局限在最后一个验证任务结果上
        //正确的方法是，要记录所有TD组验证的结果,逐个遍历
        boolean totallyVerified = true;
        for(TBFVResult res : finalResultsOfEveryCoupleOfTD){
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
        ModelPrompt fsfPrompt = ModelPrompt.initCode2FSFPrompt(modelName,inputFilePath);
        String logPath = LogManager.codePath2LogPath(inputFilePath, modelName);
        LogManager.appendMessage(logPath,fsfPrompt.getMessages().get(fsfPrompt.getMessages().size() - 1));
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
            ModelMessage respMsg = ModelCaller.make1RoundConversation(fsfPrompt, mc);
            if(respMsg == null){
                ModelMessage retryMsg = new ModelMessage("user", "Did not get correct response from llm, try again!");
                LogManager.appendMessage(logPath,retryMsg);
                continue;
            }
            //将回复msg写入deepseekRequest
            fsfPrompt.getMessages().add(respMsg);
            //将回复写入日志
            LogManager.appendMessage(logPath,respMsg);
            System.out.println("conversation round " + count + " is over!\n");
            try{
                FSF = LogManager.getLastestFSFFromLog(logPath);
                if(FSF == null || FSF.isEmpty()){
                    ModelMessage retryMsg = new ModelMessage("user", "There is no FSF in history conversation, retry the conversation, please regenerate the FSF for given program.");
                    LogManager.appendMessage(logPath,retryMsg);
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
            TBFVResult formTBFVResult = checkBadFormFSF(FSF,ssmp);
            if(formTBFVResult.getStatus() == 1){
                String msgContent = "There exist variables which do not belong to input params of the program:" + formTBFVResult.getCounterExample() + " please regenerate the FSF, avoiding these variables!";
                System.out.println(msgContent);
                ModelMessage msg = new ModelMessage("user", msgContent);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(logPath, msg);
                continue;
            }

            //对FSF中T的互斥性进行验证
            String FSFValidationResult = FSFValidationTask(ssmp,FSF);
            if(!FSFValidationResult.equals("SUCCESS")){
                ModelMessage msg = new ModelMessage("user", FSFValidationResult);
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(logPath, msg);
                continue;
            }
            String T = "";
            String D = "";
            List<TBFVResult> finalResultsOfEveryCoupleOfTD = new ArrayList<>();//记录每个TD对的验证结果
//            boolean regenerateFlag = false; //标记是否需要重新生成FSF
//            //对每一个TD进行验证
//            for(String[] td : FSF) {
//                T = td[0];
//                D = td[1];
//                String currentTD = "T: " + T + "\n" + "D: " + D;
//                System.out.println(YELLOW + "the current T&D is：" + currentTD + RESET);
//                String validationTDResult = validateATAndD(ssmp, T, D, maxRoundsOf1CoupleOfTD,
//                                                                    historyTestcases, finalResultsOfEveryCoupleOfTD);
//                if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
//                    continue;
//                }
//                if(validationTDResult.equals("OVERTIME ERROR!")){
//                    System.err.println(currentTD + "\n" + "======>overtime error, please check the log for details!");
//                    LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + validationTDResult +
//                            "\n" + "Current conversation round is: [" + count + "]");
//                    return false;
//                }
//                if(validationTDResult.equals("ERROR")){
//                    String errorInfo = "Some error unhandled happened in validation stage，please check the log!";
//                    System.err.println(currentTD + "\t" + errorInfo);
//                    LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--" + errorInfo +
//                            "\n" + "Current conversation round is: [" + count + "]");
//                    return false;
//                }
//                regenerateFlag = true;
//                ModelMessage msg = new ModelMessage("user", validationTDResult);
//                fsfPrompt.addMessage(msg);
//                LogManager.appendMessage(logPath,msg);
//                break;
//            }
//            if(regenerateFlag){
//                //因为某些原因，需要重新进行一轮对话生成FSF
//                continue;
//            }
//            String verifyType = isTotallyVerified(finalResultsOfEveryCoupleOfTD) ? "totally verified!" : "Iteration_N verified!";
//            System.out.println(inputFilePath + " is " + verifyType);
//            LogManager.appendCode2FSFRemark(logPath,"Validation SUCCESS--" + verifyType
//                    +  "\n" + "Current conversation round is: [" + count + "]");
//            return true;
            TBFVValidationResultType tbfvValidationResultType = validateFSF(FSF, ssmp, fsfPrompt, logPath, maxRoundsOf1CoupleOfTD, historyTestcases, finalResultsOfEveryCoupleOfTD);
            if(tbfvValidationResultType == TBFVValidationResultType.REGENERATE){
                //需要重新生成FSF
                continue;
            }
            if(tbfvValidationResultType == TBFVValidationResultType.OVERTIME_ERROR){
                return false;
            }
            if(tbfvValidationResultType == TBFVValidationResultType.SUCCESS){
                String verifyType = isTotallyVerified(finalResultsOfEveryCoupleOfTD) ? "totally verified!" : "partially verified!";
                System.out.println(inputFilePath + " is " + verifyType);
                LogManager.appendCode2FSFRemark(logPath,"Validation SUCCESS--" + verifyType
                        +  "\n" + "Current conversation round is: [" + count + "]");
                return true;
            }
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
            TBFVResult TBFVResult = validate1Path(ssmp, mainMd, null, t, "Exception");
            if(TBFVResult == null){
                System.out.println("验证过程发生错误，没有返回result");
                return "ERROR: No result returned during validation!";
            }
            if(TBFVResult.getStatus() == 0){
                return "SUCCESS";
            }
            if(TBFVResult.getStatus() == 1){
                return mainMd;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "FAILED";
    }

    public static void runConversationForDir(int maxRounds, ModelConfig mc, String inputDir) throws Exception {
        // 遍历输入目录下的所有文件
        int taskCount = 0;
        String[] filePaths = LogManager.fetchSuffixFilePathInDir(inputDir,".java");
        int totalTaskNum = filePaths.length;
        int countSucc = 0, countFail = 0, countException = 0, countTotal = 0;
        for (String filePath : filePaths) {
            System.out.println("Processing file: " + filePath + " (" + (++taskCount) + "/" + totalTaskNum + ")");
            String canNotHandleFilePath = LogManager.filePath2FailedPath(filePath);
            String handledFilePath = LogManager.filePath2SuccPath(filePath);
            countTotal++;
            if(Files.exists(Path.of(canNotHandleFilePath))){
                System.out.println("文件已存在于failedDataset目录中，跳过");
                countFail++;
                continue;
            }
            if(Files.exists(Path.of(handledFilePath))){
                System.out.println("This file already exists in dir succDataset, skip it.");
                countSucc++;
                continue;
            }
            try{
                boolean succ = runConversations(maxRounds, mc, filePath);
                if(succ) {
                    System.out.println("Generation success! Copy the code to succDataset directory");
                    LogManager.copyFileToSuccDataset(filePath);
                    countSucc++;
                } else {
                    System.err.println("Generation failed! Copy the code to failedDataset directory");
                    LogManager.copyFileToFailedDataset(filePath);
                    countFail++;
                }
            }catch (Exception e){
                System.err.println("Error during runConversations for file: " + filePath);
                System.err.println("Unexpected error ocurred during generation or verification, copy the code to exceptionDataset directory");
                countException++;
                LogManager.copyFileToExceptionDataset(filePath);
            }
        }
        System.out.println("Total num: " + countTotal + " , " + "Success num: " + countSucc + " , " + "Failed num: " + countFail + " , " +
                "Exception num: " + countException);
    }

    public static void runConversationForDataset(int maxRounds,ModelConfig mc, String datasetDir,String experimentName) throws Exception {
        Set<String> categories = LogManager.getCategoriesInDatasetDir(datasetDir);
        //如果没有categories,那当前目录名作为一个类别,处理单独一个类别实验时起作用
        if(categories.isEmpty()){
            categories.add(datasetDir.substring(datasetDir.lastIndexOf("/") + 1));
            datasetDir = datasetDir.substring(0, datasetDir.lastIndexOf("/"));
        }
        for (String category : categories) {
            System.out.println("Start experiment for category: " + category);
            String experimentDir = LogManager.getExperimentLogPath(experimentName,category);
            if(Files.exists(Path.of(experimentDir))) {
                System.out.println("Experiment directory already exists, maybe this task has been done, skip it: " + experimentName + "-" + category);
                continue;
            }
            String SSMPDir = pickSSMPCodes(datasetDir + "/" + category);
            runConversationForDir(maxRounds, mc, SSMPDir);
            LogManager.collectExperimentRecords(category,experimentName,mc.getModelName());
            LogManager.clearCurrentExperimentTmpFiles(experimentName,category, mc.getModelName());
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
        String experimentName = argsMap.get("experimentName");

        boolean initLogSucc = LogManager.initLogWorkDirs();
        if(!initLogSucc) return;

        ModelConfig mc = initModel(model);
        //先清理一下旧日志
        LogManager.cleanLogOfModel(model);

        if(inputDir == null || inputDir.isEmpty()){
            runConversations(maxRounds, mc, inputFilePath);
        }
        else{
            if(experimentName == null || experimentName.isEmpty()){
                System.out.println("Experiment name is not provided, please provide a name for the experiment.");
                return;
            }
            runConversationForDataset(maxRounds, mc, inputDir,experimentName);
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
