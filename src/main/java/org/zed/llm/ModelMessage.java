package org.zed.llm;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ModelMessage {
    private String role;
    private String content;
    @JsonIgnore
    private String refusal;
    @JsonIgnore
    private String[] annotations;
//    @JsonProperty("reasoning_content")
//    private String reasoningContent;

    // 无参构造方法，供 Jackson 反序列化使用
    public ModelMessage() {
    }

    public ModelMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    public String getRefusal() {
        return refusal;
    }

    public void setRefusal(String refusal) {
        this.refusal = refusal;
    }

//    public String getReasoningContent() {
//        return reasoningContent;
//    }
//
//    public void setReasoningContent(String reasoningContent) {
//        this.reasoningContent = reasoningContent;
//    }
}
