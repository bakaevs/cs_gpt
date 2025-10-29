package com.cattlescan.systemassistant.model;

public class ApiResponse {

    /** The assistant’s natural language response */
    private String answer;

    /** Conversation thread ID (for chat history grouping) */
    private Long threadId;

    /** Optional raw model response (debugging / logging) */
    private String raw;

    /** Optional error or system message */
    private String systemMessage;

    /** Optional alert object (if assistant calls a tool that generates an alert) */
    private Object payload;

    public ApiResponse() {}

    public ApiResponse(String answer) {
        this.answer = answer;
    }

    public ApiResponse(String answer, Long threadId) {
        this.answer = answer;
        this.threadId = threadId;
    }

    // --- getters & setters ---

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
