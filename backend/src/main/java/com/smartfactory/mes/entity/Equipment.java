package com.smartfactory.mes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "line_id", foreignKey = @ForeignKey(name = "FK_EQUIPMENT_LINE"))
    private Line line;

    private String status;
    private Long production;
    private int uph;
    private Double uptime;
    private String lastUpdated;
}
