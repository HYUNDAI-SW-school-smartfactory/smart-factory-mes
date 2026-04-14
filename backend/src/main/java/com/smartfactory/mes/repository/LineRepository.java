package com.smartfactory.mes.repository;

import com.smartfactory.mes.entity.Line;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class LineRepository {

    private final Map<Long, Line> lines = Map.of(
            1L, Line.builder()
                    .id(1L)
                    .code("PRESS_LOOP_PANEL")
                    .name("루프판넬 라인")
                    .processName("Press")
                    .panelType("루프판넬")
                    .description("자동차 차체 상단에 적용되는 루프판넬을 생산하는 프레스 라인입니다.")
                    .equipmentCount(5)
                    .processSteps(List.of("언코일링", "전단", "프레스", "트림", "적재"))
                    .build(),
            2L, Line.builder()
                    .id(2L)
                    .code("PRESS_DOOR_PANEL")
                    .name("도어판넬 라인")
                    .processName("Press")
                    .panelType("도어판넬")
                    .description("자동차 도어 외판용 패널을 성형 및 트림하는 프레스 라인입니다.")
                    .equipmentCount(5)
                    .processSteps(List.of("언코일링", "전단", "프레스", "트림", "적재"))
                    .build(),
            3L, Line.builder()
                    .id(3L)
                    .code("PRESS_TRUNK_PANEL")
                    .name("트렁크 판넬 라인")
                    .processName("Press")
                    .panelType("트렁크 판넬")
                    .description("차량 후면 트렁크 판넬을 생산하는 프레스 라인입니다.")
                    .equipmentCount(5)
                    .processSteps(List.of("언코일링", "전단", "프레스", "트림", "적재"))
                    .build()
    );

    public Optional<Line> findById(Long id) {
        return Optional.ofNullable(lines.get(id));
    }
}
