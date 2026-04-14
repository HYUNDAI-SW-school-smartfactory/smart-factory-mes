package com.smartfactory.mes.simulation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRecord {

    private Long recordId;
    private Long lineId;
    private Long equipmentId;
    private LocalDateTime recordTime;
    private Integer productionCount;
    private Integer defectCount;
    private BigDecimal operationRate;
    private Integer uph;
    private LocalDateTime createdAt;
}
