package com.dyh.salesAgent.repository;
import com.dyh.salesAgent.entity.ChatMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, Long> {

    Optional<ChatMemoryEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
