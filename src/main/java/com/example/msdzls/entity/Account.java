package com.example.msdzls.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 账号信息实体类
 */
@Entity
@Table(name = "msdzls_account")
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 主键ID

    @Column(nullable = false, length = 500)
    private String cookies; // 4399账号的Cookies信息

    @Column(nullable = false, length = 12, unique = true)
    private String code; // 12位唯一助力码
}
