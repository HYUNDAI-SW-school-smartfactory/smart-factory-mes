package com.smartfactory.mes.dto.line;

import com.smartfactory.mes.dto.equipment.EquipmentBasicInfo;
import com.smartfactory.mes.dto.alarm.AlarmBasicInfo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LineDetailResponse {

    private final Long lineId;
    private final String lineCode;
    private final String lineName;
    private final String status;
    private final String processName;
    private final String panelType;
    private final String description;
    private final int equipmentCount;
    private final List<String> processSteps;

    // KPI 정보
    private final Long production;
    private final Long targetProduction;
    private final Double achievementRate;
    private final Double uptime;
    private final Double defectRate;

    // 관련 데이터
    private final List<EquipmentBasicInfo> equipmentList;
    private final List<AlarmBasicInfo> alarmList;
}
