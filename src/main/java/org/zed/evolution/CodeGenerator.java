package org.zed.evolution;

import org.zed.TBFV.TBFVResult;
import org.zed.TBFV.Z3Solver;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.tcg.TestCaseAutoGenerator;
import org.zed.trans.TransWorker;
import org.zed.verification.SpecUnit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zed.tcg.ExecutionEnabler.generateMainMdUnderExpr;
import static org.zed.tcg.ExecutionEnabler.insertMainMdInSSMP;
import static org.zed.trans.ExecutionPathPrinter.addPrintStmt;


enum MUTATION_TYPE{
    EXCHANGE_D,
    MERGE_TD,
    MODIFY_T,
    MODIFY_D,
    ADD_TD,
    DELETE_TD
}

public class CodeGenerator {
    static final String YELLOW = "\u001B[33m";
    static final String RESET = "\u001B[0m";
    private ModelPrompt initCodeGenPrompt(String originalCode, List<String[]> originalFSF, List<String[]> modifiedFSF, String model){
        ModelPrompt prompt = ModelPrompt.generateCodeGenBasicPrompt();
        if(prompt.getMessages().isEmpty()){
            System.err.println("Read few-shot prompt failed!");
            return null;
        }
        if(originalFSF.isEmpty() || modifiedFSF.isEmpty()){
            System.err.println("original FSF or modifiedFSF is empty!");
            return null;
        }
        prompt.setModel(model);
        StringBuilder sb = new StringBuilder();
        sb.append("Please modify the following code according to the modified FSF:\n");
        //原代码插入
        sb.append("```Code\n");
        sb.append(originalCode);
        sb.append("```\n");
        //原FSF插入
        sb.append("```Original FSF\n");
        for (int i = 0; i < originalFSF.size(); i++) {
            sb.append("T").append(i).append(": ").append(originalFSF.get(i)[0]);
            sb.append("\n");
            sb.append("D").append(i).append(": ").append(originalFSF.get(i)[1]);
            sb.append("\n\n");
        }
        //modified FSF插入
        sb.append("```Modified FSF\n");
        for (int i = 0; i < modifiedFSF.size(); i++) {
            sb.append("T").append(i).append(": ").append(modifiedFSF.get(i)[0]);
            sb.append("\n");
            sb.append("D").append(i).append(": ").append(modifiedFSF.get(i)[1]);
            sb.append("\n\n");
        }
        sb.append("```\n").append("\n");

        String userContent = sb.toString();
        ModelMessage message = new ModelMessage("user", userContent);
        prompt.addMessage(message);
        return prompt;
    }

    private String getCodeFromMsg(ModelMessage msg){
        int topIndex = msg.getContent().indexOf("public class");
        int bottomIndex = msg.getContent().lastIndexOf("```");
        return msg.getContent().substring(topIndex, bottomIndex);
    }

    private List<String[]> exchangeD(List<String[]> originalFSF){
        List<String[]> modifiedFSF = new ArrayList<>(originalFSF);
        int total = originalFSF.size();
        if(total < 2){
            return null;
        }
        //生成两个0-total-1的随机数
        int randomIndex1 = (int) (Math.random() * total);
        int randomIndex2 = (int) (Math.random() * total);
        while(randomIndex1 == randomIndex2){
            randomIndex2 = (int) (Math.random() * total);
        }
        String[] tmpTD1 = new String[]{originalFSF.get(randomIndex1)[0], originalFSF.get(randomIndex2)[1]};
        String[] tmpTD2 = new String[]{originalFSF.get(randomIndex2)[0], originalFSF.get(randomIndex1)[1]};
        modifiedFSF.set(randomIndex1,tmpTD1);
        modifiedFSF.set(randomIndex2, tmpTD2);
        return modifiedFSF;
    }

    //将两个随机的TD合并成一个新的TD，T 用 || 连接，公用其中一个D
    private List<String[]> merge2TD(List<String[]> originalFSF){
        List<String[]> modifiedFSF = new ArrayList<>(originalFSF);
        int total = originalFSF.size();
        if(total < 2){
            return null;
        }
        int randomIndex1 = (int) (Math.random() * total);
        int randomIndex2 = (int) (Math.random() * total);
        while(randomIndex1 == randomIndex2){
            randomIndex2 = (int) (Math.random() * total);
        }
        String combinedT = "(" + originalFSF.get(randomIndex1)[0] + ")" + " || (" + originalFSF.get(randomIndex2)[0] + ")";
        String commonD = originalFSF.get(randomIndex1)[1];
        String[] newTD = new String[]{combinedT, commonD};
        modifiedFSF.set(randomIndex1,newTD);
        modifiedFSF.remove(randomIndex2);
        return modifiedFSF;
    }

