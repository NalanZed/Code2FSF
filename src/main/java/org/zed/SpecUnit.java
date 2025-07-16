package org.zed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.zed.log.LogManager;

import java.util.List;

@Data
public class SpecUnit {
    private String program = "";
    // TDs存储 T&D组
    @JsonProperty("T")
    private String T;
    @JsonProperty("D")
    private String D;
    @JsonProperty("pre_constrains")
    private List<String> preConstrains;

    public SpecUnit(String program, String T,String D,List<String> preConstrains){
        this.program = program;
        this.T = T;
        this.D = D;
        this.preConstrains = preConstrains;
    }
    public List<String> getPreconditions() {
        return preConstrains;
    }

    public SpecUnit(String codePath){
        this.program = LogManager.file2String(codePath);
    }

    public SpecUnit(){
    }
}

