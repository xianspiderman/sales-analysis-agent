package com.dyh.salesAgent.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sa_chat_memory")
@Getter
@Setter
@NoArgsConstructor
public class ChatMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String messages;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist // 保存新实体时用
    @PreUpdate // 修改已有实体时用
    void touch() { //所以同一个touch()方法可以同时负责新增时间和更新时间，自动调用
        updatedAt = LocalDateTime.now();
    }
}