    //随机将一个分支替换为抛出异常
    private List<String[]> setRandomDAsException(List<String[]> originalFSF){
        List<String[]> modifiedFSF = new ArrayList<>(originalFSF);
        int total = originalFSF.size();
        if(total < 1){
            return null;
        }
        int randomIndex = (int) (Math.random() * total);
        String[] tmpTD = new String[]{originalFSF.get(randomIndex)[0], "Exception"};
        modifiedFSF.set(randomIndex,tmpTD);
        return modifiedFSF;
    }

    private List<String[]> mutateFSF(List<String[]> originalFSF, MUTATION_TYPE mutationType){
        List<String[]> modifiedFSF;

        if(mutationType == MUTATION_TYPE.EXCHANGE_D){
            modifiedFSF = exchangeD(originalFSF);
        }
        else if(mutationType == MUTATION_TYPE.MERGE_TD){
            modifiedFSF = merge2TD(originalFSF);
        }
        else {
            System.err.println("Unsupported mutation type: " + mutationType);
            return null;
        }
        if(modifiedFSF == null){
            System.err.println("The original FSF is too small to exchange D:");
            return null;
        }
        return modifiedFSF;
    }

    private void generateEvolutionTask(String originalCodeFSFFilePath){
        String originalCode = LogManager.getOriginalCodeFromFile(originalCodeFSFFilePath);
        if(originalCode.isEmpty()){
            System.err.println("Original code is empty, please check the file: " + originalCodeFSFFilePath);
            return;
        }
        List<String[]> originalFSF = LogManager.getOriginalFSFFromFile(originalCodeFSFFilePath);
        if(originalFSF == null || originalFSF.isEmpty()){
            System.err.println("Original FSF is empty, please check the file: " + originalCodeFSFFilePath);
            return;
        }
        MUTATION_TYPE mt = MUTATION_TYPE.MERGE_TD;

        List<String[]> modifiedFSF = mutateFSF(originalFSF,mt);

        if(modifiedFSF == null || modifiedFSF.isEmpty()){
            System.err.println("Modified FSF is empty, please check the mutation process: " + originalCodeFSFFilePath);
            return;
        }
        String taskName = originalCodeFSFFilePath.substring(originalCodeFSFFilePath.lastIndexOf("/") + 1);
        String taskPath = LogManager.EVOLUTION_DATASET_DIR + "/" + mt.name() + "/" + taskName;
        LogManager.writeACodeGenTask(originalCode, originalFSF, modifiedFSF, taskPath);
    }

    public void generateEvolutionTaskForDir(String originalCodeFSFDir) throws IOException {
        Files.list(Path.of(originalCodeFSFDir)).forEach(path -> {
            if(Files.isRegularFile(path) && path.toString().endsWith(".txt")){
                System.out.println("Generating evolution task for file: " + path);
                generateEvolutionTask(String.valueOf(path));
            }
        });
    }

