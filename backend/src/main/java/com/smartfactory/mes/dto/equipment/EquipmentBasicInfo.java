package com.smartfactory.mes.dto.equipment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EquipmentBasicInfo {

    private final Long equipmentId;
    private final String equipmentName;
    private final String status;
    private final Long production;
    private final int uph;
    private final Double uptime;
    private final String lastUpdated;
}
