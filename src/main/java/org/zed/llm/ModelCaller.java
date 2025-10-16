package org.zed.llm;

import java.io.IOException;

public class ModelCaller {
    public static ModelMessage make1RoundConversation(ModelPrompt prompt,ModelConfig mc){
        ModelClient client = new ModelClient(mc);
        try {
            //调用DeepSeekApi得到回复
            ModelResponse response = client.call(prompt);
            //将json格式的回复转化为对象chatResponse
            //这里指定第一个choice做记录，因为对话中只可能收到一个choice
            return response.getChoices().get(0).getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
