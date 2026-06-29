<template>
  <div>
    <h2 style="margin-top: 0">财务管理</h2>
    <el-table :data="financials" border stripe style="width: 100%">
      <el-table-column prop="invoiceNo" label="账单编号" width="180" />
      <el-table-column label="运单号" width="160">
        <template #default="{ row }">{{ row.shipment?.shipmentNo }}</template>
      </el-table-column>
      <el-table-column prop="feeType" label="费用类型" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ feeTypeLabel(row.feeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="120">
        <template #default="{ row }">
          <span style="font-weight: 600">{{ row.amount }} {{ row.currency }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="paymentStatus" label="支付状态" width="120">
        <template #default="{ row }">
          <el-tag :type="paymentType(row.paymentStatus)" size="small">
            {{ paymentLabel(row.paymentStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式" width="120" />
      <el-table-column prop="paymentRef" label="交易参考号" width="180" />
      <el-table-column prop="dueDate" label="到期日" width="170" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button 
            type="primary" 
            size="small" 
            @click="payBySwift(row)"
            :disabled="row.paymentStatus === 'PAID'">
            SWIFT支付
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { financialApi } from '@/api'

const financials = ref([])

function feeTypeLabel(type) {
  const map = { FREIGHT: '空运费', FSC: '燃油附加费', SSC: '安全附加费', GROUND: '地面操作费', CUSTOMS_BROKER: '报关费', INSURANCE: '保险费', DUTY: '关税', VAT: '增值税', STORAGE: '滞仓费', TOTAL: '总费用' }
  return map[type] || type
}

function paymentType(status) {
  return status === 'PAID' ? 'success' : status === 'OVERDUE' ? 'danger' : 'warning'
}

function paymentLabel(status) {
  return status === 'PAID' ? '已支付' : status === 'OVERDUE' ? '已逾期' : '待支付'
}

async function payBySwift(row) {
  try {
    const { value: swiftCode } = await ElMessageBox.prompt('请输入收款方SWIFT代码', 'SWIFT国际支付', { confirmButtonText: '确认支付', cancelButtonText: '取消', inputPattern: /^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$/, inputErrorMessage: 'SWIFT代码格式不正确' })
    if (swiftCode) {
      const res = await financialApi.swiftPayment(row.id, swiftCode, 'HK123456789')
      row.paymentStatus = res.data.paymentStatus
      row.paymentRef = res.data.paymentRef
      ElMessage.success(`SWIFT支付成功！交易号: ${res.data.paymentRef}`)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('支付失败')
  }
}

onMounted(async () => {
  try {
    const res = await financialApi.getAll()
    financials.value = res.data
  } catch (e) {
    console.log('加载财务数据（首次运行为空）')
  }
})
</script>