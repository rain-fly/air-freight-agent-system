<template>
  <div>
    <h2 style="margin-top: 0">客户管理</h2>
    <el-button type="primary" style="margin-bottom: 15px" @click="dialogVisible = true">
      <el-icon><Plus /></el-icon> 新增客户
    </el-button>

    <el-table :data="customers" border stripe style="width: 100%">
      <el-table-column prop="name" label="客户名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type === 'SHIPPER' ? '发货人' : row.type === 'CONSIGNEE' ? '收货人' : '代理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contactPerson" label="联系人" width="120" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="country" label="国家" width="100" />
      <el-table-column prop="swiftCode" label="SWIFT代码" width="130" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="editCustomer(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑客户对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="客户名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="发货人" value="SHIPPER" />
            <el-option label="收货人" value="CONSIGNEE" />
            <el-option label="代理" value="AGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="国家">
          <el-input v-model="form.country" placeholder="如 China / USA / Germany" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxId" />
        </el-form-item>
        <el-form-item label="SWIFT代码">
          <el-input v-model="form.swiftCode" placeholder="用于国际转账" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomer">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi } from '@/api'

const customers = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  name: '', type: 'SHIPPER', contactPerson: '', phone: '', email: '',
  country: '', address: '', taxId: '', swiftCode: '', bankAccount: ''
})

function resetForm() {
  Object.assign(form, { name: '', type: 'SHIPPER', contactPerson: '', phone: '', email: '', country: '', address: '', taxId: '', swiftCode: '', bankAccount: '' })
  isEdit.value = false
  editId.value = null
}

function editCustomer(row) {
  Object.assign(form, row)
  isEdit.value = true
  editId.value = row.id
  dialogVisible.value = true
}

async function saveCustomer() {
  try {
    if (isEdit.value) {
      await customerApi.update(editId.value, form)
      ElMessage.success('客户更新成功')
    } else {
      await customerApi.create(form)
      ElMessage.success('客户创建成功')
    }
    dialogVisible.value = false
    resetForm()
    const res = await customerApi.getAll()
    customers.value = res.data
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(async () => {
  try {
    const res = await customerApi.getAll()
    customers.value = res.data
  } catch (e) {
    console.log('加载客户数据（首次运行为空）')
  }
})
</script>