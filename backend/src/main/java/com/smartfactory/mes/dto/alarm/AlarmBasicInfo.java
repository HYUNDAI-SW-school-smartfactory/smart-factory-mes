package com.smartfactory.mes.dto.alarm;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlarmBasicInfo {

    private final Long alarmId;
    private final String time;
    private final Long equipmentId;
    private final String equipmentName;
    private final String message;
    private final String severity;
}
