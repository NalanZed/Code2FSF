package org.zed.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ModelClient {
    private final String url;
    private final String API_KEY;


    private final OkHttpClient client = new OkHttpClient.Builder().
            connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public ModelClient(ModelConfig mc) {
        this.url = mc.getUrl();
        this.API_KEY = mc.getApiKey();
    }
    public ModelClient(String url, String apiKey){
        this.url = url;
        this.API_KEY = apiKey;
    }

    public ModelResponse call(ModelPrompt prompt) throws IOException {
        // 转换为JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(prompt);
//        System.out.println("requestBody = " +requestBody);

        RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();
//        System.out.println("Authorization Header: Bearer " + API_KEY);
//        System.out.println("Request URL: " + url);
//        System.out.println("Request Body: " + body.toString());


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            return parseRespFromJson(response.body().string());
        }
    }

    public ModelResponse parseRespFromJson(String json) {
        return ModelResponse.fromJson(json);
    }

}
