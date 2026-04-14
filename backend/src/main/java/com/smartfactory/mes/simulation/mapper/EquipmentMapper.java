package com.smartfactory.mes.simulation.mapper;

import com.smartfactory.mes.simulation.domain.Equipment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EquipmentMapper {

    @Select("""
            SELECT equipment_id, line_id, equipment_code, equipment_name, equipment_type,
                   current_status, process_order, last_status_changed_at, last_inspection_at,
                   is_active, created_at, updated_at
            FROM equipments
            WHERE is_active = TRUE
            ORDER BY line_id, process_order, equipment_id
            """)
    List<Equipment> selectActiveEquipments();

    @Select("""
            SELECT equipment_id, line_id, equipment_code, equipment_name, equipment_type,
                   current_status, process_order, last_status_changed_at, last_inspection_at,
                   is_active, created_at, updated_at
            FROM equipments
            WHERE line_id = #{lineId} AND is_active = TRUE
            ORDER BY process_order, equipment_id
            """)
    List<Equipment> selectByLineId(@Param("lineId") Long lineId);

    @Select("""
            SELECT equipment_id, line_id, equipment_code, equipment_name, equipment_type,
                   current_status, process_order, last_status_changed_at, last_inspection_at,
                   is_active, created_at, updated_at
            FROM equipments
            WHERE equipment_id = #{equipmentId} AND is_active = TRUE
            """)
    Equipment selectById(@Param("equipmentId") Long equipmentId);

    @Select("SELECT COUNT(*) FROM equipments")
    long countAll();

    @Insert("""
            INSERT INTO equipments (
                equipment_id, line_id, equipment_code, equipment_name, equipment_type,
                current_status, process_order, last_status_changed_at, last_inspection_at,
                is_active, created_at, updated_at
            ) VALUES (
                #{equipmentId}, #{lineId}, #{equipmentCode}, #{equipmentName}, #{equipmentType},
                #{currentStatus}, #{processOrder}, #{lastStatusChangedAt}, #{lastInspectionAt},
                #{isActive}, #{createdAt}, #{updatedAt}
            )
            """)
    void insert(Equipment equipment);

    @Update("""
            UPDATE equipments
            SET current_status = #{status},
                last_status_changed_at = #{lastStatusChangedAt},
                updated_at = #{updatedAt}
            WHERE equipment_id = #{equipmentId}
            """)
    void updateCurrentState(
            @Param("equipmentId") Long equipmentId,
            @Param("status") String status,
            @Param("lastStatusChangedAt") LocalDateTime lastStatusChangedAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
