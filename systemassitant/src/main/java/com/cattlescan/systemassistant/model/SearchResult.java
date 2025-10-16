package com.cattlescan.systemassistant.model;

public class SearchResult {
    private String text;
    private double score;

    public SearchResult(String text, double score) {
        this.text = text;
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public double getScore() {
        return score;
    }
}
