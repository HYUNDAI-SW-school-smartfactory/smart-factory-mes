package com.smartfactory.mes.controller;

import com.smartfactory.mes.dto.line.LineBasicInfoResponse;
import com.smartfactory.mes.dto.line.LineDetailResponse;
import com.smartfactory.mes.global.api.ApiResponse;
import com.smartfactory.mes.service.LineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lines")
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;

    @GetMapping("/{lineId}")
    public ApiResponse<LineBasicInfoResponse> getLineBasicInfo(@PathVariable Long lineId) {
        return ApiResponse.success(lineService.getLineBasicInfo(lineId));
    }

    @GetMapping("/{lineId}/detail")
    public ApiResponse<LineDetailResponse> getLineDetail(@PathVariable Long lineId) {
        return ApiResponse.success(lineService.getLineDetail(lineId));
    }
}
