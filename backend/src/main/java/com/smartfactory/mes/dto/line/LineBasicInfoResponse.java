package com.smartfactory.mes.dto.line;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LineBasicInfoResponse {

    private final Long lineId;
    private final String lineCode;
    private final String lineName;
    private final String processName;
    private final String panelType;
    private final String description;
    private final int equipmentCount;
    private final List<String> processSteps;
}
