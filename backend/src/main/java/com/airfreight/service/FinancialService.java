package com.airfreight.service;

import com.airfreight.entity.Financial;
import com.airfreight.entity.Shipment;
import com.airfreight.repository.FinancialRepository;
import com.airfreight.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 财务服务 - 对应工作流中的费用结算
 * 包含：费用计算、账单生成、国际支付对接(SWIFT)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialRepository financialRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 生成运单费用账单
     */
    @Transactional
    public List<Financial> generateInvoice(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("运单不存在: " + shipmentId));

        List<Financial> invoices = new ArrayList<>();

        // 逐项生成费用账单
        invoices.add(createFinancialItem(shipment, "FREIGHT", shipment.getFreightCharge()));
        invoices.add(createFinancialItem(shipment, "FSC", shipment.getFuelSurcharge()));
        invoices.add(createFinancialItem(shipment, "SSC", shipment.getSecuritySurcharge()));
        invoices.add(createFinancialItem(shipment, "GROUND", shipment.getGroundHandlingFee()));
        invoices.add(createFinancialItem(shipment, "CUSTOMS_BROKER", shipment.getCustomsBrokerFee()));
        invoices.add(createFinancialItem(shipment, "INSURANCE", shipment.getInsuranceFee()));

        // 如有关税
        if (shipment.getCustomsDuty() != null && shipment.getCustomsDuty().compareTo(BigDecimal.ZERO) > 0) {
            invoices.add(createFinancialItem(shipment, "DUTY", shipment.getCustomsDuty()));
        }
        if (shipment.getVatAmount() != null && shipment.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
            invoices.add(createFinancialItem(shipment, "VAT", shipment.getVatAmount()));
        }
        if (shipment.getStorageFee() != null && shipment.getStorageFee().compareTo(BigDecimal.ZERO) > 0) {
            invoices.add(createFinancialItem(shipment, "STORAGE", shipment.getStorageFee()));
        }

        // 总费用账单
        Financial total = createFinancialItem(shipment, "TOTAL", shipment.getTotalCharge());

        log.info("账单生成完成: 运单={} 总费用={} {} 共{}项",
                shipment.getShipmentNo(), shipment.getTotalCharge(), shipment.getCurrency(), invoices.size());

        return invoices;
    }

    private Financial createFinancialItem(Shipment shipment, String feeType, BigDecimal amount) {
        Financial item = new Financial();
        item.setInvoiceNo("INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + new Random().nextInt(100));
        item.setShipment(shipment);
        item.setFeeType(feeType);
        item.setAmount(amount);
        item.setCurrency(shipment.getCurrency());
        item.setPaymentStatus("PENDING");
        item.setDueDate(LocalDateTime.now().plusDays(15));
        return financialRepository.save(item);
    }

    /**
     * 计算滞仓费
     * 模拟：超期未提货按0.5元/kg/天计算
     */
    public BigDecimal calculateStorageFee(Shipment shipment, int overdueDays) {
        if (overdueDays <= 0) return BigDecimal.ZERO;
        BigDecimal fee = shipment.getChargeableWeight()
                .multiply(BigDecimal.valueOf(0.5))
                .multiply(BigDecimal.valueOf(overdueDays))
                .setScale(2, RoundingMode.HALF_UP);
        log.info("滞仓费计算: 计费重={}kg 超期={}天 费用={}",
                shipment.getChargeableWeight(), overdueDays, fee);
        return fee;
    }

    /**
     * 模拟SWIFT国际转账支付
     * 国外对接：通过SWIFT API发起国际电汇
     */
    @Transactional
    public Financial processSwiftPayment(Long financialId, String swiftCode, String bankAccount) {
        Financial financial = financialRepository.findById(financialId)
                .orElseThrow(() -> new RuntimeException("账单不存在: " + financialId));

        log.info("[SWIFT API] 发起国际支付: InvoiceNo={} 金额={} {} SWIFT={} 账号={}",
                financial.getInvoiceNo(), financial.getAmount(), financial.getCurrency(),
                swiftCode, bankAccount);

        // 模拟SWIFT交易
        String swiftRef = "SWIFT" + System.currentTimeMillis();
        financial.setPaymentRef(swiftRef);
        financial.setPaymentMethod("SWIFT");
        financial.setPaymentStatus("PAID");
        financial.setPaidAt(LocalDateTime.now());

        Financial saved = financialRepository.save(financial);

        // 更新运单支付状态
        Shipment shipment = saved.getShipment();
        shipment.setPaid(true);
        shipment.setPaidTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        log.info("[SWIFT API] 支付成功: 交易号={}", swiftRef);
        return saved;
    }
}