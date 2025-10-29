package com.cattlescan.systemassistant.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cattlescan.systemassistant.entity.ChatThread;

@Repository
public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {

    List<ChatThread> findByUserIdOrderByUpdatedAtDesc(String userId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatThread t SET t.name = :name WHERE t.id = :threadId")
    void renameThread(@Param("threadId") Long threadId, @Param("name") String newName);
}
