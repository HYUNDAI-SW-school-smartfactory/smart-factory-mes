package com.smartfactory.mes.simulation.persistence.mapper;

import com.smartfactory.mes.simulation.domain.AlarmHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface AlarmHistoryMapper {

    @Insert("""
            INSERT INTO alarm_histories (
                line_id, equipment_id, alarm_type, severity, message,
                acknowledged, acknowledged_at, created_at
            ) VALUES (
                #{lineId}, #{equipmentId}, #{alarmType}, #{severity}, #{message},
                #{acknowledged}, #{acknowledgedAt}, #{createdAt}
            )
            """)
    void insert(AlarmHistory alarmHistory);

    @Select("""
            SELECT alarm_id, line_id, equipment_id, alarm_type, severity, message,
                   acknowledged, acknowledged_at, created_at
            FROM alarm_histories
            ORDER BY created_at DESC
            LIMIT 10
            """)
    List<AlarmHistory> selectRecentDashboardAlarms();

    @Select("""
            SELECT alarm_id, line_id, equipment_id, alarm_type, severity, message,
                   acknowledged, acknowledged_at, created_at
            FROM alarm_histories
            WHERE line_id = #{lineId}
            ORDER BY created_at DESC
            LIMIT 20
            """)
    List<AlarmHistory> selectRecentByLineId(@Param("lineId") Long lineId);

    @Select("""
            SELECT alarm_id, line_id, equipment_id, alarm_type, severity, message,
                   acknowledged, acknowledged_at, created_at
            FROM alarm_histories
            WHERE equipment_id = #{equipmentId}
            ORDER BY created_at DESC
            LIMIT 20
            """)
    List<AlarmHistory> selectRecentByEquipmentId(@Param("equipmentId") Long equipmentId);

    @Delete("DELETE FROM alarm_histories")
    void deleteAll();
}
