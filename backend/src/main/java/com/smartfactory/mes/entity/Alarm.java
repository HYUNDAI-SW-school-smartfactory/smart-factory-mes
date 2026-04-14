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
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm {

    @Id
    private Long id;

    private LocalDateTime time;

    @ManyToOne
    @JoinColumn(name = "line_id", foreignKey = @ForeignKey(name = "FK_ALARM_LINE"))
    private Line line;

    @ManyToOne
    @JoinColumn(name = "equipment_id", foreignKey = @ForeignKey(name = "FK_ALARM_EQUIPMENT"))
    private Equipment equipment;

    private String message;
    private String severity;
}
