package com.smartfactory.mes.simulation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    private Long equipmentId;
    private Long lineId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private String currentStatus;
    private Integer processOrder;
    private LocalDateTime lastStatusChangedAt;
    private LocalDateTime lastInspectionAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
