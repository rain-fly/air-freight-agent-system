<template>
  <div>
    <div class="page-header">
      <div>
        <h2>邮箱配置</h2>
        <div class="page-subtitle">管理 POP3/SMTP 账号，测试连接和真实发信能力。</div>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新增邮箱
      </el-button>
    </div>

    <el-card>
      <el-table :data="accounts" style="width: 100%" v-loading="loading">
        <el-table-column prop="accountName" label="账号别名" min-width="140" />
        <el-table-column prop="emailAddress" label="邮箱地址" min-width="200" />
        <el-table-column label="收信" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" style="margin-right: 4px">{{ row.receiveProtocol || 'IMAP' }}</el-tag>
            <template v-if="(row.receiveProtocol || 'IMAP') === 'IMAP'">
              {{ row.imapHost }}:{{ row.imapPort }}
              <el-tag size="small" type="info" style="margin-left: 6px">{{ row.imapSsl ? 'SSL' : '非SSL' }}</el-tag>
            </template>
            <template v-else>
              {{ row.pop3Host }}:{{ row.pop3Port }}
              <el-tag size="small" type="info" style="margin-left: 6px">{{ row.pop3Ssl ? 'SSL' : '非SSL' }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="SMTP" min-width="180">
          <template #default="{ row }">
            {{ row.smtpHost }}:{{ row.smtpPort }}
            <el-tag size="small" type="info" style="margin-left: 6px">{{ row.smtpSsl ? 'SSL' : '非SSL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCheckAt" label="上次检查" width="170" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" :loading="row.testingConnection" @click="testSavedConnection(row)">
              测试连接
            </el-button>
            <el-button size="small" type="success" :loading="row.testingSend" @click="testSavedSend(row)">
              测试发信
            </el-button>
            <el-button size="small" type="danger" @click="deleteAccount(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showAccountDialog" :title="editingAccount ? '编辑邮箱账号' : '新增邮箱账号'" width="640px">
      <el-form :model="accountForm" label-width="120px">
        <el-form-item label="账号别名">
          <el-input v-model="accountForm.accountName" placeholder="如：业务QQ邮箱" />
        </el-form-item>
        <el-form-item label="邮箱地址">
          <el-input v-model="accountForm.emailAddress" placeholder="example@qq.com" />
        </el-form-item>
        <el-form-item label="密码/授权码">
          <el-input
            v-model="accountForm.password"
            type="password"
            show-password
            placeholder="QQ邮箱需使用授权码"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="accountForm.status" style="width: 100%">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>

        <el-divider>收信配置</el-divider>
        <el-form-item label="收信协议">
          <el-radio-group v-model="accountForm.receiveProtocol">
            <el-radio label="IMAP">IMAP（推荐）</el-radio>
            <el-radio label="POP3">POP3</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="accountForm.receiveProtocol === 'IMAP'">
          <el-form-item label="IMAP服务器">
            <el-input v-model="accountForm.imapHost" placeholder="imap.example.com" />
          </el-form-item>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item label="端口">
                <el-input-number v-model="accountForm.imapPort" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SSL">
                <el-switch v-model="accountForm.imapSsl" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <template v-else>
          <el-form-item label="POP3服务器">
            <el-input v-model="accountForm.pop3Host" placeholder="pop.qq.com" />
          </el-form-item>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item label="端口">
                <el-input-number v-model="accountForm.pop3Port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SSL">
                <el-switch v-model="accountForm.pop3Ssl" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-divider>SMTP 配置（发信）</el-divider>
        <el-form-item label="SMTP服务器">
          <el-input v-model="accountForm.smtpHost" placeholder="smtp.qq.com" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="端口">
              <el-input-number v-model="accountForm.smtpPort" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SSL">
              <el-switch v-model="accountForm.smtpSsl" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-alert
        v-if="testResult"
        :type="testResult.valid || testResult.success ? 'success' : 'error'"
        :closable="false"
        :title="testResult.message"
        style="margin-top: 18px"
      >
        <div v-if="testResult.successes && testResult.successes.length > 0">
          <div v-for="success in testResult.successes" :key="success" style="margin-top: 5px">
            <el-icon color="#67c23a"><Check /></el-icon> {{ success }}
          </div>
        </div>
        <div v-if="testResult.errors && testResult.errors.length > 0">
          <div v-for="error in testResult.errors" :key="error" style="margin-top: 5px">
            <el-icon color="#f56c6c"><Close /></el-icon> {{ error }}
          </div>
        </div>
      </el-alert>

      <template #footer>
        <el-button @click="showAccountDialog = false">取消</el-button>
        <el-button :loading="testingConnection" @click="testFormConnection">测试连接</el-button>
        <el-button type="success" :loading="testingSend" @click="testFormSend">测试发信</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mailAccountApi } from '@/api'

const accounts = ref([])
const loading = ref(false)
const saving = ref(false)
const testingConnection = ref(false)
const testingSend = ref(false)
const showAccountDialog = ref(false)
const editingAccount = ref(null)
const testResult = ref(null)

const accountForm = reactive({
  id: null,
  accountName: '',
  emailAddress: '',
  password: '',
  status: 'ACTIVE',
  receiveProtocol: 'IMAP',
  imapHost: '',
  imapPort: 993,
  imapSsl: true,
  pop3Host: '',
  pop3Port: 995,
  pop3Ssl: true,
  smtpHost: '',
  smtpPort: 465,
  smtpSsl: true
})

async function loadAccounts() {
  loading.value = true
  try {
    const res = await mailAccountApi.getAll()
    accounts.value = res.data || []
  } catch (e) {
    accounts.value = []
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingAccount.value = null
  testResult.value = null
  resetForm()
  showAccountDialog.value = true
}

function openEditDialog(row) {
  editingAccount.value = row
  testResult.value = null
  Object.assign(accountForm, {
    id: row.id,
    accountName: row.accountName,
    emailAddress: row.emailAddress,
    password: row.password,
    status: row.status || 'ACTIVE',
    receiveProtocol: row.receiveProtocol || 'IMAP',
    imapHost: row.imapHost || '',
    imapPort: row.imapPort || 993,
    imapSsl: row.imapSsl !== false,
    pop3Host: row.pop3Host || '',
    pop3Port: row.pop3Port || 995,
    pop3Ssl: row.pop3Ssl !== false,
    smtpHost: row.smtpHost,
    smtpPort: row.smtpPort,
    smtpSsl: row.smtpSsl
  })
  showAccountDialog.value = true
}

function resetForm() {
  Object.assign(accountForm, {
    id: null,
    accountName: '',
    emailAddress: '',
    password: '',
    status: 'ACTIVE',
    receiveProtocol: 'IMAP',
    imapHost: '',
    imapPort: 993,
    imapSsl: true,
    pop3Host: '',
    pop3Port: 995,
    pop3Ssl: true,
    smtpHost: '',
    smtpPort: 465,
    smtpSsl: true
  })
}

function validateForm() {
  if (!accountForm.accountName || !accountForm.emailAddress || !accountForm.password) {
    ElMessage.warning('请填写账号别名、邮箱地址和密码/授权码')
    return false
  }
  if (accountForm.receiveProtocol === 'IMAP') {
    if (!accountForm.imapHost) {
      ElMessage.warning('请填写 IMAP 服务器地址')
      return false
    }
  } else {
    if (!accountForm.pop3Host) {
      ElMessage.warning('请填写 POP3 服务器地址')
      return false
    }
  }
  if (!accountForm.smtpHost) {
    ElMessage.warning('请填写 SMTP 服务器地址')
    return false
  }
  return true
}

async function saveAccount() {
  if (!validateForm()) return
  saving.value = true
  try {
    const payload = { ...accountForm }
    if (editingAccount.value) {
      await mailAccountApi.update(editingAccount.value.id, payload)
      ElMessage.success('邮箱账号已更新')
    } else {
      delete payload.id
      await mailAccountApi.create(payload)
      ElMessage.success('邮箱账号已新增')
    }
    showAccountDialog.value = false
    await loadAccounts()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

async function testFormConnection() {
  if (!validateForm()) return
  testingConnection.value = true
  testResult.value = null
  try {
    const res = await mailAccountApi.testConnection({ ...accountForm })
    testResult.value = res.data
    if (res.data.valid) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.warning('连接测试失败，请检查配置')
    }
  } catch (e) {
    ElMessage.error('测试失败：' + (e.response?.data?.message || e.message))
  } finally {
    testingConnection.value = false
  }
}

async function testSavedConnection(row) {
  row.testingConnection = true
  try {
    const res = await mailAccountApi.testSavedConnection(row.id)
    if (res.data.valid) {
      ElMessage.success(`${row.accountName} 连接测试成功`)
    } else {
      ElMessage.warning(res.data.message || '连接测试失败，请检查配置')
    }
  } catch (e) {
    ElMessage.error('测试失败：' + (e.response?.data?.message || e.message))
  } finally {
    row.testingConnection = false
  }
}

async function promptRecipient(defaultEmail) {
  const result = await ElMessageBox.prompt('请输入测试邮件收件人', '测试发信', {
    confirmButtonText: '发送测试邮件',
    cancelButtonText: '取消',
    inputValue: defaultEmail || '',
    inputPattern: /.+@.+\..+/,
    inputErrorMessage: '请输入有效的邮箱地址'
  })
  return result.value
}

async function testFormSend() {
  if (!validateForm()) return
  testingSend.value = true
  testResult.value = null
  try {
    const to = await promptRecipient(accountForm.emailAddress)
    const res = await mailAccountApi.testSend({ ...accountForm }, to)
    testResult.value = res.data
    if (res.data.success) {
      ElMessage.success(res.data.message || '测试邮件已发送')
    } else {
      ElMessage.error(res.data.message || '测试发信失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('测试发信失败：' + (e.response?.data?.message || e.message))
    }
  } finally {
    testingSend.value = false
  }
}

async function testSavedSend(row) {
  row.testingSend = true
  try {
    const to = await promptRecipient(row.emailAddress)
    const res = await mailAccountApi.testSavedSend(row.id, to)
    if (res.data.success) {
      ElMessage.success(res.data.message || '测试邮件已发送')
    } else {
      ElMessage.error(res.data.message || '测试发信失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('测试发信失败：' + (e.response?.data?.message || e.message))
    }
  } finally {
    row.testingSend = false
  }
}

async function deleteAccount(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除邮箱账号 "${row.accountName}" 吗？该账号在本系统内已同步的邮件记录也会被删除。`,
      '删除邮箱账号',
      { type: 'warning' }
    )
    await mailAccountApi.delete(row.id)
    ElMessage.success('邮箱账号已删除')
    await loadAccounts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.response?.data?.message || e.message))
    }
  }
}

onMounted(() => {
  loadAccounts()
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
</style>
