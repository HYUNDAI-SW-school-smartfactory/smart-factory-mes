package com.smartfactory.mes.simulation.controller;

import com.smartfactory.mes.global.api.ApiResponse;
import com.smartfactory.mes.simulation.dto.equipment.EquipmentResponseModels;
import com.smartfactory.mes.simulation.service.EquipmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/equipments")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class EquipmentQueryController {

    private final EquipmentQueryService equipmentQueryService;

    @GetMapping("/{equipmentId}")
    public ApiResponse<EquipmentResponseModels.EquipmentDetailResponse> getEquipmentDetail(
            @PathVariable Long equipmentId
    ) {
        return ApiResponse.success(equipmentQueryService.getEquipmentDetail(equipmentId));
    }
}
