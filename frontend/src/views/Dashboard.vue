<template>
  <div>
    <h2 style="margin-top: 0">首页概览</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6" v-for="card in stats" :key="card.label">
        <el-card shadow="hover">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div>
              <p style="margin: 0; color: #8a9db0; font-size: 13px">{{ card.label }}</p>
              <p style="margin: 5px 0 0; font-size: 28px; font-weight: bold; color: #303133">{{ card.value }}</p>
            </div>
            <el-icon :size="40" :color="card.color">
              <component :is="card.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 15步工作流进度 -->
    <el-card style="margin-bottom: 20px">
      <template #header>
        <span style="font-weight: 600">15步工作流进度</span>
      </template>
      <el-steps :active="currentStep" align-center>
        <el-step title="询价" description="第1步" />
        <el-step title="订舱" description="第4步" />
        <el-step title="交货" description="第5步" />
        <el-step title="报关" description="第7步" />
        <el-step title="运输" description="第9步" />
        <el-step title="清关" description="第12步" />
        <el-step title="提货" description="第14步" />
      </el-steps>
    </el-card>

    <!-- 快捷操作 -->
    <el-card>
      <template #header>
        <span style="font-weight: 600">快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="action in quickActions" :key="action.label">
          <el-button :type="action.type" style="width: 100%" @click="$router.push(action.path)">
            <el-icon><component :is="action.icon" /></el-icon>
            {{ action.label }}
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { shipmentApi, bookingApi, financialApi } from '@/api'

const stats = ref([
  { label: '运单总数', value: 0, icon: 'Document', color: '#409eff' },
  { label: '待处理订舱', value: 0, icon: 'TakeawayBox', color: '#e6a23c' },
  { label: '在途运输', value: 0, icon: 'Plane', color: '#67c23a' },
  { label: '待支付账单', value: 0, icon: 'Money', color: '#f56c6c' }
])

const currentStep = ref(1)

const quickActions = [
  { label: '新建询价', type: 'primary', icon: 'Edit', path: '/quotation' },
  { label: '订舱管理', type: 'success', icon: 'TakeawayBox', path: '/bookings' },
  { label: '财务管理', type: 'warning', icon: 'Money', path: '/financials' },
  { label: '客户管理', type: 'info', icon: 'User', path: '/customers' }
]

onMounted(async () => {
  try {
    const shipments = await shipmentApi.getAll()
    stats.value[0].value = shipments.data.length
    
    const bookings = await bookingApi.getAll()
    const pendingBookings = bookings.data.filter(b => b.bookingStatus === 'SUBMITTED')
    stats.value[1].value = pendingBookings.length
    
    const financials = await financialApi.getAll()
    const pendingPayments = financials.data.filter(f => f.paymentStatus === 'PENDING')
    stats.value[3].value = pendingPayments.length
  } catch (e) {
    console.log('加载统计数据（首次运行使用默认值）')
    stats.value.forEach(s => { s.value = '—' })
  }
})
</script>