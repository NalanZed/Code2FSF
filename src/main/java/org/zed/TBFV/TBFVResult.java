package org.zed.TBFV;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TBFVResult {
    private int status;
    @JsonProperty("counter_example")
    private String counterExample;
    @JsonProperty("path_constrain")
    private String pathConstrain;

    public TBFVResult(String json) {
        //将json转化为SpecUnit对象
        ObjectMapper mapper = new ObjectMapper();
        try {
            TBFVResult res = mapper.readValue(json, TBFVResult.class);
            this.status = res.getStatus();
            this.counterExample = res.getCounterExample();
            this.pathConstrain = res.getPathConstrain();
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }
    public TBFVResult(int status, String counterExample, String pathConstrain) {
        this.status = status;
        this.counterExample = counterExample;
        this.pathConstrain = pathConstrain;
    }
}