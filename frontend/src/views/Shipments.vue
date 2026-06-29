<template>
  <div>
    <h2 style="margin-top: 0">运单管理</h2>
    <el-table :data="shipments" border stripe style="width: 100%">
      <el-table-column prop="shipmentNo" label="运单号" width="180" />
      <el-table-column prop="goodsName" label="品名" min-width="150" />
      <el-table-column prop="originAirport" label="起运港" width="100" />
      <el-table-column prop="destAirport" label="目的港" width="100" />
      <el-table-column prop="chargeableWeight" label="计费重(kg)" width="100" />
      <el-table-column prop="totalCharge" label="总费用" width="120">
        <template #default="{ row }">
          {{ row.totalCharge }} {{ row.currency }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewDetail(row)">查看</el-button>
          <el-button type="success" size="small" @click="generateInvoice(row)">生成账单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 运单详情对话框 -->
    <el-dialog v-model="dialogVisible" title="运单详情" width="800px">
      <el-descriptions :column="2" border v-if="selectedShipment">
        <el-descriptions-item label="运单号" :span="2">{{ selectedShipment.shipmentNo }}</el-descriptions-item>
        <el-descriptions-item label="品名">{{ selectedShipment.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(selectedShipment.status) }}</el-descriptions-item>
        <el-descriptions-item label="件数">{{ selectedShipment.pieceCount }}</el-descriptions-item>
        <el-descriptions-item label="计费重">{{ selectedShipment.chargeableWeight }} kg</el-descriptions-item>
        <el-descriptions-item label="起运港">{{ selectedShipment.originAirport }}</el-descriptions-item>
        <el-descriptions-item label="目的港">{{ selectedShipment.destAirport }}</el-descriptions-item>
        <el-descriptions-item label="贸易术语">{{ selectedShipment.incoterm }}</el-descriptions-item>
        <el-descriptions-item label="发货人">{{ selectedShipment.shipperName }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ selectedShipment.consigneeName }}</el-descriptions-item>
        <el-descriptions-item label="提单类型">{{ selectedShipment.awbType }}</el-descriptions-item>
        <el-descriptions-item label="主单号">{{ selectedShipment.mawbNumber }}</el-descriptions-item>
        <el-descriptions-item label="总费用" :span="2">
          <span style="color: #f56c6c; font-weight: bold">
            {{ selectedShipment.totalCharge }} {{ selectedShipment.currency }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { shipmentApi, financialApi } from '@/api'

const shipments = ref([])
const dialogVisible = ref(false)
const selectedShipment = ref(null)

function statusType(status) {
  const map = { QUOTATION: 'info', BOOKED: 'warning', CONFIRMED: '', IN_TRANSIT: 'success', ARRIVED: '', DELIVERED: 'success' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { QUOTATION: '报价中', BOOKED: '已订舱', CONFIRMED: '已确认', RECEIVED: '已收货', CUSTOMS_CLEARED: '已报关', IN_TRANSIT: '运输中', ARRIVED: '已到港', DELIVERED: '已提货' }
  return map[status] || status
}

function viewDetail(row) {
  selectedShipment.value = row
  dialogVisible.value = true
}

async function generateInvoice(row) {
  try {
    await financialApi.generateInvoice(row.id)
    ElMessage.success(`账单已生成！运单: ${row.shipmentNo}`)
  } catch (e) {
    ElMessage.error('生成账单失败')
  }
}

onMounted(async () => {
  try {
    const res = await shipmentApi.getAll()
    shipments.value = res.data
  } catch (e) {
    console.log('加载运单数据（首次运行为空）')
  }
})
</script>