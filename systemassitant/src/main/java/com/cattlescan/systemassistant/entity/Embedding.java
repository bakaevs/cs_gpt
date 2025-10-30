package com.cattlescan.systemassistant.entity;

import java.time.LocalDateTime;

import javax.persistence.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Embedding", schema = "dbo")
public class Embedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", columnDefinition = "nvarchar(max)")
    private String text;

    @Column(name = "embedding", columnDefinition = "nvarchar(max)")
    private String embedding;

    @CreatedDate
    private LocalDateTime createdAt;    
    
    private String source;
    
    // Constructors
    public Embedding() {}

    public Embedding(String text, String embedding, String source) {
        this.text = text;
        this.embedding = embedding;
        this.source = source;
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
    

    public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
    public String toString() {
        return "Embedding{" +
                "id=" + id +
                ", text='" + (text != null ? text.substring(0, Math.min(50, text.length())) + "..." : null) + '\'' +
                '}';
    }
}
