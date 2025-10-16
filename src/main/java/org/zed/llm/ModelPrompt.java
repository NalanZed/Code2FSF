package org.zed.llm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.zed.log.LogManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModelPrompt {
    @Getter
    @Setter
    private String model;
    @Getter
    private List<ModelMessage> messages = new ArrayList<>();

    @Getter
    @JsonIgnore
    //一次连续的对话请求，始终对应同一个测试程序，记录此程序的位置信息。
    private String codePath = "";
    @JsonIgnore
//    private static final String CODE2FSF_FEW_SHOT_PATH = "resources/fewShot/prompt.txt";
    private static final String CODE2FSF_FEW_SHOT_PATH = "resources/fewShot/prompt2.1.txt";
    private static final String CODEGEN_FEW_SHOT_PATH = "resources/fewShot/codeGenPrompt.txt";

    public ModelPrompt(){

    }

    public static ModelPrompt initCode2FSFPrompt(String model, String codeFilePath) throws IOException {
        ModelPrompt mp = new ModelPrompt();
        mp.model = model;

        // 读取 codeFilePath 指定的文件内容到 programCode
        String programCode = LogManager.file2String(codeFilePath);

        if(programCode.isEmpty()) {
            throw new IOException("程序代码为空或未找到文件: " + codeFilePath);
        }

        List<ModelMessage> preMessages = mp.assembleMessages(CODE2FSF_FEW_SHOT_PATH);
        mp.messages.addAll(preMessages);

        String userContent = "Please generate an FSF for the program below:\n" + "```" + "\n" + programCode + "\n" + "```";
        ModelMessage message = new ModelMessage("user", userContent);

        mp.addMessage(message);

        //记录日志
        mp.codePath = codeFilePath;
        if(Files.exists(Path.of(LogManager.codePath2LogPath(codeFilePath,model)))) {
            Files.delete(Path.of(LogManager.codePath2LogPath(codeFilePath,model)));
        }
        return mp;
    }

    public static ModelPrompt generateCodeGenBasicPrompt(){
        ModelPrompt mp = new ModelPrompt();
        List<ModelMessage> preMessages = mp.assembleMessages(CODEGEN_FEW_SHOT_PATH);
        mp.messages.addAll(preMessages);
        return mp;
    }

    //通过model和filepath构造DeepSeekRequest
    public ModelPrompt(String model, String codeFilePath) throws IOException {
        this.model = model;

        // 读取 codeFilePath 指定的文件内容到 programCode
        String programCode = LogManager.file2String(codeFilePath);

        if(programCode.isEmpty()) {
            throw new IOException("程序代码为空或未找到文件: " + codeFilePath);
        }

        List<ModelMessage> preMessages = assembleMessages(CODE2FSF_FEW_SHOT_PATH);
        this.messages.addAll(preMessages);

        String userContent = "为下面的代码生成FSF:\n" + "```" + "\n" + programCode + "\n" + "```";
        ModelMessage message = new ModelMessage("user", userContent);

        this.addMessage(message);

        //记录日志
        this.codePath = codeFilePath;
        String logPath = LogManager.codePath2LogPath(codeFilePath, model);
        LogManager.appendMessage(logPath,message);

    }

    //测试用构造函数
    public ModelPrompt(String model, ModelMessage message) {
        this.model = model;
        this.messages.add(message);
    }

    public List<ModelMessage> assembleMessages(String filePath) {
        List<ModelMessage> preMessages = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                //读取system部分内容，封装成一个DeepSeekRequestMessage
                if(line.contains("start role system")) {
                    StringBuilder sb = new StringBuilder();
                    line = br.readLine();
                    while(line != null && !line.contains("*end* role system")) {
                        sb.append(line).append("\n");
                        line = br.readLine();
                    }
                    ModelMessage systemRoleMessage = new ModelMessage("system",sb.toString());
                    preMessages.add(systemRoleMessage);
                }
                //读取user部分内容，封装成一个DeepSeekRequestMessage
                else if(line.contains("start role assistant")) {
                    StringBuilder sb = new StringBuilder();
                    line = br.readLine();
                    while(line != null && !line.contains("*end* role assistant")) {
                        sb.append(line).append("\n");
                        line = br.readLine();
                    }
                    ModelMessage assistantRoleMessage = new ModelMessage("assistant",sb.toString());
                    preMessages.add(assistantRoleMessage);
                }
                else if(line.contains("start role user")){
                    StringBuilder sb = new StringBuilder();
                    line = br.readLine();
                    while(line != null && !line.contains("*end* role user")) {
                        sb.append(line).append("\n");
                        line = br.readLine();
                    }
                    ModelMessage usermRoleMessage = new ModelMessage("user",sb.toString());
                    preMessages.add(usermRoleMessage);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return preMessages;
    }

    public void addMessageWithContent(String content) {
        ModelMessage message = new ModelMessage("user", content);
        this.messages.add(message);
    }

    public void addMessage(ModelMessage message) {
        this.getMessages().add(message);
    }

}
