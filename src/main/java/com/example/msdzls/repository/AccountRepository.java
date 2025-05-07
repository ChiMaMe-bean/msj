package com.example.msdzls.repository;

import com.example.msdzls.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 账号数据访问接口
 */
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // 添加缺失的方法定义
    @Query(value = "SELECT a.* FROM msdzls_account a " +
            "WHERE NOT EXISTS (SELECT 1 FROM msdzls_activity WHERE help_code = :userCode) " +
            "AND NOT EXISTS (SELECT 1 FROM msdzls_activity ar WHERE ar.account_id = a.id " +
            "AND (ar.day_number = :day AND ar.type = 0 OR ar.type = 0)) " +
            "ORDER BY a.id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<Account> findAvailableHelper(@Param("day") int day, @Param("userCode") String code);

    // 添加被助力查询方法
    @Query(value = "SELECT * FROM msdzls_account a WHERE NOT EXISTS (" +
            "SELECT 1 FROM msdzls_activity ar WHERE ar.account_id = a.id " +
            "AND ar.day_number = :day AND ar.type = 1) LIMIT 1",
            nativeQuery = true)
    Optional<Account> findAvailableHelped(@Param("day") int day);

    // 添加防重复查询方法
    @Query(value = "SELECT a.* FROM msdzls_account a " +
            "WHERE a.code <> :usedCode " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM msdzls_activity ar " +
            "    WHERE ar.account_id = a.id " +
            "    AND ar.day_number = :day " +
            "    AND ar.type = 1" +
            ") " +
            "ORDER BY a.id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<Account> findByCodeNotUsed(
            @Param("usedCode") String usedCode,
            @Param("day") int day
    );
}
