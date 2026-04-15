package com.smartfactory.mes.simulation.persistence.mapper;

import com.smartfactory.mes.simulation.domain.ProductionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductionRecordMapper {

    @Select("SELECT COUNT(*) FROM production_records")
    long countAll();

    @Insert("""
            INSERT INTO production_records (
                line_id, equipment_id, record_time, production_count,
                defect_count, operation_rate, uph, created_at
            ) VALUES (
                #{lineId}, #{equipmentId}, #{recordTime}, #{productionCount},
                #{defectCount}, #{operationRate}, #{uph}, #{createdAt}
            )
            """)
    void insert(ProductionRecord record);

    @Delete("DELETE FROM production_records")
    void deleteAll();

    @Select("""
            SELECT record_id, line_id, equipment_id, record_time, production_count,
                   defect_count, operation_rate, uph, created_at
            FROM production_records
            WHERE record_time >= #{since}
            ORDER BY record_time, equipment_id
            """)
    List<ProductionRecord> selectSince(@Param("since") LocalDateTime since);

    @Select("""
            SELECT record_id, line_id, equipment_id, record_time, production_count,
                   defect_count, operation_rate, uph, created_at
            FROM production_records
            WHERE line_id = #{lineId} AND record_time >= #{since}
            ORDER BY record_time, equipment_id
            """)
    List<ProductionRecord> selectByLineSince(@Param("lineId") Long lineId, @Param("since") LocalDateTime since);

    @Select("""
            SELECT record_id, line_id, equipment_id, record_time, production_count,
                   defect_count, operation_rate, uph, created_at
            FROM production_records
            WHERE equipment_id = #{equipmentId} AND record_time >= #{since}
            ORDER BY record_time DESC
            """)
    List<ProductionRecord> selectByEquipmentSince(@Param("equipmentId") Long equipmentId, @Param("since") LocalDateTime since);
}
