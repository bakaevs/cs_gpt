package com.cattlescan.systemassistant.model;

public class ApiResponse {
    private String answer;

    public ApiResponse() { }

    public ApiResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
