<template>
  <div>
    <h2 style="margin-top: 0">询价报价</h2>
    <el-row :gutter="20">
      <!-- 左侧：货物信息输入 -->
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight: 600">货物信息</span></template>
          <el-form :model="form" label-width="100px" size="default">
            <el-form-item label="品名">
              <el-input v-model="form.goodsName" placeholder="请输入货物名称" />
            </el-form-item>
            <el-form-item label="品类">
              <el-select v-model="form.goodsCategory" style="width: 100%">
                <el-option label="普货" value="GENERAL" />
                <el-option label="电池类" value="BATTERY" />
                <el-option label="危险品" value="DANGEROUS" />
                <el-option label="食品" value="FOOD" />
                <el-option label="化妆品" value="COSMETIC" />
              </el-select>
            </el-form-item>
            <el-form-item label="件数">
              <el-input-number v-model="form.pieceCount" :min="1" style="width: 100%" />
            </el-form-item>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="长(cm)">
                  <el-input-number v-model="form.lengthCm" :min="0" :precision="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="宽(cm)">
                  <el-input-number v-model="form.widthCm" :min="0" :precision="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="高(cm)">
                  <el-input-number v-model="form.heightCm" :min="0" :precision="1" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="毛重(kg)">
              <el-input-number v-model="form.grossWeight" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="起运港">
              <el-select v-model="form.originAirport" style="width: 100%">
                <el-option label="上海浦东(PVG)" value="PVG" />
                <el-option label="广州白云(CAN)" value="CAN" />
              </el-select>
            </el-form-item>
            <el-form-item label="目的港">
              <el-select v-model="form.destAirport" style="width: 100%">
                <el-option label="纽约肯尼迪(JFK)" value="JFK" />
                <el-option label="洛杉矶(LAX)" value="LAX" />
                <el-option label="伦敦希思罗(LHR)" value="LHR" />
                <el-option label="法兰克福(FRA)" value="FRA" />
                <el-option label="新加坡(SIN)" value="SIN" />
              </el-select>
            </el-form-item>
            <el-form-item label="贸易术语">
              <el-select v-model="form.incoterm" style="width: 100%">
                <el-option label="FOB" value="FOB" />
                <el-option label="CIF" value="CIF" />
                <el-option label="EXW" value="EXW" />
                <el-option label="DDP" value="DDP" />
              </el-select>
            </el-form-item>
            <el-form-item label="发货人">
              <el-input v-model="form.shipperName" placeholder="发货人名称" />
            </el-form-item>
            <el-form-item label="收货人">
              <el-input v-model="form.consigneeName" placeholder="收货人名称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="getQuotation" :loading="loading">
                <el-icon><Search /></el-icon> 获取报价
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：报价结果 -->
      <el-col :span="12">
        <el-card v-if="quotation">
          <template #header><span style="font-weight: 600">报价结果</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="报价编号">{{ quotation.quotationNo }}</el-descriptions-item>
            <el-descriptions-item label="有效期">{{ quotation.validUntil }}</el-descriptions-item>
            <el-descriptions-item label="体积重">{{ quotation.volumeWeight }} kg</el-descriptions-item>
            <el-descriptions-item label="计费重">{{ quotation.chargeableWeight }} kg</el-descriptions-item>
            <el-descriptions-item label="空运费">{{ quotation.freightCharge }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="燃油附加费FSC">{{ quotation.fuelSurcharge }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="安全附加费SSC">{{ quotation.securitySurcharge }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="地面操作费">{{ quotation.groundHandlingFee }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="报关费">{{ quotation.customsBrokerFee }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="保险费">{{ quotation.insuranceFee }} {{ quotation.currency }}</el-descriptions-item>
            <el-descriptions-item label="总费用" :span="2">
              <span style="color: #f56c6c; font-size: 20px; font-weight: bold">
                {{ quotation.totalCharge }} {{ quotation.currency }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
          <el-alert :title="quotation.remarks" type="info" show-icon :closable="false" style="margin-top: 15px" />
          <el-button type="success" style="margin-top: 15px; width: 100%" @click="createShipment" :loading="creating">
            <el-icon><Check /></el-icon> 确认报价并创建运单
          </el-button>
        </el-card>
        <el-card v-else>
          <div style="text-align: center; padding: 40px; color: #8a9db0">
            <el-icon :size="60"><Search /></el-icon>
            <p style="margin-top: 15px">填写左侧货物信息，点击"获取报价"</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { shipmentApi } from '@/api'

const form = reactive({
  goodsName: '',
  goodsCategory: 'GENERAL',
  pieceCount: 1,
  grossWeight: 10,
  lengthCm: 50,
  widthCm: 40,
  heightCm: 30,
  originAirport: 'PVG',
  destAirport: 'JFK',
  incoterm: 'CIF',
  shipperName: '',
  consigneeName: '',
  currency: 'CNY'
})

const loading = ref(false)
const creating = ref(false)
const quotation = ref(null)

async function getQuotation() {
  loading.value = true
  try {
    const res = await shipmentApi.getQuotation(form)
    quotation.value = res.data
    ElMessage.success('报价生成成功！')
  } catch (e) {
    ElMessage.error('报价生成失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

async function createShipment() {
  creating.value = true
  try {
    const res = await shipmentApi.createShipment(form)
    ElMessage.success(`运单创建成功！运单号: ${res.data.shipmentNo}`)
    quotation.value = null
  } catch (e) {
    ElMessage.error('创建运单失败')
  } finally {
    creating.value = false
  }
}
</script>