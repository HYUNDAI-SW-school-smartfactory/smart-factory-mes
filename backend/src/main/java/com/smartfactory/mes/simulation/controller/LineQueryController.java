package com.smartfactory.mes.simulation.controller;

import com.smartfactory.mes.global.api.ApiResponse;
import com.smartfactory.mes.simulation.dto.LineResponseModels;
import com.smartfactory.mes.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lines")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class LineQueryController {

    private final SimulationQueryService simulationQueryService;

    @GetMapping("/{lineId}")
    public ApiResponse<LineResponseModels.LineDetailResponse> getLineDetail(@PathVariable Long lineId) {
        return ApiResponse.success(simulationQueryService.getLineDetail(lineId));
    }
}
