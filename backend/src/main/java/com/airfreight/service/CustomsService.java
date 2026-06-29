package com.airfreight.service;

import com.airfreight.entity.CustomsRecord;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.CustomsRecordRepository;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 报关服务 - 对应工作流第7步（出口报关）和第12步（目的港清关）
 * 国外对接：中国e-Customs / 欧盟AIS / 美国ACE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomsService {

    private final CustomsRecordRepository customsRecordRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 提交报关申请
     */
    @Transactional
    public CustomsRecord submitDeclaration(Long shipmentId, String customsType, String hsCode, String declarationData) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        CustomsRecord record = new CustomsRecord();
        record.setDeclarationNo("DEC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + new Random().nextInt(1000));
        record.setShipment(shipment);
        record.setCustomsType(customsType);
        record.setHsCode(hsCode);
        record.setDeclarationData(declarationData);
        record.setStatus("SUBMITTED");
        record.setSubmittedAt(LocalDateTime.now());

        CustomsRecord saved = customsRecordRepository.save(record);
        log.info("报关提交成功: {} 类型={} HS={}", saved.getDeclarationNo(), customsType, hsCode);

        // 模拟e-Customs API调用
        log.info("[e-Customs API] 提交电子申报: DeclarationNo={}", saved.getDeclarationNo());

        return saved;
    }

    /**
     * 确认海关放行
     */
    @Transactional
    public CustomsRecord confirmClearance(Long recordId) {
        CustomsRecord record = customsRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("报关记录不存在: " + recordId));

        record.setStatus("CLEARED");
        record.setClearedAt(LocalDateTime.now());

        // 更新运单状态
        Shipment shipment = record.getShipment();
        if ("EXPORT".equals(record.getCustomsType())) {
            shipment.setStatus("CUSTOMS_CLEARED");
            shipment.setCustomsCleared(true);
            shipment.setCustomsClearanceTime(LocalDateTime.now());
            shipment.setHsCode(record.getHsCode());
        } else {
            shipment.setStatus("ARRIVED");
        }
        shipmentRepository.save(shipment);

        CustomsRecord saved = customsRecordRepository.save(record);
        log.info("海关放行: {} 运单={}", saved.getDeclarationNo(), shipment.getShipmentNo());
        return saved;
    }

    public List<CustomsRecord> getByShipment(Long shipmentId) {
        return customsRecordRepository.findByShipmentId(shipmentId);
    }

    public List<CustomsRecord> getAll() {
        return customsRecordRepository.findAll();
    }
}