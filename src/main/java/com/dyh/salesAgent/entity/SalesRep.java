package com.dyh.salesAgent.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sa_sales_rep")
@Getter
@Setter
@NoArgsConstructor
public class SalesRep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "login_name", nullable = false, unique = true, length = 50)
    private String loginName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(nullable = false, length = 20)
    private String role;   // SALES_REP / SALES_MANAGER / SALES_DIRECTOR，角色

    @Column(length = 100)
    private String email;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
