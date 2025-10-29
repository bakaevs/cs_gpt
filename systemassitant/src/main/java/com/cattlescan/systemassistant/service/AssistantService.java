package com.cattlescan.systemassistant.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cattlescan.systemassistant.entity.ChatMessage;
import com.cattlescan.systemassistant.entity.ChatThread;
import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.model.SearchResult;
import com.cattlescan.systemassistant.repository.ChatMessageRepository;
import com.cattlescan.systemassistant.repository.ChatThreadRepository;

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
    
    @Autowired
    private ChatThreadRepository chatThreadRepo;


    public ApiResponse processQuestion(String question, String userId, Long threadId) throws IOException {

        // ✅ 1. Auto-create new thread if needed
        if (threadId == null) {
            String autoName = "Chat — " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            threadId = createThread(userId, autoName);
        }

        // ✅ 2. Save user message
        saveUserMessage(userId, threadId, question);

        // ✅ 3. Generate embedding
        double[] queryEmbedding = embeddingService.generateEmbedding(question);
        List<SearchResult> contextResults = vectorStoreService.findSimilar(queryEmbedding);
        String context = vectorStoreService.combineResults(contextResults);

        // ✅ 4. Ask assistant
        ApiResponse response = assistantClient.askAssistant(userId, threadId, question, context);

        // ✅ 5. Save assistant message
        saveAssistantMessage(userId, threadId, response.getAnswer());


        return response;
    }


    private void saveUserMessage(String userId, Long threadId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setThreadId(threadId);
        msg.setRole("user");
        msg.setContent(content);

        chatRepo.save(msg);
    }
    
    private void saveAssistantMessage(String userId, Long threadId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setThreadId(threadId);
        msg.setRole("assistant");
        msg.setContent(content);

        chatRepo.save(msg);
    }
    

    /**
     * Resets conversation for a user by deleting previous messages
     */
    @Transactional
    public void resetConversation(String userId) {
        chatRepo.deleteByUserId(""+userId);
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
    
    @Transactional
    public Long createThread(String userId, String name) {
        ChatThread t = new ChatThread();
        t.setUserId(userId);
        t.setName(name);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        chatThreadRepo.save(t);
        return t.getId();
    }

    
    public List<ChatMessage> getThread(Long threadId) {
        return chatRepo.findByThreadIdOrderByCreatedAtAsc(threadId);
    }
    
    public List<Long> getUserThreads(String userId) {
        return chatRepo.findThreadIdsForUser(userId);
    }
    
    @Transactional
    public void deleteThread(Long threadId) {
        chatRepo.deleteByThreadId(threadId);
    }
	    
	    /* ---------------------------------------------------------
	    ✅ Return all threads for a user
	    --------------------------------------------------------- */
    public List<Map<String, Object>> getThreads(String userId) {

        List<ChatThread> threads = chatThreadRepo.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> list = new ArrayList<>();

        for (ChatThread t : threads) {
            Map<String, Object> map = new HashMap<>();
            map.put("threadId", t.getId());
            map.put("threadName", t.getName());
            map.put("updatedAt", t.getUpdatedAt());
            map.put("createdAt", t.getCreatedAt());
            list.add(map);
        }

        return list;
    }

	
	 
	 
	 
	 /* ---------------------------------------------------------
	    ✅ Return all messages in a specific thread
	    --------------------------------------------------------- */
	 public List<ChatMessage> getMessagesForThread(Long threadId) {
	     return chatRepo.findByThreadIdOrderByCreatedAtAsc(threadId);
	 }
	
	 /* ---------------------------------------------------------
	    ✅ Rename a thread
	    --------------------------------------------------------- */
	 @Transactional
	 public void renameThread(Long threadId, String newName) {
	     chatThreadRepo.renameThread(threadId, newName);
	 }

	
	 /* ---------------------------------------------------------
	    ✅ Process message inside specific thread
	    --------------------------------------------------------- */
	 public ApiResponse processThreadQuestion(Long threadId, String question, String userId) throws IOException {
	
	     ChatMessage userMsg = new ChatMessage();
	     userMsg.setUserId(userId);
	     userMsg.setThreadId(threadId);
	     userMsg.setRole("user");
	     userMsg.setContent(question);
	     userMsg.setCreatedAt(LocalDateTime.now());
	     chatRepo.save(userMsg);
	
	     double[] embedding = embeddingService.generateEmbedding(question);
	     List<SearchResult> ctx = vectorStoreService.findSimilar(embedding);
	     String context = vectorStoreService.combineResults(ctx);
	
	     ApiResponse ai = assistantClient.askAssistant(userId, threadId, question, context);
	
	     ChatMessage aMsg = new ChatMessage();
	     aMsg.setUserId(userId);
	     aMsg.setThreadId(threadId);
	     aMsg.setRole("assistant");
	     aMsg.setContent(ai.getAnswer());
	     aMsg.setCreatedAt(LocalDateTime.now());
	     chatRepo.save(aMsg);
	
	     ai.setThreadId(threadId);
	
	     return ai;
	 }
    
    
}
