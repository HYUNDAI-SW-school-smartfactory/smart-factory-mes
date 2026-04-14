package com.smartfactory.mes.simulation.persistence.mapper;

import com.smartfactory.mes.simulation.domain.ProductionLine;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductionLineMapper {

    @Select("""
            SELECT line_id, line_code, line_name, product_name, current_status, target_production,
                   location, is_active, created_at, updated_at
            FROM production_lines
            WHERE is_active = TRUE
            ORDER BY line_id
            """)
    List<ProductionLine> selectActiveLines();

    @Select("""
            SELECT line_id, line_code, line_name, product_name, current_status, target_production,
                   location, is_active, created_at, updated_at
            FROM production_lines
            WHERE line_id = #{lineId} AND is_active = TRUE
            """)
    ProductionLine selectById(@Param("lineId") Long lineId);

    @Select("SELECT COUNT(*) FROM production_lines")
    long countAll();

    @Insert("""
            INSERT INTO production_lines (
                line_id, line_code, line_name, product_name, current_status,
                target_production, location, is_active, created_at, updated_at
            ) VALUES (
                #{lineId}, #{lineCode}, #{lineName}, #{productName}, #{currentStatus},
                #{targetProduction}, #{location}, #{isActive}, #{createdAt}, #{updatedAt}
            )
            """)
    void insert(ProductionLine line);

    @Update("""
            UPDATE production_lines
            SET current_status = #{status},
                updated_at = #{updatedAt}
            WHERE line_id = #{lineId}
            """)
    void updateCurrentStatus(
            @Param("lineId") Long lineId,
            @Param("status") String status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
