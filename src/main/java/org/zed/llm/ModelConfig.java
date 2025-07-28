package org.zed.llm;

import org.zed.log.LogManager;

import java.io.File;
import java.util.HashMap;

public class ModelConfig {
    private String modelName;
    private String url;
    private String apiKey;
    private static final String LLMS_CONFIG_DIR = "resources/config";

    public ModelConfig(String configPath){
        this.modelName = configPath.substring(configPath.lastIndexOf("/")+1, configPath.lastIndexOf("."));
        String[] s = LogManager.file2String(configPath).split("\n");
        for(String line : s){
            if(line.contains("[url]") || line.contains("[URL]")){
                this.url = line.split("=")[1].trim();
            } else if(line.contains("[apiKey]") || line.contains("[APIKEY]") || line.contains("[apikey]")){
                this.apiKey = line.split("=")[1].trim();
            }
        }
    }
    public ModelConfig(){
        this.modelName = "deepseek-chat";
        this.apiKey = "sk-bfb078d9e3ec4681b89309271bfc4265";
        this.url = "https://api.deepseek.com/chat/completions";
    }
    public static ModelConfig CreateChatGptModel(){
        String configPath = LLMS_CONFIG_DIR + "/" + "gpt-4o.txt";
        return new ModelConfig(configPath);
    }

    public static HashMap<String,ModelConfig> GetAllModels(String configDir) {
        HashMap<String,ModelConfig> modelConfigs = new HashMap<>();
        String[] configPaths = LogManager.fetchSuffixFilePathInDir(configDir,"txt");
        if(configPaths.length == 0){
            System.out.println("No model config files found in the directory: " + configDir);
            System.exit(1);
        }
        for(String configPath : configPaths){
            ModelConfig mc = new ModelConfig(configPath);
            modelConfigs.put(mc.getModelName(),mc);
        }
        return modelConfigs;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}
