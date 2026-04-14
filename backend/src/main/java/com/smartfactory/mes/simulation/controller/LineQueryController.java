package com.smartfactory.mes.simulation.controller;

import com.smartfactory.mes.global.api.ApiResponse;
import com.smartfactory.mes.simulation.dto.line.LineResponseModels;
import com.smartfactory.mes.simulation.service.LineQueryService;
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

    private final LineQueryService lineQueryService;

    @GetMapping("/{lineId}")
    public ApiResponse<LineResponseModels.LineDetailResponse> getLineDetail(@PathVariable Long lineId) {
        return ApiResponse.success(lineQueryService.getLineDetail(lineId));
    }
}
