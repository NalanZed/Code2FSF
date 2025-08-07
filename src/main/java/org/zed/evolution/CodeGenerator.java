package org.zed.evolution;

import org.zed.Result;
import org.zed.llm.*;
import org.zed.log.LogManager;
import org.zed.trans.TransWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.zed.FSFGenerator.*;
import static org.zed.evolution.ModifyFSF.exchangeTRandomly;
import static org.zed.log.LogManager.*;

public class CodeGenerator {
    static final String YELLOW = "\u001B[33m";
    static final String RESET = "\u001B[0m";
    public ModelPrompt initCodeGenPrompt(String originalCode, List<String[]> originalFSF, List<String[]> modifiedFSF, String model){
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

    public ModelMessage codeGenConversation(ModelPrompt prompt, ModelConfig mc){
        ModelClient client = new ModelClient(mc);
        ModelMessage respMsg = null;
        try {
            ModelResponse response = client.call(prompt);
            respMsg = response.getChoices().get(0).getMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return respMsg;
    }

    public String getCodeFromMsg(ModelMessage msg){
        int topIndex = msg.getContent().indexOf("public class");
        int bottomIndex = msg.getContent().lastIndexOf("```");
        return msg.getContent().substring(topIndex, bottomIndex);
    }

    public boolean CodeGenerate(ModelPrompt prompt, ModelConfig mc, List<String[]> mFSF) throws Exception {
        int maxRounds = 3;
        int n = 0;
        int maxRoundsOf1CoupleOfTD = 10;
        List<Result> finalResultsOfEveryCoupleOfTD = new ArrayList<>();
        List<String> historyTestcases = new ArrayList<>();
        boolean regenerateFlag = false;
        while( ++n <= maxRounds){
            System.out.println("正在进行第[" + n + "/" + maxRounds + "]轮对话...");
            ModelMessage respMsg = codeGenConversation(prompt, mc);
            if(respMsg == null){
                System.err.println("没有收到有效的LLM回复,跳过本次任务！");
                return false;
            }
            prompt.getMessages().add(respMsg);
            System.out.println("得到LLM回复：" + respMsg.getContent());
            String className = getClassNameInCodeGenPrompt(prompt);
            String mCode = getCodeFromMsg(respMsg);
            String ssmp = TransWorker.trans2SSMP(mCode);
            if(ssmp == null || ssmp.isEmpty()){
                System.out.println("转换为单静态方法程序失败，无法进行后续操作");
                return false;
            }
            //开始验证环节
            String T = "";
            String D = "";
            for(String[] td : mFSF) {
                T = td[0];
                D = td[1];
                String currentTD = "T: " + T + "\n" + "D: " + D;
                System.out.println(YELLOW + "正在进行验证的TD对为：" + currentTD + RESET);
                String validationTDResult = validateATAndD(ssmp, T, D, maxRoundsOf1CoupleOfTD,
                        historyTestcases, finalResultsOfEveryCoupleOfTD);
                if(validationTDResult.equals("SUCCESS") || validationTDResult.equals("PARTIALLY SUCCESS")){
                    continue;
                }
                if(validationTDResult.equals("ERROR")){
                    System.err.println(currentTD + "\n" + "========>验证过程出现严重错误，跳过本次任务");
                    return false;
                }
                if(validationTDResult.startsWith("Exception")){
                    //TODO
                }
                regenerateFlag = true;
                ModelMessage msg = new ModelMessage("user", validationTDResult);
                prompt.addMessage(msg);
                break;
            }
            if(regenerateFlag){
                //因为某些原因，需要重新进行一轮对话生成FSF
                continue;
            }
            String verifyType = isTotallyVerified(finalResultsOfEveryCoupleOfTD) ? "totally verified!" : "partially verified!";
            System.out.println(className + "is" + verifyType);
            //记录历史测试用例
//            LogManager.saveHistoryTestcases(className,historyTestcases);
            return true;
        }
        System.err.println("CodeGen超过最大对话次数,结束");
        return false;
    }

    public static void main(String[] args) throws Exception {
        CodeGenerator gt = new CodeGenerator();
        String taskPath = "resources/dataset/evolutionDataset/Example.txt";

        ModelConfig mc = new ModelConfig();
        //准备prompt

//        ModelPrompt mp = gt.initCodeGenPrompt(mFSF,code,mc.getModelName());
        //对话，并将对话内容记录在prompt中
//        boolean b = gt.CodeGenerate(mp, mc, mFSF);
//        String className = LogManager.getClassNameInCodeGenPrompt(mp);
//        saveACodeGenMsg(mp.getMessages().get(mp.getMessages().size()-1), mp.getModel(), className);
//        saveACodeGenPrompt(mp);
    }
}
