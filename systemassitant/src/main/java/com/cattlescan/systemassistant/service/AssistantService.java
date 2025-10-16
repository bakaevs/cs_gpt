package com.cattlescan.systemassistant.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cattlescan.systemassistant.embedding.SystemEmbedding;
import com.cattlescan.systemassistant.entity.ChatMessage;
import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.model.SearchResult;
import com.cattlescan.systemassistant.repository.ChatMessageRepository;

@Service
public class AssistantService {

    @Autowired
    private OpenAIAssistantClient assistantClient;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ChatMessageRepository chatRepo;


    /**
     * Processes a user question:
     * 1️⃣ Stores the user message in DB.
     * 2️⃣ Generates embedding and retrieves relevant context from vector store.
     * 3️⃣ Calls the assistant with context and functions.
     * 4️⃣ Stores assistant response in DB and returns it.
     * @throws IOException 
     */
    public ApiResponse processQuestion(String question, String userId) throws IOException {
        // --- Step 1: save user message ---
        ChatMessage userMessageEntity = new ChatMessage();
        userMessageEntity.setUserId(userId);
        userMessageEntity.setRole("user");
        userMessageEntity.setContent(question);
        chatRepo.save(userMessageEntity);

        // --- Step 2: generate embedding & get context ---
        double[] queryEmbedding = embeddingService.generateEmbedding(question);
        List<SearchResult> contextResults = vectorStoreService.findSimilar(queryEmbedding);
        String context = vectorStoreService.combineResults(contextResults);

        // --- Step 3: call assistant with context ---
        ApiResponse response = assistantClient.askAssistant(userId, question, context);

        // --- Step 4: save assistant response ---
        ChatMessage assistantMessageEntity = new ChatMessage();
        assistantMessageEntity.setUserId(userId);
        assistantMessageEntity.setRole("assistant");
        assistantMessageEntity.setContent(response.getAnswer());
        chatRepo.save(assistantMessageEntity);

        return response;
    }

    /**
     * Resets conversation for a user by deleting previous messages
     */
    @Transactional
    public void resetConversation(String userId) {
        chatRepo.deleteByUserId(""+userId);
    }

    /**
     * Retrieves full conversation for display
     */
    public List<ChatMessage> getConversation(String userId) {
        return chatRepo.findByUserIdOrderByTimestampAsc(""+userId);
    }

    /**
     * Retrieves context for assistant from embeddings (can be used separately)
     * @throws IOException 
     */
    public String getContextFromEmbeddings(String question) throws IOException {
        double[] queryEmbedding = embeddingService.generateEmbedding(question);
        List<SearchResult> contextResults = vectorStoreService.findSimilar(queryEmbedding);
        return vectorStoreService.combineResults(contextResults);
    }
}
