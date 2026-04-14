package com.smartfactory.mes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartfactory.mes.dto.line.LineBasicInfoResponse;
import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.repository.LineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LineServiceTest {

    private LineService lineService;

    @BeforeEach
    void setUp() {
        lineService = new LineService(new LineRepository());
    }

    @Test
    void returnsLineBasicInfoWhenLineExists() {
        LineBasicInfoResponse response = lineService.getLineBasicInfo(1L);

        assertThat(response.getLineId()).isEqualTo(1L);
        assertThat(response.getLineCode()).isEqualTo("PRESS_LOOP_PANEL");
        assertThat(response.getLineName()).isEqualTo("루프판넬 라인");
        assertThat(response.getEquipmentCount()).isEqualTo(5);
        assertThat(response.getProcessSteps()).containsExactly("언코일링", "전단", "프레스", "트림", "적재");
    }

    @Test
    void throwsExceptionWhenLineDoesNotExist() {
        assertThatThrownBy(() -> lineService.getLineBasicInfo(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("존재하지 않는 라인입니다.");
    }
}
