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
       ✅ Threads for a user (ordered by last message time)
       (Avoids SQL 8120: threadId is grouped; timestamp aggregated)
       --------------------------------------------------------- */
    @Query("SELECT c.threadId " +
           "FROM ChatMessage c " +
           "WHERE c.userId = :userId " +
           "GROUP BY c.threadId " +
           "ORDER BY MAX(c.timestamp) DESC")
    List<Long> findThreadIdsForUser(@Param("userId") String userId);

    /* ---------------------------------------------------------
       ✅ Messages in a thread (chronological)
       --------------------------------------------------------- */
    List<ChatMessage> findByThreadIdOrderByTimestampAsc(Long threadId);

    /* ---------------------------------------------------------
       ✅ Rename a thread (update all rows in that thread)
       --------------------------------------------------------- */
    @Modifying
    @Query("UPDATE ChatMessage c SET c.threadName = :name WHERE c.threadId = :threadId")
    void renameThread(@Param("threadId") Long threadId, @Param("name") String name);

    /* ---------------------------------------------------------
       ✅ Get a thread name (first non-null, earliest row)
       JPQL can’t do TOP 1, so use native for SQL Server.
       --------------------------------------------------------- */
    @Query(
        value = "SELECT TOP 1 threadName " +
                "FROM ChatMessage " +
                "WHERE threadId = :threadId AND threadName IS NOT NULL " +
                "ORDER BY id ASC",
        nativeQuery = true
    )
    String findThreadName(@Param("threadId") Long threadId);

    /* ---------------------------------------------------------
       🧰 (Optional) Delete helpers
       --------------------------------------------------------- */
    void deleteByThreadId(Long threadId);
    void deleteByUserId(String userId);

    /* ---------------------------------------------------------
       🧰 (Optional) Count messages in a time range (debug/metrics)
       --------------------------------------------------------- */
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.threadId = :threadId AND c.timestamp >= :from AND c.timestamp < :to")
    long countMessagesInRange(@Param("threadId") Long threadId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
    
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);
}
