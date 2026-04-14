package com.smartfactory.mes.service;

import com.smartfactory.mes.dto.line.LineBasicInfoResponse;
import com.smartfactory.mes.dto.line.LineDetailResponse;
import com.smartfactory.mes.dto.equipment.EquipmentBasicInfo;
import com.smartfactory.mes.dto.alarm.AlarmBasicInfo;
import com.smartfactory.mes.entity.Line;
import com.smartfactory.mes.entity.Equipment;
import com.smartfactory.mes.entity.Alarm;
import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import com.smartfactory.mes.repository.LineRepository;
import com.smartfactory.mes.repository.EquipmentRepository;
import com.smartfactory.mes.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineService {

    private final LineRepository lineRepository;
    private final EquipmentRepository equipmentRepository;
    private final AlarmRepository alarmRepository;

    public LineBasicInfoResponse getLineBasicInfo(Long lineId) {
        Line line = lineRepository.findById(lineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LINE_NOT_FOUND));

        return LineBasicInfoResponse.builder()
                .lineId(line.getId())
                .lineCode(line.getCode())
                .lineName(line.getName())
                .processName(line.getProcessName())
                .panelType(line.getPanelType())
                .description(line.getDescription())
                .equipmentCount(line.getEquipmentCount())
                .processSteps(line.getProcessSteps())
                .build();
    }

    public LineDetailResponse getLineDetail(Long lineId) {
        Line line = lineRepository.findById(lineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LINE_NOT_FOUND));

        // 설비 목록 조회
        List<Equipment> equipmentList = equipmentRepository.findByLineId(lineId);
        List<EquipmentBasicInfo> equipmentInfoList = equipmentList.stream()
                .map(eq -> EquipmentBasicInfo.builder()
                        .equipmentId(eq.getId())
                        .equipmentName(eq.getName())
                        .status(eq.getStatus())
                        .production(eq.getProduction())
                        .uph(eq.getUph())
                        .uptime(eq.getUptime())
                        .lastUpdated(eq.getLastUpdated())
                        .build())
                .collect(Collectors.toList());

        // 최근 알람 목록 조회 (최대 6개)
        Pageable pageable = PageRequest.of(0, 6);
        List<Alarm> alarmList = alarmRepository.findByLineIdOrderByTimeDesc(lineId, pageable);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<AlarmBasicInfo> alarmInfoList = alarmList.stream()
                .map(alarm -> AlarmBasicInfo.builder()
                        .alarmId(alarm.getId())
                        .time(alarm.getTime().format(formatter))
                        .equipmentId(alarm.getEquipment().getId())
                        .equipmentName(alarm.getEquipment().getName())
                        .message(alarm.getMessage())
                        .severity(alarm.getSeverity())
                        .build())
                .collect(Collectors.toList());

        // 달성률 계산
        Double achievementRate = line.getTargetProduction() > 0
                ? (line.getProduction() * 100.0) / line.getTargetProduction()
                : 0.0;

        return LineDetailResponse.builder()
                .lineId(line.getId())
                .lineCode(line.getCode())
                .lineName(line.getName())
                .status(line.getStatus())
                .processName(line.getProcessName())
                .panelType(line.getPanelType())
                .description(line.getDescription())
                .equipmentCount(line.getEquipmentCount())
                .processSteps(line.getProcessSteps())
                .production(line.getProduction())
                .targetProduction(line.getTargetProduction())
                .achievementRate(Math.round(achievementRate * 10.0) / 10.0)
                .uptime(line.getUptime())
                .defectRate(line.getDefectRate())
                .equipmentList(equipmentInfoList)
                .alarmList(alarmInfoList)
                .build();
    }
}
