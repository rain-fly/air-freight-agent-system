<template>
  <div>
    <h2 style="margin-top: 0">订舱管理</h2>
    <el-table :data="bookings" border stripe style="width: 100%">
      <el-table-column prop="bookingNo" label="订舱编号" width="180" />
      <el-table-column label="运单号" width="180">
        <template #default="{ row }">{{ row.shipment?.shipmentNo }}</template>
      </el-table-column>
      <el-table-column label="品名" min-width="150">
        <template #default="{ row }">{{ row.shipment?.goodsName }}</template>
      </el-table-column>
      <el-table-column label="航线" width="130">
        <template #default="{ row }">
          {{ row.shipment?.originAirport }} → {{ row.shipment?.destAirport }}
        </template>
      </el-table-column>
      <el-table-column prop="bookingStatus" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.bookingStatus === 'CONFIRMED' ? 'success' : 'warning'" size="small">
            {{ row.bookingStatus === 'CONFIRMED' ? '已确认' : '待确认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseReceipt" label="入仓通知" width="160" />
      <el-table-column prop="cutOffTime" label="截止交货" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button 
            type="primary" 
            size="small" 
            @click="confirmBooking(row)"
            :disabled="row.bookingStatus === 'CONFIRMED'">
            确认舱位
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { bookingApi } from '@/api'

const bookings = ref([])

async function confirmBooking(row) {
  try {
    const res = await bookingApi.confirm(row.id)
    row.bookingStatus = res.data.bookingStatus
    ElMessage.success(`订舱确认成功！${res.data.bookingNo}`)
  } catch (e) {
    ElMessage.error('确认失败')
  }
}

onMounted(async () => {
  try {
    const res = await bookingApi.getAll()
    bookings.value = res.data
  } catch (e) {
    console.log('加载订舱数据（首次运行为空）')
  }
})
</script>