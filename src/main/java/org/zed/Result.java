package org.zed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Result {
    private int status;
    @JsonProperty("counter_example")
    private String counterExample;
    @JsonProperty("path_constrain")
    private String pathConstrain;

    public Result(String json) {
        //将json转化为SpecUnit对象
        ObjectMapper mapper = new ObjectMapper();
        try {
            Result res = mapper.readValue(json, Result.class);
            this.status = res.getStatus();
            this.counterExample = res.getCounterExample();
            this.pathConstrain = res.getPathConstrain();
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }
}