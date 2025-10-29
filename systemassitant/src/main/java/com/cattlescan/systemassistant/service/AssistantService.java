package com.cattlescan.systemassistant.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    public ApiResponse processQuestion(String question, String userId, Long threadId) throws IOException {

        // ✅ 1 — If no threadId → create a new thread
        if (threadId == null) {
            threadId = createThread(userId);
        }

        // ✅ 2 — Save user message
        ChatMessage userMessageEntity = new ChatMessage();
        userMessageEntity.setUserId(userId);
        userMessageEntity.setRole("user");
        userMessageEntity.setContent(question);
        userMessageEntity.setThreadId(threadId);
        chatRepo.save(userMessageEntity);

        // ✅ 3 — Generate embedding / find context
        double[] queryEmbedding = embeddingService.generateEmbedding(question);
        List<SearchResult> contextResults = vectorStoreService.findSimilar(queryEmbedding);
        String context = vectorStoreService.combineResults(contextResults);

        // ✅ 4 — Call Assistant with context
        ApiResponse response = assistantClient.askAssistant(userId, question, context);

        // ✅ 5 — Save assistant message
        ChatMessage assistantMessageEntity = new ChatMessage();
        assistantMessageEntity.setUserId(userId);
        assistantMessageEntity.setRole("assistant");
        assistantMessageEntity.setContent(response.getAnswer());
        assistantMessageEntity.setThreadId(threadId);
        chatRepo.save(assistantMessageEntity);

        // include thread id in response
        response.setThreadId(threadId);

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
    
    public Long createThread(String userId) {
        ChatMessage starter = new ChatMessage();
        starter.setUserId(userId);
        starter.setRole("system");
        starter.setContent("THREAD_STARTED");
        chatRepo.save(starter);
        starter.setThreadId(starter.getId()); // use message id as thread id
        chatRepo.save(starter);
        return starter.getThreadId();
    }
    
    public List<ChatMessage> getThread(Long threadId) {
        return chatRepo.findByThreadIdOrderByTimestampAsc(threadId);
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
	
	     List<Long> threadIds = chatRepo.findThreadIdsForUser(userId);
	     List<Map<String, Object>> list = new ArrayList<>();
	
	     for (Long tid : threadIds) {
	         String name = chatRepo.findThreadName(tid);
	
	         Map<String, Object> map = new HashMap<>();
	         map.put("threadId", tid);
	         map.put("threadName", name != null ? name : "Thread " + tid);
	
	         list.add(map);
	     }
	
	     return list;
	 }
	
	 /* ---------------------------------------------------------
	    ✅ Return all messages in a specific thread
	    --------------------------------------------------------- */
	 public List<ChatMessage> getMessagesForThread(Long threadId) {
	     return chatRepo.findByThreadIdOrderByTimestampAsc(threadId);
	 }
	
	 /* ---------------------------------------------------------
	    ✅ Rename a thread
	    --------------------------------------------------------- */
	 @Transactional
	 public void renameThread(Long threadId, String newName) {
	     chatRepo.renameThread(threadId, newName);
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
	     chatRepo.save(userMsg);
	
	     double[] embedding = embeddingService.generateEmbedding(question);
	     List<SearchResult> ctx = vectorStoreService.findSimilar(embedding);
	     String context = vectorStoreService.combineResults(ctx);
	
	     ApiResponse ai = assistantClient.askAssistant(userId, question, context);
	
	     ChatMessage aMsg = new ChatMessage();
	     aMsg.setUserId(userId);
	     aMsg.setThreadId(threadId);
	     aMsg.setRole("assistant");
	     aMsg.setContent(ai.getAnswer());
	     chatRepo.save(aMsg);
	
	     ai.setThreadId(threadId);
	
	     return ai;
	 }
    
    
}
