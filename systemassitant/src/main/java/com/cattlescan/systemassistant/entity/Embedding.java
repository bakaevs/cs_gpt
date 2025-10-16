package com.cattlescan.systemassistant.entity;

import javax.persistence.*;

@Entity
@Table(name = "Embedding", schema = "dbo")
public class Embedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", columnDefinition = "nvarchar(max)")
    private String text;

    @Column(name = "embedding", columnDefinition = "nvarchar(max)")
    private String embedding;

    // Constructors
    public Embedding() {}

    public Embedding(String text, String embedding) {
        this.text = text;
        this.embedding = embedding;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "Embedding{" +
                "id=" + id +
                ", text='" + (text != null ? text.substring(0, Math.min(50, text.length())) + "..." : null) + '\'' +
                '}';
    }
}
