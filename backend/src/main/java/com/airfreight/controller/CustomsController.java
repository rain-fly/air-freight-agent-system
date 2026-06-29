package com.airfreight.controller;

import com.airfreight.entity.CustomsRecord;
import com.airfreight.service.CustomsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报关管理 - 对应工作流第7步（出口报关）和第12步（目的港清关）
 */
@RestController
@RequestMapping("/api/customs")
@RequiredArgsConstructor
@Tag(name = "报关管理", description = "出口报关/进口清关，对接中国e-Customs/欧盟AIS/美国ACE")
@CrossOrigin(origins = "*")
public class CustomsController {

    private final CustomsService customsService;

    @PostMapping("/submit/{shipmentId}")
    @Operation(summary = "提交报关申请")
    public ResponseEntity<CustomsRecord> submitDeclaration(
            @PathVariable Long shipmentId,
            @RequestParam String customsType,
            @RequestParam String hsCode,
            @RequestParam(defaultValue = "{}") String declarationData) {
        return ResponseEntity.ok(customsService.submitDeclaration(shipmentId, customsType, hsCode, declarationData));
    }

    @PutMapping("/{id}/clear")
    @Operation(summary = "确认海关放行")
    public ResponseEntity<CustomsRecord> confirmClearance(@PathVariable Long id) {
        return ResponseEntity.ok(customsService.confirmClearance(id));
    }

    @GetMapping
    @Operation(summary = "查询所有报关记录")
    public ResponseEntity<List<CustomsRecord>> getAll() {
        return ResponseEntity.ok(customsService.getAll());
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "按运单查询报关记录")
    public ResponseEntity<List<CustomsRecord>> getByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(customsService.getByShipment(shipmentId));
    }
}