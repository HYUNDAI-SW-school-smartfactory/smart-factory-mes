package com.smartfactory.mes.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Line {

    @Id
    private Long id;

    private String code;
    private String name;
    private String processName;
    private String panelType;
    private String description;
    private int equipmentCount;
    private String status;

    @ElementCollection
    private List<String> processSteps;

    // KPI 정보
    private Long production;
    private Long targetProduction;
    private Double uptime;
    private Double defectRate;
}