    private TBFVResult validate1Path(String ssmp, String mainMd, List<String> prePathConstrains, String T, String D) throws Exception {
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

    private TDResult validateExceptionPath(String ssmp, String t) {
        String mainMd = generateMainMdUnderExpr(t,null,ssmp);
        if(mainMd == null || mainMd.isEmpty() || mainMd.startsWith("ERROR")){
            System.out.println("输入约束条件[" + t + "]下生成测试用例失败, 默认为异常路径");
            return TDResult.createSuccessResult();
        }
        try {
            TBFVResult TBFVResult = validate1Path(ssmp, mainMd, null, t, "Exception");
            if(TBFVResult == null){
                return new TDResult(TDValidationStatus.UNEXPECTED_ERROR,"Error occurred while validating，no result was returned", mainMd);
            }
            if(TBFVResult.getStatus() == 0){
                return TDResult.createSuccessResult();
            }
            if(TBFVResult.getStatus() == 1){
                return new TDResult(TDValidationStatus.ABSENCE_EXCEPTION,
                        "The D is 'Exception', but no exception thrown by the program under T", mainMd);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TDResult(TDValidationStatus.UNEXPECTED_ERROR,"Unhandled error occurred while validating 'Exception' path!", mainMd);
    }

    public TDResult validateATAndD(String ssmp, String T, String D, int maxRoundsOf1CoupleOfTD, List<String> historyTestcases) throws Exception {
        int countOfPathValidated = 0;
        TBFVResult r = null;
        String currentTD = "T: " + T + "\t" + "D: " + D;
        List<String> prePathConstrains = new ArrayList<>();

        if(D.contains("Exception")) {
            return validateExceptionPath(ssmp, T);
        }
        while(countOfPathValidated < maxRoundsOf1CoupleOfTD){
            //对一个TD下所有路径验证
            //生成main方法，即测试用例

            String testCase = generateMainMdUnderExpr(T,prePathConstrains,ssmp);
            historyTestcases.add(testCase);
            if(testCase == null || testCase.isEmpty() || testCase.startsWith("ERROR")){
                return new TDResult(TDValidationStatus.TESTCASE_GENERATION_FAILED,
                        "current TD:" + currentTD + "，generate testcase failed, please check the log for details!", "");
            }
            r = validate1Path(ssmp,testCase,prePathConstrains,T,D);
            if(r == null){
                String pathInfo = "T: " + T + "\t" + "D: " + D + "\n" + "testcase: " + testCase + "\n" + "prePathConstrains: " + prePathConstrains;
                return TDResult.createUnexpectedErrorResult("No result returned while validating path : " + pathInfo);
            }
            if(r.getStatus() == 0){
                prePathConstrains.add(r.getPathConstrain());
                countOfPathValidated++;
            }
            else break;
        }
        if(r.getStatus() == -3){
            System.out.println("Timeout error occurred during validation of " + currentTD);
            return TDResult.createTimeoutResult("Timeout error occurred during validation of " + currentTD, T, D);
        }
        if(r.getStatus() == -2){
            return TDResult.createUnexpectedExceptionResult("Unexpected exception thrown by the program under T: " + T + "\n Error info :" + r.getCounterExample());
        }
        if(r.getStatus() == -1){
            if(r.getCounterExample().contains("Unknown variable")){
                return TDResult.createUnknownVariableResult(r.getCounterExample());
            }
            return TDResult.createUnexpectedErrorResult("Unknown error occurred during validation of " + currentTD);
        }
        if(r.getStatus() == 0 && countOfPathValidated == maxRoundsOf1CoupleOfTD){
            return TDResult.createPartiallySuccessResult();
        }
        if(r.getStatus() == 3){
            return TDResult.createSuccessResult();
        }
        if(r.getStatus() == 1){
            return TDResult.createUnexpectedErrorResult("Unknown error occurred during validation of " + currentTD);
        }
        if(r.getStatus() == 2){
            HashMap<String, String> map = TestCaseAutoGenerator.analyzeModelFromZ3Solver(r.getCounterExample(), ssmp);
            if(map.isEmpty()){
                return TDResult.createUnexpectedErrorResult("No counterexample or something wrong happened while parsing counterexample!");
            }
            StringBuilder counterExampleMsg = new StringBuilder();
            for(Map.Entry<String, String> entry : map.entrySet()){
                counterExampleMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\t");
            }
            String info = "When the variables are assigned as "+ counterExampleMsg +"，the output of the program violates " + currentTD;
            return TDResult.createCounterExampleResult(info,counterExampleMsg.toString());
        }
        return TDResult.createUnexpectedErrorResult("Unknown error occurred during validation of " + currentTD);
    }

    public FSFResult validateFSF(List<String[]> FSF, String ssmp, int maxRoundsOf1CoupleOfTD, List<String> historyTestcases, List<TDResult> finalResultsOfEveryCoupleOfTD) throws Exception {
        String T = "";
        String D = "";
        int count = 0;
        TDResult r = null;
        for(String[] td : FSF) {
            T = td[0];
            D = td[1];
            String currentTD = "T: " + T + "\n" + "D: " + D;
            System.out.println(YELLOW + "the current T&D is：" + currentTD + RESET);
            r = validateATAndD(ssmp, T, D, maxRoundsOf1CoupleOfTD,
                    historyTestcases);
            if(r.getValidationStatus() == TDValidationStatus.SUCCESS || r.getValidationStatus() == TDValidationStatus.PARTIALLY_SUCCESS) {
                finalResultsOfEveryCoupleOfTD.add(r);
                continue;
            }
            if(r.getValidationStatus() == TDValidationStatus.UNEXPECTED_ERROR){
                return FSFResult.createUnexpectedErrorResult(r, T, D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.TIMEOUT_ERROR){
                return FSFResult.createOvertimeErrorResult(r, T, D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.TESTCASE_GENERATION_FAILED){
                return FSFResult.createTestCaseGenerationFailedResult(r, T, D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.UNKNOWN_VARIABLE){
                return FSFResult.createUnknownVariableResult(r, T, D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.UNEXPECTED_EXCEPTION){
                return FSFResult.createUnexpectedExceptionResult(r,T,D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.ABSENCE_EXCEPTION){
                return FSFResult.createAbsenceExceptionResult(r,T,D);
            }
            else if(r.getValidationStatus() == TDValidationStatus.COUNTER_EXAMPLE){
                return FSFResult.createCounterExampleResult(r, T, D);
            }
        }
        if(r == null){
            return FSFResult.createUnexpectedErrorResult(TDResult.createUnexpectedErrorResult("Unknown error occurred during validation"), "", "");
        }
        return FSFResult.createSuccessResult();
    }

    public EvolutionTaskStatus runEvolutionTaskForFile(String taskFilePath,ModelConfig mc,int maxRounds) throws IOException {
        String originalCode = "";
        List<String[]> originalFSF;
        List<String[]> modifiedFSF;
        List<TDResult> finalResultsOfEveryCoupleOfTD = new ArrayList<>();
        List<String> historyTestcases = new ArrayList<>();
        String logPath = LogManager.evolutionTaskPath2LogPath(taskFilePath,mc.getModelName());
        try {
            originalCode = LogManager.getOriginalCodeFromFile(taskFilePath);
            originalFSF = LogManager.getOriginalFSFFromFile(taskFilePath);
            modifiedFSF = LogManager.getModifiedFSFFromFile(taskFilePath);
        }catch (Exception e){
            System.err.println("Exception occurred while parsing code generation task file: " + taskFilePath + ", please check the file format.");
            e.printStackTrace();
            return EvolutionTaskStatus.ERROR;
        }
        if(originalFSF == null || modifiedFSF == null || originalCode.isEmpty() || originalFSF.isEmpty() || modifiedFSF.isEmpty()){
            System.err.println("Failed to parse the code generation task file: " + taskFilePath);
            return EvolutionTaskStatus.ERROR;
        }
        ModelPrompt cgPrompt = initCodeGenPrompt(originalCode, originalFSF, modifiedFSF, mc.getModelName());
        if(cgPrompt == null || cgPrompt.getMessages().isEmpty()){
            System.err.println("Failed to initialize code generation prompt, please check the few-shot prompt file.");
            return EvolutionTaskStatus.ERROR;
        }
        LogManager.appendMessage(logPath,cgPrompt.getMessages().get(cgPrompt.getMessages().size()-1));
        int currentRound = 1;

        int networkWrongTimes = 0;
        do{
            System.out.println("Conversation round " + "[" + currentRound++ + "]" + "starts! Wait for the response from LLM...");
            ModelMessage respMsg = ModelCaller.make1RoundConversation(cgPrompt, mc);
            if(respMsg == null){
                System.err.println("Did not receive a correct response from the LLM, try again.");
                if(networkWrongTimes++ > 3){
                    return EvolutionTaskStatus.NO_RESPONSE;
                }
                continue;
            }
            LogManager.appendMessage(logPath, respMsg);
            String modifiedCode = LogManager.parseModifiedCodeFormLog(logPath);
            String ssmp = TransWorker.trans2SSMP(modifiedCode);
            try {
                FSFResult fsfResult = validateFSF(modifiedFSF, ssmp, 10, historyTestcases, finalResultsOfEveryCoupleOfTD);
                if(fsfResult == null){
                    System.err.println("FSF validation failed,validate FSF return null, please check the log for details.");
                    continue;
                }
                if(fsfResult.getValidationStatus() == FSFValidationStatus.SUCCESS){
                    System.out.println("FSF validation success!");
                    return EvolutionTaskStatus.SUCCESS;
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.UNEXPECTED_ERROR){
                    System.err.println("FSF validation failed with unexpected error: " + fsfResult.getValidationInfo());
                    ModelMessage message = new ModelMessage("user", "FSF validation failed with unexpected error: " + fsfResult.getValidationInfo());
                    LogManager.appendMessage(logPath,message);
                    return EvolutionTaskStatus.ERROR;
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.OVERTIME_ERROR){
                    ModelMessage message = new ModelMessage("user", "FSF validation failed with overtime error: " + fsfResult.getValidationInfo());
                    LogManager.appendMessage(logPath,message);
                    System.err.println("FSF validation failed with overtime error: " + fsfResult.getValidationInfo());
                    return EvolutionTaskStatus.ERROR;
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.TESTCASE_GENERATION_FAILED){
                    String msgInfo = "";
                    ModelMessage message = new ModelMessage("user", msgInfo);
                    cgPrompt.addMessage(message);
                    LogManager.appendMessage(logPath,message);
                    System.err.println("FSF validation failed with testcase generation failed: " + fsfResult.getValidationInfo());
                    return EvolutionTaskStatus.FAILED;
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.UNKNOWN_VARIABLE){
                    String msgInfo = "The modified code you generated contains unknown variable under T: " + fsfResult.getTroubleT() + ", " +
                            "D: " + fsfResult.getTroubleD() + ", please regenerate the modified code to make it conforms to the given modified FSF.";
                    ModelMessage message = new ModelMessage("user", msgInfo);
                    LogManager.appendMessage(logPath,message);
                    cgPrompt.addMessage(message);
                    System.err.println("FSF validation failed with unknown variable: " + fsfResult.getValidationInfo());
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.ABSENCE_EXCEPTION){
                    String msgInfo = "The modified code you generated does not throw exception under T: " + fsfResult.getTroubleT() + ", " +
                            "D: " + fsfResult.getTroubleD() + ", please regenerate the modified code to make it conforms to the given modified FSF.";
                    ModelMessage message = new ModelMessage("user", msgInfo);
                    LogManager.appendMessage(logPath,message);
                    cgPrompt.addMessage(message);
                    System.out.println("FSF validation failed with absence exception: " + fsfResult.getValidationInfo());
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.UNEXPECTED_EXCEPTION){
                    System.err.println("FSF validation failed with unexpected exception: " + fsfResult.getValidationInfo());
                    String msgInfo = "The modified code you generated throws unexpected exception under T: " + fsfResult.getTroubleT() + ", " +
                            "D: " + fsfResult.getTroubleD() + ", please regenerate the modified code to make it conforms to the given modified FSF.";
                    ModelMessage message = new ModelMessage("user", msgInfo);
                    LogManager.appendMessage(logPath,message);
                    cgPrompt.addMessage(message);
                }
                else if(fsfResult.getValidationStatus() == FSFValidationStatus.COUNTER_EXAMPLE){
                    String msgInfo = "The modified code you generated violates the modified FSF under T: " + fsfResult.getTroubleT() + ", " +
                            "D: " + fsfResult.getTroubleD() + ", please regenerate the modified code to make it conforms to the given modified FSF.\n" +
                            "Counter example: " + fsfResult.getTestCase();
                    ModelMessage message = new ModelMessage("user", msgInfo);
                    LogManager.appendMessage(logPath,message);
                    cgPrompt.addMessage(message);
                    System.out.println("FSF validation failed with counter example: " + fsfResult.getValidationInfo());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }while(currentRound <= maxRounds);
        System.err.println("The code generation task has been run for " + maxRounds + " rounds, but no success.");
        ModelMessage msg = new ModelMessage("user", "The code generation task has been run for " + maxRounds + " rounds, but no success.");
        LogManager.appendMessage(logPath,msg);
        return EvolutionTaskStatus.FAILED;
    }

    public static void runConversationForDataset(int maxRounds,ModelConfig mc, String datasetDir,String experimentName) throws Exception {
        CodeGenerator gt = new CodeGenerator();
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
            gt.runEvolutionTask(datasetDir + "/" + category, mc, maxRounds);
            LogManager.collectExperimentRecords(category,experimentName,mc.getModelName());
            LogManager.clearCurrentExperimentTmpFiles(experimentName,category, mc.getModelName());
        }
    }
    public void runEvolutionTask(String targetFile, ModelConfig mc, int maxRounds) throws IOException {
        //检查targetFile是文件还是目录
        boolean isDirectory = Files.isDirectory(Path.of(targetFile));
        if(isDirectory){
            //如果是目录，则遍历目录下所有的txt文件
            Files.list(Path.of(targetFile)).forEach(path -> {
                String logPath = LogManager.evolutionTaskPath2LogPath(path.toString(), mc.getModelName());
                String failedPath = LogManager.filePath2SuccPath(path.toString());
                if(Files.exists(Path.of(failedPath))){
                    System.out.println("The file " + path + " has been processed before, skipping...");
                    return;
                }
                if(Files.isRegularFile(path) && path.toString().endsWith(".txt")){
                    System.out.println("Running evolution task for file: " + path);
                    try {
                        EvolutionTaskStatus status = runEvolutionTaskForFile(path.toString(), mc, maxRounds);
                        if(status == EvolutionTaskStatus.SUCCESS){
                            System.out.println("The code generation task has been successfully completed!");
                            LogManager.copyFileToSuccDataset(String.valueOf(path));
                            LogManager.appendCode2FSFRemark(logPath,"Validation SUCCESS!");
                        }else if(status == EvolutionTaskStatus.FAILED){
                            System.err.println("The code generation task has been failed!");
                            LogManager.copyFileToFailedDataset(String.valueOf(path));
                            LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Conversation rounds number is over the maxRounds");
                        }else if(status == EvolutionTaskStatus.NO_RESPONSE){
                            System.err.println("No response from the LLM, please check your network connection.");
                            LogManager.copyFileToExceptionDataset(String.valueOf(path));
                            LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Network error, no response from the LLM");
                        }else if(status == EvolutionTaskStatus.ERROR){
                            System.err.println("An error occurred while running the code generation task, please check the log for details.");
                            LogManager.copyFileToExceptionDataset(String.valueOf(path));
                            LogManager.appendCode2FSFRemark(logPath,"Validation FAIL--Unknown error!");
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to run evolution task for file: " + path + ", error: " + e.getMessage());
                        try {
                            LogManager.copyFileToExceptionDataset(String.valueOf(path));
                        } catch (IOException ex) {
                            System.err.println("Failed to copy file to exception dataset: " + path + ", error: " + ex.getMessage());
                        }
                    }
                }
            });
        }
    }

    // 调用 google-java-format.jar 格式化代码
    private static String runFormatter(String code) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "resources/google-java-format.jar", "-");
        Process proc = pb.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()))) {
            writer.write(code);
        }

        String formatted;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            formatted = sb.toString();
        }

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("google-java-format 执行失败");
        }
        return formatted;
    }

    // 从文本中提取 Original Code 和 Modified Code
    private static String[] extractCode(String experimentDir,String className,String modelName) {
        String taskFilePath = experimentDir + "/succDataset/" + className + ".txt";
        String originalCode = LogManager.getOriginalCodeFromEvoTaskFile(taskFilePath);
        String modifiedCode = LogManager.getLastModifiedCodeFromEvolutionLog(experimentDir,className,modelName);
        return new String[]{originalCode, modifiedCode};
    }

    // 计算基于行的 FDR
    private static double calcDiffRateLines(String codeBefore, String codeAfter)
            throws IOException, InterruptedException {
        String formattedBefore = runFormatter(codeBefore);
        String formattedAfter = runFormatter(codeAfter);

        List<String> beforeLines = Arrays.asList(formattedBefore.split("\\R"));
        List<String> afterLines = Arrays.asList(formattedAfter.split("\\R"));

        // 生成 diff
        List<String> diff = unifiedDiff(beforeLines, afterLines);

        long changedLines = diff.stream()
                .filter(line -> (line.startsWith("+") || line.startsWith("-"))
                        && !(line.startsWith("+++") || line.startsWith("---")))
                .count();

        int totalLines = beforeLines.size();
        return totalLines > 0 ? (double) changedLines / totalLines : 0.0;
    }

    // 简单实现 unified diff（基于 java-diff-utils 思路）
    private static List<String> unifiedDiff(List<String> before, List<String> after) {
        List<String> diff = new ArrayList<>();
        int m = before.size(), n = after.size();
        int[][] dp = new int[m + 1][n + 1];

        // LCS 动态规划
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (before.get(i).equals(after.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        // 回溯输出 diff
        int i = 0, j = 0;
        while (i < m && j < n) {
            if (before.get(i).equals(after.get(j))) {
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                diff.add("-" + before.get(i));
                i++;
            } else {
                diff.add("+" + after.get(j));
                j++;
            }
        }
        while (i < m) {
            diff.add("-" + before.get(i));
            i++;
        }
        while (j < n) {
            diff.add("+" + after.get(j));
            j++;
        }
        return diff;
    }

    // 简单的 Java Tokenizer（不完全，但能覆盖常见情况）
    private static List<String> tokenizeJava(String code) {
        Pattern pattern = Pattern.compile(
                "[A-Za-z_][A-Za-z0-9_]*"   // 标识符 / 关键字
                        + "|\\d+"                 // 数字
                        + "|==|!=|<=|>=|&&|\\|\\|" // 复合运算符
                        + "|[{}();.,+\\-*/<>=%]"   // 单字符运算符/符号
        );
        Matcher matcher = pattern.matcher(code);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    // Levenshtein 编辑距离
    private static int levenshtein(List<String> a, List<String> b) {
        int m = a.size(), n = b.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.get(i - 1).equals(b.get(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[m][n];
    }

    private static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[m][n];
    }

    // 计算 Token 级差异率
    public static double calcTokenDiffRate(String codeBefore, String codeAfter) {
        List<String> tokensBefore = tokenizeJava(codeBefore);
        List<String> tokensAfter = tokenizeJava(codeAfter);
        if (tokensBefore.isEmpty()) return 0.0;
        int dist = levenshtein(tokensBefore, tokensAfter);
        return (double) dist / tokensBefore.size();
    }

    public static void fdrTaskForFile(String experimentDir,String className,String modelName) throws IOException, InterruptedException {
        String[] codes = extractCode(experimentDir,className,modelName);
        String originalCode = codes[0];
        String modifiedCode = codes[1];
        if(originalCode.isEmpty() || modifiedCode.isEmpty()){
            System.err.println("Failed to extract code for class: " + className);
            return;
        }
        double fdrLines = calcDiffRateLines(originalCode, modifiedCode);
        int linesBefore = originalCode.split("\\R").length;
        double fdrTokens = calcTokenDiffRate(originalCode, modifiedCode);
        System.out.printf("Class: %s, FDR (Lines): %.2f%%, FDR (Tokens): %.2f%%\n",
                className, fdrLines * 100, fdrTokens * 100);

        //创建一个结果文件.txt,存放结果
        String resultFilePath = experimentDir + "/fdr_results.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFilePath, true))) {
            writer.write(String.format("Class: %s, linesBefore: %d, FDR (Lines): %.2f%%, FDR (Tokens): %.2f%%\n",
                    className, linesBefore ,fdrLines * 100, fdrTokens * 100));
        }
    }

    private static void fdrTaskForDir(String experimentDir,String modelName) throws IOException, InterruptedException {
        File[] succFiles = LogManager.fetchTxtFileInDir(experimentDir + "/succDataset");
        List<String> classNames = new ArrayList<>();
        for (File file : succFiles) {
            if(file.isFile() && file.getName().endsWith(".txt")){
                String className = file.getName().substring(0, file.getName().lastIndexOf("."));
                classNames.add(className);
            }
        }

        if(classNames.isEmpty()){
            System.err.println("No successfully processed classes found in: " + experimentDir);
            return;
        }
        String resultFilePath = experimentDir + modelName + ".txt";
        //如果结果文件已存在，删除它
        if(Files.exists(Path.of(resultFilePath))){
            Files.delete(Path.of(resultFilePath));
        }
        for (String className : classNames) {
            fdrTaskForFile(experimentDir,className,modelName);
        }
    }

    private static void runRQ4Task(){
        String[] categories = new String[]{"BorderConditionMove","EXCHANGE_D","MERGE_TD"};
        for (int i = 1; i <= 9; i++) {
            String experimentName = "resources/experiment/08110" + i + "-evolution";

            for (String category : categories) {
                System.out.println("正在处理实验 --" + experimentName + " -- " + category);
                String experimentDir = experimentName + "/" + category;
                try {
//                    fdrTaskForDir(experimentDir,"deepseek-chat");
                    derivedLinesAnalysisForDir(experimentDir,"deepseek-chat",0.1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 计算一行是否“取自”原代码
    private static boolean isDerived(String line, List<String> beforeLines, double threshold) {
        for (String before : beforeLines) {
            int dist = levenshtein(line, before);
            int maxLen = Math.max(line.length(), before.length());
            if (maxLen > 0 && (double) dist / maxLen <= threshold) {
                return true;
            }
        }
        return false;
    }

    private static void derivedLinesAnalysisForFile(String experimentDir, String className, String modelName, double threshold) throws IOException, InterruptedException {
        String[] codes = extractCode(experimentDir,className,modelName);
        String originalCode = codes[0];
        String modifiedCode = codes[1];
        if(originalCode.isEmpty() || modifiedCode.isEmpty()){
            System.err.println("Failed to extract code for class: " + className);
            return;
        }
        List<String> beforeLines = Arrays.asList(originalCode.split("\\R"));
        List<String> afterLines = Arrays.asList(modifiedCode.split("\\R"));

        long derivedCount = afterLines.stream()
                .filter(line -> isDerived(line, beforeLines, threshold))
                .count();

        System.out.printf("Class: %s, Derived Lines: %d out of %d (%.2f%%)\n",
                className, derivedCount, afterLines.size(), (double) derivedCount / afterLines.size() * 100);

        //创建一个结果文件.txt,存放结果
        String resultFilePath = experimentDir + "/derived_lines_results.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFilePath, true))) {
            writer.write(String.format("Class: %s, Derived Lines: %d out of %d (%.2f%%)\n",
                    className, derivedCount, afterLines.size(), (double) derivedCount / afterLines.size() * 100));
        }
    }

    private static void derivedLinesAnalysisForDir(String experimentDir,String modelName,double threshold) throws IOException, InterruptedException {
        File[] succFiles = LogManager.fetchTxtFileInDir(experimentDir + "/succDataset");
        List<String> classNames = new ArrayList<>();
        for (File file : succFiles) {
            if(file.isFile() && file.getName().endsWith(".txt")){
                String className = file.getName().substring(0, file.getName().lastIndexOf("."));
                classNames.add(className);
            }
        }

        if(classNames.isEmpty()){
            System.err.println("No successfully processed classes found in: " + experimentDir);
            return;
        }
        String resultFilePath = experimentDir + modelName + "_derived_lines.txt";
        //如果结果文件已存在，删除它
        if(Files.exists(Path.of(resultFilePath))){
            Files.delete(Path.of(resultFilePath));
        }
        for (String className : classNames) {
            derivedLinesAnalysisForFile(experimentDir,className,modelName,threshold);
        }
    }

    private static void statisticDeriveLines(String datasetDir){
        String prefix = "derived_lines_results-";
        String suffix = ".txt";
        HashMap<String,Double> statisticMap = new HashMap<>();
        HashMap<String,Integer> successTimesMap = new HashMap<>();
        for (int i = 1; i <=10 ; i++) {
            String fileName = prefix + i + suffix;
            String filePath = datasetDir + "/" + fileName;
            String content = LogManager.file2String(filePath);
            List<String> lines = List.of(content.split("\n"));
            for (String line : lines) {
                int startIndex = line.indexOf("Class: ") + "Class: ".length();
                int endIndex = line.indexOf(",");
                String className = line.substring(startIndex, endIndex).trim();
                startIndex = line.indexOf("(") + 1;
                endIndex = line.indexOf("%");
                Double rate = Double.valueOf(line.substring(startIndex,endIndex).trim());
                if(!statisticMap.containsKey(className)){
                    statisticMap.put(className, rate);
                    successTimesMap.put(className, 1);
                }else{
                    double oldRate = statisticMap.get(className);
                    successTimesMap.put(className, successTimesMap.get(className) + 1);
                    oldRate += rate;
                    statisticMap.put(className, oldRate);
                }
            }
        }
        //创建一个结果文件.txt,存放结果
        String resultFilePath = datasetDir + "/summary.csv";

        statisticMap.forEach((k,v)->{
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFilePath, true))) {
                writer.write(String.format("%s, %.2f%%\n",
                        k, v / successTimesMap.get(k)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }



    public static void main(String[] args) throws Exception {
//        CodeGenerator gt = new CodeGenerator();
//        ModelConfig mc = new ModelConfig();
//        String taskPath = "resources/dataset/evolutionDataset/";
//        String[] codes = extractCode("resources/experiment/081104-evolution/BorderConditionMove", "Abs_Original", "deepseek-chat");
//        System.out.println(codes[0]);
//        System.out.println(codes[1]);
//        double v = calcDiffRateLines(codes[0], codes[1]);
//        double v2 = calcTokenDiffRate(codes[0], codes[1]);
//        System.out.println(v);
//        System.out.println(v2);

//        fdrTaskForFile("resources/experiment/081104-evolution/BorderConditionMove","Abs_Original","deepseek-chat");
//        fdrTaskForDir("resources/experiment/081105-evolution/EXCHANGE_D","deepseek-chat");
//            runRQ4Task();
        statisticDeriveLines("resources/dataset/0907-RQ4/SHIFTING_B");
        statisticDeriveLines("resources/dataset/0907-RQ4/EXCHANGE_D");
        statisticDeriveLines("resources/dataset/0907-RQ4/MERGE_TD");
    }
}
