package com.example.msdzls.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 活动记录实体类
 */
@Entity
@Table(name = "msdzls_activity")
@Data
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 主键ID

    @Column(name = "account_id", nullable = false)
    private Integer accountId; // 关联账号id

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber; // 活动当天在一年中的第几天（如126）

    @Column(nullable = false)
    private Integer type; // 类型：0-助力他人 1-被他人助力

    @Column(name = "help_code", nullable = false, length = 12, unique = true)
    private String helpCode; // 被助力的码（全局唯一）

    @CreationTimestamp
    private LocalDateTime createdAt; // 记录创建时间
}