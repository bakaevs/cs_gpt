package com.cattlescan.systemassistant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cattlescan.systemassistant.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

 
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);

 
    void deleteByUserId(String userId);
}
