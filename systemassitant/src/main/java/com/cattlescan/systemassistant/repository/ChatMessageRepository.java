package com.cattlescan.systemassistant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cattlescan.systemassistant.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /* ---------------------------------------------------------
       ✅ List thread IDs for a user ordered by latest message
       --------------------------------------------------------- */
    @Query("SELECT c.threadId " +
           "FROM ChatMessage c " +
           "WHERE c.userId = :userId " +
           "GROUP BY c.threadId " +
           "ORDER BY MAX(c.createdAt) DESC")
    List<Long> findThreadIdsForUser(@Param("userId") String userId);


    /* ---------------------------------------------------------
       ✅ Get messages for a thread chronologically 
       --------------------------------------------------------- */
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);


    /* ---------------------------------------------------------
       ✅ Optional: get ALL messages for user (chronological)
       --------------------------------------------------------- */
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(String userId);


    /* ---------------------------------------------------------
       ✅ Delete operations
       --------------------------------------------------------- */
    void deleteByThreadId(Long threadId);

    void deleteByUserId(String userId);


    /* ---------------------------------------------------------
       ✅ Count messages between two timestamps
       --------------------------------------------------------- */
    @Query("SELECT COUNT(c) " +
           "FROM ChatMessage c " +
           "WHERE c.threadId = :threadId " +
           "AND c.createdAt >= :from " +
           "AND c.createdAt < :to")
    long countMessagesInRange(@Param("threadId") Long threadId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
