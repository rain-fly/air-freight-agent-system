<template>
  <div>
    <div class="page-header">
      <div>
        <h2>LLM配置</h2>
        <div class="page-subtitle">配置大语言模型用于邮件分类，支持 OpenAI、Claude、Azure 和本地兼容接口。</div>
      </div>
      <div class="header-actions">
        <el-button @click="initDefaultRules">初始化默认规则</el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新增配置
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="llmConfigs" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="provider" label="服务商" width="130" />
        <el-table-column prop="model" label="模型" min-width="180" />
        <el-table-column prop="baseUrl" label="API地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="参数" width="150">
          <template #default="{ row }">
            T={{ row.temperature }} / {{ row.maxTokens }} tokens
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isActive" type="success" size="small">已激活</el-tag>
            <el-tag v-else type="info" size="small">未激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :loading="row.testing" @click="testLlmConnection(row)">测试</el-button>
            <el-button size="small" type="success" v-if="!row.isActive" @click="activateLlm(row)">激活</el-button>
            <el-button size="small" @click="editLlm(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteLlm(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        v-if="llmTestResult"
        :type="llmTestResult.success ? 'success' : 'error'"
        :title="llmTestResult.message"
        :closable="false"
        style="margin-top: 15px"
      >
        <div v-if="llmTestResult.response" style="margin-top: 5px; font-size: 12px; color: #606266">
          响应: {{ llmTestResult.response }}
        </div>
      </el-alert>
    </el-card>

    <el-dialog v-model="showLlmEditDialog" :title="editingLlm ? '编辑LLM配置' : '新增LLM配置'" width="560px">
      <el-form :model="llmForm" label-width="100px">
        <el-form-item label="配置名称">
          <el-input v-model="llmForm.name" placeholder="如：OpenAI GPT-4" />
        </el-form-item>
        <el-form-item label="服务商">
          <el-select v-model="llmForm.provider" style="width: 100%">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="Anthropic Claude" value="CLAUDE" />
            <el-option label="Azure OpenAI" value="AZURE" />
            <el-option label="本地模型(兼容OpenAI)" value="LOCAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="llmForm.baseUrl" placeholder="https://api.openai.com（本地模型可填http://localhost:11434）" />
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="llmForm.apiKey" type="password" show-password placeholder="API Key" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="llmForm.model" placeholder="gpt-3.5-turbo / gpt-4-turbo / claude-3-5-sonnet-..." />
        </el-form-item>
        <el-form-item label="API版本">
          <el-input v-model="llmForm.apiVersion" placeholder="Claude填2023-06-01，Azure填2024-02-15-preview" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="温度">
              <el-input-number v-model="llmForm.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大Token">
              <el-input-number v-model="llmForm.maxTokens" :min="100" :max="4000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="激活">
          <el-switch v-model="llmForm.isActive" />
          <el-text type="info" size="small" style="margin-left: 10px">激活后该配置用于AI分类</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLlmEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLlmConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classificationRuleApi, llmConfigApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const llmConfigs = ref([])
const llmTestResult = ref(null)
const showLlmEditDialog = ref(false)
const editingLlm = ref(null)

const llmForm = reactive({
  name: '',
  provider: 'OPENAI',
  apiKey: '',
  baseUrl: '',
  model: '',
  apiVersion: '',
  temperature: 0.3,
  maxTokens: 500,
  isActive: false
})

async function loadLlmConfigs() {
  loading.value = true
  try {
    const res = await llmConfigApi.getAll()
    llmConfigs.value = res.data || []
  } catch (e) {
    ElMessage.error('加载LLM配置失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingLlm.value = null
  resetForm()
  showLlmEditDialog.value = true
}

function editLlm(row) {
  editingLlm.value = row
  Object.assign(llmForm, {
    name: row.name,
    provider: row.provider,
    apiKey: row.apiKey,
    baseUrl: row.baseUrl,
    model: row.model,
    apiVersion: row.apiVersion,
    temperature: row.temperature,
    maxTokens: row.maxTokens,
    isActive: row.isActive
  })
  showLlmEditDialog.value = true
}

function resetForm() {
  Object.assign(llmForm, {
    name: '',
    provider: 'OPENAI',
    apiKey: '',
    baseUrl: '',
    model: '',
    apiVersion: '',
    temperature: 0.3,
    maxTokens: 500,
    isActive: false
  })
}

async function saveLlmConfig() {
  if (!llmForm.name || !llmForm.provider) {
    ElMessage.warning('请填写配置名称和服务商')
    return
  }
  saving.value = true
  try {
    if (editingLlm.value) {
      await llmConfigApi.update(editingLlm.value.id, { ...llmForm })
    } else {
      await llmConfigApi.create({ ...llmForm })
    }
    ElMessage.success('保存成功')
    showLlmEditDialog.value = false
    editingLlm.value = null
    await loadLlmConfigs()
    resetForm()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

async function activateLlm(row) {
  try {
    await llmConfigApi.activate(row.id)
    ElMessage.success(`已激活: ${row.name}`)
    await loadLlmConfigs()
  } catch (e) {
    ElMessage.error('激活失败：' + (e.response?.data?.message || e.message))
  }
}

async function testLlmConnection(row) {
  row.testing = true
  llmTestResult.value = null
  try {
    const res = await llmConfigApi.testConnection(row.id)
    llmTestResult.value = res.data
    if (res.data.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接测试失败')
    }
  } catch (e) {
    llmTestResult.value = { success: false, message: e.response?.data?.message || e.message }
    ElMessage.error('测试失败')
  } finally {
    row.testing = false
  }
}

async function deleteLlm(row) {
  try {
    await ElMessageBox.confirm(`确定删除配置 "${row.name}" 吗？`, '提示', { type: 'warning' })
    await llmConfigApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadLlmConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.response?.data?.message || e.message))
    }
  }
}

async function initDefaultRules() {
  try {
    const res = await classificationRuleApi.initDefault()
    ElMessage.success(res.data.message || '初始化完成')
  } catch (e) {
    ElMessage.error('初始化失败：' + (e.response?.data?.message || e.message))
  }
}

onMounted(() => {
  loadLlmConfigs()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 0;
}
.page-subtitle {
  margin-top: 6px;
  color: #8a9db0;
  font-size: 13px;
}
.header-actions {
  display: flex;
  gap: 10px;
}
</style>
