package org.zed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.tcg.ExecutionEnabler;
import org.zed.trans.TransWorker;

import java.io.BufferedReader;
import java.io.File;
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
import static org.zed.trans.TransWorker.pickSSMPCodes;

public class FSFGenerator {

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

    public static String constructConstrain(String T,List<String> preConstrains){
        StringBuilder consExpr = new StringBuilder();
        if(T.startsWith("(")){
            consExpr.append(T);
        }else {
            consExpr.append('(').append(T).append(")");
        }
        for(String con : preConstrains){
            consExpr.append(" && ");
            if(con.startsWith("(")){
                consExpr.append(con);
            } else {
                consExpr.append('(').append(con).append(")");
            }
        }
        return consExpr.toString();
    }

    public static Result valid1Path(String pureProgram,List<String> prePathConstrains,String T,String D) throws Exception {
        //构造当前测试约束
        String conExpr = constructConstrain(T,prePathConstrains);
        System.out.println("当前测试用例生成条件为：" + conExpr);
        //生成main方法，即测试用例

        //确保是ssmp
        String ssmp = TransWorker.trans2SSMP(pureProgram);
        if(ssmp.isEmpty()){
            System.out.println("无法转换为单静态方法程序，无法检验！");
            return null;
        }

        String mainMd = generateMainMdUnderExpr(conExpr,ssmp);
        if(mainMd == null){
            System.out.println("generateMainMdUnderExpr 返回 null");
            return null;
        }
        //给测试函数插桩
        String addedPrintProgram = addPrintStmt(ssmp);
        //组装可执行程序
        String runnableProgram = insertMainMdInSSMP(addedPrintProgram, mainMd);
        System.out.println("runnableProgram: " + runnableProgram);
        //拿到SpecUnit
        SpecUnit su =new SpecUnit(runnableProgram,T,D,prePathConstrains);
        Result result = callTBFV4J(su);
        if(result != null){
            System.out.println("验证返回 result: " + result);
            return result;
        }
        return null;
    }

    public static boolean runConversations(int maxRounds, ModelConfig mc, String inputFilePath) throws Exception {
        String modelName = mc.getModelName();
        ModelPrompt fsfPrompt = ModelPrompt.generateCode2FSFPrompt(modelName,inputFilePath);
        String logPath = LogManager.codePath2LogPath(inputFilePath, modelName);
        String pureProgram = LogManager.file2String(inputFilePath);
        List<String[]> FSF;
        int count = 1;
        while(count <= maxRounds){
            System.out.println("["+ modelName +"]"+"正在进行第"+count+"轮对话");
            make1RoundConversation(fsfPrompt,mc);
            System.out.println("第"+count+"轮对话完成");
            try{
                FSF = LogManager.getLastestTDsFromLog(logPath);
            }catch (Exception e){
                System.out.println("对话生成FSF失败，跳过本次任务");
                return false;
            }
            count++;
            Result r = null;
            String T = "";
            String D = "";
            List<String> prePathConstrains = new ArrayList<>();
            //对每一个TD进行验证
            for(String[] td : FSF) {
                T = td[0];
                D = td[1];
                while(true){
                    //对一个TD下所有路径验证
                    r = valid1Path(pureProgram, prePathConstrains, T, D);
                    if (r != null && r.getStatus() == 0) {
                        prePathConstrains.add(r.getPathConstrain());
                    }else{
                        break;
                    }
                }
                if(r == null){
                    System.out.println("验证过程发生错误，没有返回result");
                    return false;
                }else if (r.getStatus() == 3) { //status 为 3 表示 路径已经全部覆盖
                    System.out.println("T：" + T + " ; " + "D: " + D + "====>" + "验证通过");
                }else{
                    break;
                }
            }
            if(r.getStatus() == 2){
                System.out.println("T：" + T + "\n" + "D: " + D + "\n" + "验证未通过，可能是FSF不准确, 需要重新生成");
                System.out.println("反例如下：\n" + r.getCounterExample());
                ModelMessage msg = new ModelMessage("user","现在经检验当变量赋值为"+r.getCounterExample()+"时，与所有TD约束都不相符，请结合这个例子重新回答！");
                fsfPrompt.addMessage(msg);
                LogManager.appendMessage(fsfPrompt.getCodePath(),msg,fsfPrompt.getModel());
                continue;
            }
            if(r.getStatus() == 3){
                System.out.println("FSF生成以及检验任务完成，生成的FSF通过检验，且符合要求");
                return true;
            }else{
                System.out.println("本次任务失败!,r.status =" + r.getStatus());
                return false;
            }
        }
        System.out.println("对话轮数超过最大值" + maxRounds + "，任务失败!");
        return false;
    }

    public static void make1RoundConversation(ModelPrompt prompt,ModelConfig mc) throws IOException {
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
            if(Files.exists(Path.of(canNotHandleFilePath))){
                System.out.println("文件已存在于failedDataset目录中，跳过");
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
        ProcessBuilder pb = new ProcessBuilder("python3", "resources/dynamic_testing.py", "--specunit",suJson);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = reader.readLine()) != null){
            if(line.startsWith("result:")){
                String resultJson = line.substring("result:".length()).trim();
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
            testMain2();
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

    public static void testMain2() throws Exception {

    }

}
