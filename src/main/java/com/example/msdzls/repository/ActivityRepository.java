package com.example.msdzls.repository;

import com.example.msdzls.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 活动记录数据访问接口
 */
public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    /**
     * 检查助力码是否已存在
     * @param helpCode 被检查的助力码
     */
    @Query("SELECT COUNT(DISTINCT a.type) > 1 " +
            "FROM Activity a " +
            "WHERE a.helpCode = :helpCode " +
            "  AND a.dayNumber = :dayNumber " +
            "  AND a.type IN (0, 1)")
    boolean existsByHelpCodeAndDayAndTypeConflict(@Param("helpCode") String helpCode, @Param("dayNumber") int dayNumber);
}
