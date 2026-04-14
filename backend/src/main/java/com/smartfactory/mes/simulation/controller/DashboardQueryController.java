package com.smartfactory.mes.simulation.controller;

import com.smartfactory.mes.global.api.ApiResponse;
import com.smartfactory.mes.simulation.dto.DashboardResponseModels;
import com.smartfactory.mes.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class DashboardQueryController {

    private final SimulationQueryService simulationQueryService;

    @GetMapping
    public ApiResponse<DashboardResponseModels.DashboardSnapshotResponse> getDashboardSnapshot() {
        return ApiResponse.success(simulationQueryService.getDashboardSnapshot());
    }
}
