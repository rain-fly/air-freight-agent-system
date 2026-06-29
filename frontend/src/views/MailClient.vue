<template>
  <div>
    <h2 style="margin-top: 0">邮件客户端</h2>

    <el-row :gutter="20">
      <!-- 左侧：账号列表 -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">邮箱账号</span>
              <el-button type="primary" size="small" @click="showAccountDialog = true">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
          </template>
          <div v-if="accounts.length === 0" style="text-align: center; color: #8a9db0; padding: 20px">
            暂无邮箱账号
          </div>
          <div v-for="acc in accounts" :key="acc.id"
               :class="['account-item', { active: selectedAccount?.id === acc.id }]"
               @click="selectAccount(acc)">
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>
                <div style="font-weight: 600">{{ acc.accountName }}</div>
                <div style="font-size: 12px; color: #8a9db0">{{ acc.emailAddress }}</div>
              </div>
              <el-tag size="small" :type="acc.status === 'ACTIVE' ? 'success' : 'info'">
                {{ acc.status === 'ACTIVE' ? '正常' : '停用' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间：邮件列表 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">
                {{ selectedAccount ? selectedAccount.emailAddress : '请选择邮箱' }}
              </span>
              <div>
                <el-button size="small" @click="receiveMails" :disabled="!selectedAccount" :loading="receiving">
                  <el-icon><Download /></el-icon> 收信
                </el-button>
                <el-button type="primary" size="small" @click="showSendDialog = true" :disabled="!selectedAccount">
                  <el-icon><Edit /></el-icon> 写信
                </el-button>
              </div>
            </div>
          </template>
          <el-table :data="messages" style="width: 100%" @row-click="viewMail" highlight-current-row>
            <el-table-column width="30">
              <template #default="{ row }">
                <el-icon v-if="row.isStarred" color="#e6a23c"><StarFilled /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="fromAddress" label="发件人" width="150">
              <template #default="{ row }">
                <span :style="{ fontWeight: row.isRead ? 'normal' : 'bold' }">
                  {{ row.fromPersonal || row.fromAddress }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="subject" label="主题" min-width="200">
              <template #default="{ row }">
                <span :style="{ fontWeight: row.isRead ? 'normal' : 'bold' }">{{ row.subject || '(无主题)' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sentDate" label="时间" width="150" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：邮件内容 -->
      <el-col :span="8">
        <el-card v-if="selectedMail">
          <template #header>
            <div style="font-weight: 600">{{ selectedMail.subject || '(无主题)' }}</div>
          </template>
          <div style="margin-bottom: 15px; font-size: 13px; color: #606266">
            <div><strong>发件人：</strong>{{ selectedMail.fromPersonal || selectedMail.fromAddress }}</div>
            <div><strong>收件人：</strong>{{ selectedMail.toAddress }}</div>
            <div v-if="selectedMail.ccAddress"><strong>抄送：</strong>{{ selectedMail.ccAddress }}</div>
            <div><strong>时间：</strong>{{ selectedMail.sentDate }}</div>
          </div>
          <el-divider />
          <div style="white-space: pre-wrap; font-size: 14px; line-height: 1.6">
            {{ selectedMail.content }}
          </div>
        </el-card>
        <el-card v-else>
          <div style="text-align: center; color: #8a9db0; padding: 40px">
            <el-icon :size="50"><Message /></el-icon>
            <p style="margin-top: 10px">选择一封邮件查看详情</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加账号对话框 -->
    <el-dialog v-model="showAccountDialog" title="添加邮箱账号" width="600px">
      <el-form :model="accountForm" label-width="120px">
        <el-form-item label="账号别名">
          <el-input v-model="accountForm.accountName" placeholder="如：我的QQ邮箱" />
        </el-form-item>
        <el-form-item label="邮箱地址">
          <el-input v-model="accountForm.emailAddress" placeholder="example@qq.com" />
        </el-form-item>
        <el-form-item label="密码/授权码">
          <el-input v-model="accountForm.password" type="password" show-password
                    placeholder="QQ邮箱需使用授权码" />
        </el-form-item>
        <el-divider>POP3 配置（收信）</el-divider>
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
      <template #footer>
        <el-button @click="showAccountDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>

    <!-- 写信对话框 -->
    <el-dialog v-model="showSendDialog" title="写邮件" width="600px">
      <el-form :model="sendForm" label-width="80px">
        <el-form-item label="收件人">
          <el-input v-model="sendForm.to" placeholder="收件人邮箱地址" />
        </el-form-item>
        <el-form-item label="抄送">
          <el-input v-model="sendForm.cc" placeholder="抄送邮箱地址（可选）" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="sendForm.subject" placeholder="邮件主题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="sendForm.priority">
            <el-radio label="LOW">低</el-radio>
            <el-radio label="NORMAL">普通</el-radio>
            <el-radio label="HIGH">高</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="sendForm.content" type="textarea" :rows="8" placeholder="请输入邮件正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSendDialog = false">取消</el-button>
        <el-button type="primary" @click="sendMail" :loading="sending">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const accounts = ref([])
const selectedAccount = ref(null)
const messages = ref([])
const selectedMail = ref(null)
const receiving = ref(false)
const sending = ref(false)
const showAccountDialog = ref(false)
const showSendDialog = ref(false)

const accountForm = reactive({
  accountName: '', emailAddress: '', password: '',
  pop3Host: 'pop.qq.com', pop3Port: 995, pop3Ssl: true,
  smtpHost: 'smtp.qq.com', smtpPort: 465, smtpSsl: true
})

const sendForm = reactive({
  to: '', cc: '', subject: '', content: '', priority: 'NORMAL'
})

async function loadAccounts() {
  try {
    const res = await api.get('/mail/accounts')
    accounts.value = res.data
  } catch (e) { /* 首次为空 */ }
}

function selectAccount(acc) {
  selectedAccount.value = acc
  loadMails(acc.id)
}

async function loadMails(accountId) {
  try {
    const res = await api.get(`/mail/messages/${accountId}`)
    messages.value = res.data
  } catch (e) {
    messages.value = []
  }
}

async function receiveMails() {
  if (!selectedAccount.value) return
  receiving.value = true
  try {
    await api.post(`/mail/receive/${selectedAccount.value.id}`)
    ElMessage.success('收信成功')
    await loadMails(selectedAccount.value.id)
  } catch (e) {
    ElMessage.error('收信失败：' + (e.response?.data?.message || e.message))
  } finally {
    receiving.value = false
  }
}

function viewMail(row) {
  selectedMail.value = row
}

async function saveAccount() {
  try {
    await api.post('/mail/accounts', accountForm)
    ElMessage.success('邮箱账号添加成功')
    showAccountDialog.value = false
    await loadAccounts()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function sendMail() {
  if (!selectedAccount.value) return
  sending.value = true
  try {
    const params = new URLSearchParams({
      to: sendForm.to,
      cc: sendForm.cc,
      subject: sendForm.subject,
      content: sendForm.content,
      priority: sendForm.priority
    })
    await api.post(`/mail/send/${selectedAccount.value.id}?${params}`)
    ElMessage.success('邮件发送成功')
    showSendDialog.value = false
    sendForm.to = ''; sendForm.cc = ''; sendForm.subject = ''; sendForm.content = ''
  } catch (e) {
    ElMessage.error('发送失败：' + (e.response?.data?.message || e.message))
  } finally {
    sending.value = false
  }
}

onMounted(loadAccounts)
</script>

<style scoped>
.account-item {
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 5px;
  border: 1px solid transparent;
}
.account-item:hover {
  background: #f5f7fa;
}
.account-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}
</style>