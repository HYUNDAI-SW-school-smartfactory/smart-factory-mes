package com.smartfactory.mes.simulation.persistence.mapper;

import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EquipmentStatusHistoryMapper {

    @Insert("""
            INSERT INTO equipment_status_history (
                equipment_id, status, started_at, ended_at, duration_seconds, created_at
            ) VALUES (
                #{equipmentId}, #{status}, #{startedAt}, #{endedAt}, #{durationSeconds}, #{createdAt}
            )
            """)
    void insert(EquipmentStatusHistory history);

    @Update("""
            UPDATE equipment_status_history
            SET ended_at = #{endedAt},
                duration_seconds = #{durationSeconds}
            WHERE equipment_id = #{equipmentId} AND ended_at IS NULL
            """)
    int closeOpenHistory(
            @Param("equipmentId") Long equipmentId,
            @Param("endedAt") LocalDateTime endedAt,
            @Param("durationSeconds") Integer durationSeconds
    );

    @Select("""
            SELECT status_history_id, equipment_id, status, started_at, ended_at, duration_seconds, created_at
            FROM equipment_status_history
            WHERE started_at >= #{since}
               OR ended_at IS NULL
               OR ended_at >= #{since}
            ORDER BY equipment_id, started_at
            """)
    List<EquipmentStatusHistory> selectIntersecting(@Param("since") LocalDateTime since);

    @Select("""
            SELECT COUNT(*)
            FROM equipment_status_history
            WHERE equipment_id = #{equipmentId} AND ended_at IS NULL
            """)
    long countOpenHistory(@Param("equipmentId") Long equipmentId);
}
