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
public class AlarmHistory {

    private Long alarmId;
    private Long lineId;
    private Long equipmentId;
    private String alarmType;
    private String severity;
    private String message;
    private Boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
