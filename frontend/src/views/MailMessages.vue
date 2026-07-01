<template>
  <div class="mail-app">
    <!-- 顶部工具栏 -->
    <header class="mail-toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedAccountId"
          placeholder="选择邮箱账号"
          filterable
          class="account-select"
        >
          <el-option
            v-for="account in accounts"
            :key="account.id"
            :label="`${account.accountName}（${account.emailAddress}）`"
            :value="account.id"
          />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :disabled="!selectedAccount" :loading="receiving" @click="syncMails(true)" class="toolbar-btn">
          <el-icon><Refresh /></el-icon> 同步
        </el-button>
        <el-checkbox
          v-model="autoRefresh"
          :disabled="!selectedAccount"
          @change="handleAutoRefreshChange"
          class="auto-refresh-check"
        >
          每3分钟自动刷新
        </el-checkbox>
        <el-button type="primary" :disabled="!selectedAccount" @click="showSendDialog = true" class="toolbar-btn">
          <el-icon><Edit /></el-icon> 写信
        </el-button>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="mail-body">
      <!-- 左侧边栏 -->
      <aside class="mail-sidebar">
        <!-- 账号信息卡片 -->
        <div class="account-card" v-if="selectedAccount">
          <div class="account-avatar-large">
            {{ getInitial(selectedAccount.emailAddress) }}
          </div>
          <div class="account-info">
            <div class="account-name">{{ selectedAccount.accountName }}</div>
            <div class="account-addr">{{ selectedAccount.emailAddress }}</div>
          </div>
          <el-tag :type="selectedAccount.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ selectedAccount.status === 'ACTIVE' ? '正常' : '停用' }}
          </el-tag>
        </div>
        <div v-else class="account-card account-card-empty">
          <el-empty description="未选择账号" :image-size="48" />
        </div>

        <!-- 文件夹导航 -->
        <nav class="folder-nav">
          <div class="nav-title">文件夹</div>
          <div
            class="nav-item"
            :class="{ active: currentFolder === 'inbox' }"
            @click="currentFolder = 'inbox'"
          >
            <el-icon><Message /></el-icon>
            <span>收件箱</span>
            <span class="nav-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          </div>
          <div
            class="nav-item"
            :class="{ active: currentFolder === 'sent' }"
            @click="currentFolder = 'sent'"
          >
            <el-icon><Promotion /></el-icon>
            <span>已发送</span>
          </div>
          <div
            class="nav-item"
            :class="{ active: currentFolder === 'starred' }"
            @click="currentFolder = 'starred'"
          >
            <el-icon><Star /></el-icon>
            <span>星标邮件</span>
          </div>
        </nav>

        <!-- 分类筛选 -->
        <nav class="folder-nav">
          <div class="nav-title">分类</div>
          <div
            v-for="cat in categoryOptions"
            :key="cat"
            class="nav-item"
            :class="{ active: activeCategory === cat }"
            @click="activeCategory = activeCategory === cat ? '' : cat"
          >
            <span
              class="category-dot"
              :style="{ background: getCategoryColor(cat) }"
            ></span>
            <span>{{ cat }}</span>
            <span class="nav-badge">{{ getCategoryCount(cat) }}</span>
          </div>
        </nav>

        <!-- 底部状态 -->
        <div class="sidebar-footer">
          <div class="sync-status">
            <el-icon :class="{ spinning: receiving }"><Refresh /></el-icon>
            {{ receiving ? '同步中...' : (lastSyncedAt ? '已同步 ' + lastSyncedAt : '未同步') }}
          </div>
        </div>
      </aside>

      <!-- 中间邮件列表 -->
      <section class="mail-list-panel">
        <!-- 列表工具栏 -->
        <div class="list-toolbar">
          <div class="list-toolbar-left">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索邮件..."
              :prefix-icon="Search"
              clearable
              size="small"
              class="search-input"
              @input="onSearch"
            />
          </div>
          <div class="list-toolbar-right">
            <el-button
              type="warning"
              size="small"
              :disabled="!selectedAccount || messages.length === 0"
              :loading="classifying"
              @click="showBatchClassifyDialog = true"
            >
              <el-icon><Files /></el-icon> 批量分类
            </el-button>
          </div>
        </div>

        <!-- 选中提示条 -->
        <transition name="slide-down">
          <div v-if="selectedMailIds.length > 0" class="selection-bar">
            <span>{{ selectedMailIds.length }} 封已选</span>
            <div>
              <el-button size="small" type="primary" :loading="classifying" @click="batchClassifySelected">
                分类选中
              </el-button>
              <el-button size="small" @click="selectedMailIds = []">取消</el-button>
            </div>
          </div>
        </transition>

        <!-- 邮件列表 -->
        <div class="mail-list" ref="mailListRef">
          <div
            v-for="mail in filteredMessages"
            :key="mail.id"
            class="mail-item"
            :class="{
              'mail-item-active': selectedMail?.id === mail.id,
              'mail-item-unread': !mail.isRead
            }"
            @click="viewMail(mail)"
          >
            <div class="mail-item-check" @click.stop>
              <el-checkbox
                :model-value="selectedMailIds.includes(mail.id)"
                @change="(val) => toggleMailSelect(mail.id, val)"
              />
            </div>
            <div class="mail-item-star" @click.stop>
              <el-icon
                :color="mail.isStarred ? '#e6a23c' : '#dcdfe6'"
                :size="16"
                class="star-icon"
              >
                <StarFilled v-if="mail.isStarred" />
                <Star v-else />
              </el-icon>
            </div>
            <div class="mail-item-avatar">
              <div
                class="sender-avatar"
                :style="{ background: getAvatarColor(mail.fromAddress) }"
              >
                {{ getInitial(mail.fromPersonal || mail.fromAddress || '?') }}
              </div>
            </div>
            <div class="mail-item-body">
              <div class="mail-item-top">
                <span class="mail-item-sender">{{ mail.fromPersonal || mail.fromAddress }}</span>
                <span class="mail-item-time">{{ formatTime(mail.sentDate) }}</span>
              </div>
              <div class="mail-item-subject">
                {{ mail.subject || '(无主题)' }}
              </div>
              <div class="mail-item-preview">
                {{ getPreview(mail.content) }}
              </div>
            </div>
            <div class="mail-item-meta">
              <el-tag
                v-if="mail.category"
                :color="getCategoryBgColor(mail.category)"
                size="small"
                class="category-chip"
              >
                {{ mail.category }}
              </el-tag>
              <el-icon v-if="mail.hasAttachments" class="attach-icon"><Paperclip /></el-icon>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="filteredMessages.length === 0 && !receiving" class="mail-list-empty">
            <el-icon :size="48" color="#c0c4cc"><Message /></el-icon>
            <p>{{ searchKeyword ? '没有匹配的邮件' : '暂无邮件' }}</p>
            <el-button v-if="!searchKeyword && selectedAccount" type="primary" text @click="syncMails(true)">
              立即同步
            </el-button>
          </div>
        </div>
      </section>

      <!-- 右侧邮件详情 -->
      <section class="mail-detail-panel" v-if="selectedMail">
        <!-- 详情工具栏 -->
        <div class="detail-toolbar">
          <div class="detail-toolbar-left">
            <el-button size="small" circle @click="selectedMail = null" title="返回">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
          </div>
          <div class="detail-toolbar-right">
            <el-button size="small" text @click="classifySingleMail" :loading="classifying">
              <el-icon><Cpu /></el-icon> AI分类
            </el-button>
            <el-button size="small" text @click="showManualClassifyDialog = true">
              <el-icon><EditPen /></el-icon> 手动分类
            </el-button>
          </div>
        </div>

        <!-- 邮件内容 -->
        <div class="detail-content">
          <!-- 邮件标题区 -->
          <div class="detail-header">
            <h2 class="detail-subject">{{ selectedMail.subject || '(无主题)' }}</h2>
            <div class="detail-tags">
              <el-tag
                v-if="selectedMail.category"
                :color="getCategoryBgColor(selectedMail.category)"
                size="small"
                effect="dark"
              >
                {{ selectedMail.category }}
              </el-tag>
              <el-tag
                v-if="selectedMail.priority === 'HIGH'"
                type="danger"
                size="small"
                effect="dark"
              >
                紧急
              </el-tag>
              <el-tag
                v-for="tag in (selectedMail.tags || '').split(',').filter(Boolean)"
                :key="tag"
                size="small"
                type="info"
                effect="plain"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>

          <!-- 发件人信息卡片 -->
          <div class="sender-card">
            <div
              class="sender-avatar-large"
              :style="{ background: getAvatarColor(selectedMail.fromAddress) }"
            >
              {{ getInitial(selectedMail.fromPersonal || selectedMail.fromAddress || '?') }}
            </div>
            <div class="sender-info">
              <div class="sender-name">
                {{ selectedMail.fromPersonal || selectedMail.fromAddress }}
                <span class="sender-email">&lt;{{ selectedMail.fromAddress }}&gt;</span>
              </div>
              <div class="sender-meta">
                <span>收件人：{{ selectedMail.toAddress }}</span>
                <span v-if="selectedMail.ccAddress">抄送：{{ selectedMail.ccAddress }}</span>
              </div>
              <div class="sender-time">{{ selectedMail.sentDate }}</div>
            </div>
          </div>

          <!-- 附件 -->
          <div v-if="selectedMail.hasAttachments && selectedMail.attachmentNames" class="attachment-section">
            <div class="section-label">
              <el-icon><Paperclip /></el-icon> 附件（{{ selectedMail.attachmentNames.split(',').length }}）
            </div>
            <div class="attachment-list">
              <div
                v-for="(name, idx) in selectedMail.attachmentNames.split(',').map(s => s.trim())"
                :key="idx"
                class="attachment-item"
              >
                <div class="attachment-icon">
                  <el-icon :size="24"><Document /></el-icon>
                </div>
                <div class="attachment-name">{{ name }}</div>
                <div class="attachment-size">{{ getFileIcon(name) }}</div>
              </div>
            </div>
          </div>

          <!-- 分类信息折叠区 -->
          <el-collapse v-if="selectedMail.classificationMethod" class="classify-collapse">
            <el-collapse-item title="分类详情">
              <div class="classify-detail">
                <div class="classify-row">
                  <span class="classify-label">分类方法</span>
                  <el-tag size="small" type="info" effect="plain">
                    {{ getMethodText(selectedMail.classificationMethod) }}
                  </el-tag>
                </div>
                <div class="classify-row" v-if="selectedMail.classificationConfidence">
                  <span class="classify-label">置信度</span>
                  <el-progress
                    :percentage="Math.round(selectedMail.classificationConfidence * 100)"
                    :stroke-width="8"
                    :color="getConfidenceColor(selectedMail.classificationConfidence)"
                    style="width: 200px"
                  />
                </div>
                <div class="classify-row" v-if="selectedMail.classificationReason">
                  <span class="classify-label">推理过程</span>
                  <span class="classify-value">{{ selectedMail.classificationReason }}</span>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <!-- 邮件正文 -->
          <div class="mail-content">
            <div
              v-if="selectedMail.contentHtml"
              class="mail-content-html"
              v-html="sanitizeHtml(selectedMail.contentHtml)"
            ></div>
            <div v-else class="mail-content-text">{{ selectedMail.content }}</div>
          </div>
        </div>
      </section>

      <!-- 无选中邮件的占位 -->
      <section class="mail-detail-panel mail-detail-empty" v-else>
        <div class="empty-state">
          <div class="empty-icon">
            <el-icon :size="64" color="#dcdfe6"><Message /></el-icon>
          </div>
          <h3>选择一封邮件</h3>
          <p>从左侧列表中选择一封邮件查看详情</p>
        </div>
      </section>
    </div>

    <!-- ========== 弹窗区域 ========== -->

    <!-- 写邮件 -->
    <el-dialog v-model="showSendDialog" title="写邮件" width="720px" class="compose-dialog" :close-on-click-modal="false">
      <el-form :model="sendForm" label-width="70px" class="compose-form">
        <el-form-item label="发件人">
          <div class="compose-from">{{ selectedAccount?.emailAddress || '' }}</div>
        </el-form-item>
        <el-form-item label="收件人" required>
          <el-input v-model="sendForm.to" placeholder="输入收件人邮箱，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="抄送">
          <el-input v-model="sendForm.cc" placeholder="抄送邮箱（可选）" />
        </el-form-item>
        <el-form-item label="主题" required>
          <el-input v-model="sendForm.subject" placeholder="邮件主题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="sendForm.priority" size="small">
            <el-radio-button label="LOW">低</el-radio-button>
            <el-radio-button label="NORMAL">普通</el-radio-button>
            <el-radio-button label="HIGH">紧急</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="sendForm.content" type="textarea" :rows="12" placeholder="请输入邮件正文..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSendDialog = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="sendMail">
          <el-icon><Promotion /></el-icon> 发送
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量分类 -->
    <el-dialog v-model="showBatchClassifyDialog" title="批量邮件分类" width="500px">
      <el-form :model="classifyForm" label-width="90px">
        <el-form-item label="分类范围">
          <el-radio-group v-model="classifyForm.scope">
            <el-radio label="UNCLASSIFIED">仅未分类</el-radio>
            <el-radio label="SELECTED">已选邮件</el-radio>
            <el-radio label="ALL">全部邮件</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类方法">
          <el-radio-group v-model="classifyForm.method">
            <el-radio label="RULE">规则分类</el-radio>
            <el-radio label="LLM">AI智能分类</el-radio>
            <el-radio label="HYBRID">混合模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="classifyForm.method === 'LLM' || classifyForm.method === 'HYBRID'"
          :type="activeLlmConfig ? 'success' : 'warning'"
          :closable="false"
          class="classify-alert"
        >
          <template #title>
            {{ activeLlmConfig ? `当前激活LLM: ${activeLlmConfig.name} (${activeLlmConfig.model})` : '未激活LLM配置，LLM分类将回退到规则分类' }}
          </template>
        </el-alert>
        <el-form-item label="预计处理">
          <el-text type="info">{{ classifyTargetText }}</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchClassifyDialog = false">取消</el-button>
        <el-button type="primary" :loading="classifying" @click="batchClassifyMails">开始分类</el-button>
      </template>
    </el-dialog>

    <!-- 手动分类 -->
    <el-dialog v-model="showManualClassifyDialog" title="手动调整分类" width="450px">
      <el-form :model="manualClassifyForm" label-width="70px">
        <el-form-item label="分类">
          <el-select v-model="manualClassifyForm.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat">
              <span class="category-dot" :style="{ background: getCategoryColor(cat) }"></span>
              {{ cat }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="manualClassifyForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showManualClassifyDialog = false">取消</el-button>
        <el-button type="primary" @click="saveManualClassify">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { llmConfigApi, mailAccountApi, mailApi, mailClassifyApi } from '@/api'

const AUTO_REFRESH_INTERVAL = 180000

const accounts = ref([])
const selectedAccountId = ref(null)
const messages = ref([])
const selectedMail = ref(null)
const selectedMailIds = ref([])
const activeLlmConfig = ref(null)
const autoRefresh = ref(false)
const refreshTimer = ref(null)
const lastSyncedAt = ref('')
const searchKeyword = ref('')
const currentFolder = ref('inbox')
const activeCategory = ref('')

const receiving = ref(false)
const sending = ref(false)
const classifying = ref(false)
const showSendDialog = ref(false)
const showBatchClassifyDialog = ref(false)
const showManualClassifyDialog = ref(false)

const sendForm = reactive({
  to: '', cc: '', subject: '', content: '', priority: 'NORMAL'
})

const classifyForm = reactive({
  scope: 'UNCLASSIFIED', method: 'HYBRID'
})

const manualClassifyForm = reactive({
  category: '', tags: ''
})

const categoryOptions = ['工作', '商务', '财务', '通知', '社交', '营销', '垃圾邮件', '其他']

const selectedAccount = computed(() =>
  accounts.value.find(a => a.id === selectedAccountId.value) || null
)

const unreadCount = computed(() =>
  messages.value.filter(m => !m.isRead).length
)

const unclassifiedCount = computed(() =>
  messages.value.filter(m => !m.isClassified).length
)

const classifyTargetText = computed(() => {
  if (classifyForm.scope === 'UNCLASSIFIED') return `${unclassifiedCount.value} 封未分类邮件`
  if (classifyForm.scope === 'SELECTED') return `${selectedMailIds.value.length} 封已选邮件`
  return `${messages.value.length} 封全部邮件`
})

const filteredMessages = computed(() => {
  let list = messages.value
  if (currentFolder.value === 'sent') {
    list = list.filter(m => m.direction === 'SENT')
  } else if (currentFolder.value === 'starred') {
    list = list.filter(m => m.isStarred)
  }
  if (activeCategory.value) {
    list = list.filter(m => m.category === activeCategory.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(m =>
      (m.subject || '').toLowerCase().includes(kw) ||
      (m.fromAddress || '').toLowerCase().includes(kw) ||
      (m.fromPersonal || '').toLowerCase().includes(kw) ||
      (m.content || '').toLowerCase().includes(kw)
    )
  }
  return list
})

watch(selectedAccountId, async id => {
  selectedMail.value = null
  selectedMailIds.value = []
  messages.value = []
  searchKeyword.value = ''
  currentFolder.value = 'inbox'
  activeCategory.value = ''
  if (id) await loadMails(id)
  restartAutoRefresh()
})

async function loadAccounts() {
  try {
    const res = await mailAccountApi.getAll()
    accounts.value = res.data || []
    if (!selectedAccountId.value && accounts.value.length > 0) {
      selectedAccountId.value = accounts.value[0].id
    }
  } catch (e) { accounts.value = [] }
}

async function loadActiveLlmConfig() {
  try {
    const res = await llmConfigApi.getActive()
    activeLlmConfig.value = res.data
  } catch (e) { activeLlmConfig.value = null }
}

async function loadMails(accountId) {
  try {
    const res = await mailApi.getMessages(accountId)
    messages.value = res.data || []
    if (selectedMail.value) {
      selectedMail.value = messages.value.find(m => m.id === selectedMail.value.id) || null
    }
  } catch (e) { messages.value = [] }
}

async function syncMails(showMessage = true) {
  if (!selectedAccountId.value || receiving.value) return
  receiving.value = true
  try {
    const res = await mailApi.receive(selectedAccountId.value)
    if (res.data?.status === 'processing') {
      if (showMessage) ElMessage.info('开始后台同步邮件，请稍候...')
      await pollSyncResult(showMessage)
    } else {
      await loadMails(selectedAccountId.value)
      await loadAccounts()
      lastSyncedAt.value = new Date().toLocaleString()
      if (showMessage && res.data?.length !== undefined) {
        ElMessage.success(`同步完成，新增 ${res.data.length} 封邮件`)
      }
    }
  } catch (e) {
    if (showMessage) ElMessage.error('同步失败：' + (e.response?.data?.message || e.message))
  } finally { receiving.value = false }
}

async function pollSyncResult(showMessage) {
  const POLL_INTERVAL = 2000, MAX_POLLS = 30
  let previousCount = 0, stableCount = 0
  for (let i = 0; i < MAX_POLLS; i++) {
    await new Promise(r => setTimeout(r, POLL_INTERVAL))
    try {
      const res = await mailApi.getMessages(selectedAccountId.value)
      const currentMessages = res.data || []
      const currentCount = currentMessages.length
      if (currentCount === previousCount) {
        stableCount++
        if (stableCount >= 2) {
          messages.value = currentMessages
          await loadAccounts()
          lastSyncedAt.value = new Date().toLocaleString()
          if (showMessage) ElMessage.success('邮件同步完成')
          return
        }
      } else {
        stableCount = 0; previousCount = currentCount
        messages.value = currentMessages
      }
    } catch (e) {}
  }
  await loadMails(selectedAccountId.value)
  await loadAccounts()
  lastSyncedAt.value = new Date().toLocaleString()
  if (showMessage) ElMessage.warning('同步可能仍在进行中，请手动刷新')
}

function handleAutoRefreshChange(enabled) {
  if (enabled && !selectedAccount.value) {
    autoRefresh.value = false; ElMessage.warning('请先选择邮箱账号'); return
  }
  enabled ? startAutoRefresh() : stopAutoRefresh()
}

function restartAutoRefresh() { stopAutoRefresh(); if (autoRefresh.value && selectedAccount.value) startAutoRefresh() }
function startAutoRefresh() { stopAutoRefresh(); refreshTimer.value = window.setInterval(() => syncMails(false), AUTO_REFRESH_INTERVAL) }
function stopAutoRefresh() { if (refreshTimer.value) { window.clearInterval(refreshTimer.value); refreshTimer.value = null } }

function handleSelectionChange(rows) { selectedMailIds.value = rows.map(r => r.id) }
function viewMail(row) { selectedMail.value = row; manualClassifyForm.category = row.category || ''; manualClassifyForm.tags = row.tags || '' }
function toggleMailSelect(id, val) {
  if (val) selectedMailIds.value.push(id)
  else selectedMailIds.value = selectedMailIds.value.filter(i => i !== id)
}

async function sendMail() {
  if (!selectedAccountId.value) return
  if (!sendForm.to || !sendForm.subject) { ElMessage.warning('请填写收件人和主题'); return }
  sending.value = true
  try {
    await mailApi.send(selectedAccountId.value, { ...sendForm })
    ElMessage.success('邮件发送成功')
    showSendDialog.value = false
    Object.assign(sendForm, { to: '', cc: '', subject: '', content: '', priority: 'NORMAL' })
    await loadMails(selectedAccountId.value)
  } catch (e) {
    ElMessage.error('发送失败：' + (e.response?.data?.message || e.message))
  } finally { sending.value = false }
}

async function batchClassifyMails() {
  if (!selectedAccountId.value) return
  classifying.value = true
  try {
    if (classifyForm.scope === 'UNCLASSIFIED') {
      const res = await mailClassifyApi.classifyAccount(selectedAccountId.value, classifyForm.method)
      ElMessage.success(res.data.message || '分类完成')
    } else {
      const mailIds = classifyForm.scope === 'SELECTED' ? selectedMailIds.value : messages.value.map(m => m.id)
      if (mailIds.length === 0) {
        ElMessage.warning(classifyForm.scope === 'SELECTED' ? '请先在列表中勾选邮件' : '暂无可分类邮件')
        return
      }
      await mailClassifyApi.classifyBatch(mailIds, classifyForm.method)
      ElMessage.success(`成功分类 ${mailIds.length} 封邮件`)
      selectedMailIds.value = []
    }
    showBatchClassifyDialog.value = false
    await loadMails(selectedAccountId.value)
  } catch (e) { ElMessage.error('分类失败：' + (e.response?.data?.message || e.message))
  } finally { classifying.value = false }
}

async function batchClassifySelected() {
  if (selectedMailIds.value.length === 0) return
  classifying.value = true
  try {
    await mailClassifyApi.classifyBatch(selectedMailIds.value, 'HYBRID')
    ElMessage.success(`成功分类 ${selectedMailIds.value.length} 封邮件`)
    selectedMailIds.value = []
    await loadMails(selectedAccountId.value)
  } catch (e) { ElMessage.error('分类失败：' + (e.response?.data?.message || e.message))
  } finally { classifying.value = false }
}

async function classifySingleMail() {
  if (!selectedMail.value?.id) return
  classifying.value = true
  try {
    const res = await mailClassifyApi.classify(selectedMail.value.id, 'HYBRID')
    selectedMail.value = res.data
    const idx = messages.value.findIndex(m => m.id === res.data.id)
    if (idx >= 0) messages.value[idx] = res.data
    ElMessage.success('分类完成')
  } catch (e) { ElMessage.error('分类失败：' + (e.response?.data?.message || e.message))
  } finally { classifying.value = false }
}

async function saveManualClassify() {
  if (!selectedMail.value?.id) return
  if (!manualClassifyForm.category) { ElMessage.warning('请选择分类'); return }
  try {
    const res = await mailClassifyApi.manualClassify(selectedMail.value.id, manualClassifyForm.category, manualClassifyForm.tags)
    selectedMail.value = res.data
    const idx = messages.value.findIndex(m => m.id === res.data.id)
    if (idx >= 0) messages.value[idx] = res.data
    ElMessage.success('分类已更新')
    showManualClassifyDialog.value = false
  } catch (e) { ElMessage.error('保存失败：' + (e.response?.data?.message || e.message)) }
}

function onSearch() {}

// --- 工具函数 ---
function getInitial(str) {
  if (!str) return '?'
  const name = str.includes('@') ? str.split('@')[0] : str
  const ch = name.trim().charAt(0)
  return /[一-龥]/.test(ch) ? ch : ch.toUpperCase()
}

const avatarColors = [
  '#4f46e5','#7c3aed','#db2777','#ea580c','#059669',
  '#0891b2','#2563eb','#d97706','#65a30d','#9333ea',
  '#dc2626','#0d9488','#c026d3','#ca8a04','#0284c7'
]
function getAvatarColor(str) {
  if (!str) return avatarColors[0]
  let hash = 0
  for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash)
  return avatarColors[Math.abs(hash) % avatarColors.length]
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (diff < 172800000 && date.getDate() === now.getDate() - 1) return '昨天'
  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}

function getPreview(content) {
  if (!content) return ''
  return content.replace(/\s+/g, ' ').trim().substring(0, 120)
}

function sanitizeHtml(html) {
  if (!html) return ''
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/on\w+\s*=\s*"[^"]*"/gi, '')
    .replace(/on\w+\s*=\s*'[^']*'/gi, '')
}

function getCategoryColor(cat) {
  const map = {
    '工作': '#4f46e5', '商务': '#059669', '财务': '#d97706',
    '通知': '#2563eb', '社交': '#0891b2', '营销': '#7c3aed',
    '垃圾邮件': '#dc2626', '其他': '#6b7280', '未分类': '#9ca3af'
  }
  return map[cat] || '#6b7280'
}

function getCategoryBgColor(cat) {
  const map = {
    '工作': '#eef2ff', '商务': '#ecfdf5', '财务': '#fffbeb',
    '通知': '#eff6ff', '社交': '#ecfeff', '营销': '#f5f3ff',
    '垃圾邮件': '#fef2f2', '其他': '#f9fafb', '未分类': '#f9fafb'
  }
  return map[cat] || '#f9fafb'
}

function getConfidenceColor(v) {
  if (v >= 0.8) return '#059669'
  if (v >= 0.5) return '#d97706'
  return '#dc2626'
}

function getCategoryCount(cat) {
  return messages.value.filter(m => m.category === cat).length
}

function getFileIcon(name) {
  const ext = (name || '').split('.').pop()?.toLowerCase()
  const map = { pdf: 'PDF', doc: 'DOC', docx: 'DOCX', xls: 'XLS', xlsx: 'XLSX', ppt: 'PPT', pptx: 'PPTX', jpg: '图片', jpeg: '图片', png: '图片', gif: '图片', zip: 'ZIP', rar: 'RAR', '7z': '7Z' }
  return map[ext] || (ext || '').toUpperCase()
}

function getMethodText(m) {
  const map = { RULE: '规则分类', LLM: 'AI分类', HYBRID: '混合分类', MANUAL: '手动分类' }
  return map[m] || m
}

onMounted(() => { loadAccounts(); loadActiveLlmConfig() })
onUnmounted(() => { stopAutoRefresh() })
</script>

<style scoped>
.mail-app {
  --mail-bg: #f5f6f8;
  --mail-surface: #ffffff;
  --mail-border: #e5e7eb;
  --mail-text: #1f2937;
  --mail-text-secondary: #6b7280;
  --mail-text-tertiary: #9ca3af;
  --mail-primary: #4f46e5;
  --mail-primary-light: #eef2ff;
  --mail-hover: #f9fafb;
  --mail-active: #eef2ff;
  --mail-radius: 8px;
  --mail-shadow-sm: 0 1px 2px rgba(0,0,0,.05);
  --mail-shadow: 0 1px 3px rgba(0,0,0,.08), 0 1px 2px rgba(0,0,0,.06);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  background: var(--mail-bg);
  border-radius: 12px;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.mail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: var(--mail-surface);
  border-bottom: 1px solid var(--mail-border);
  z-index: 10;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 12px; }
.account-select { width: 300px; }
.account-select :deep(.el-input__wrapper) {
  border-radius: 8px; background: var(--mail-bg); box-shadow: none;
}
.auto-refresh-check :deep(.el-checkbox__label) { font-size: 13px; color: var(--mail-text-secondary); }
.toolbar-btn { border-radius: 8px; }

.mail-body { display: flex; flex: 1; overflow: hidden; }

.mail-sidebar {
  width: 240px; min-width: 240px;
  background: var(--mail-surface);
  border-right: 1px solid var(--mail-border);
  display: flex; flex-direction: column; overflow-y: auto;
}
.account-card { padding: 20px 16px; border-bottom: 1px solid var(--mail-border); display: flex; flex-direction: column; align-items: center; gap: 10px; text-align: center; }
.account-card-empty { padding: 30px 16px; }
.account-avatar-large {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, var(--mail-primary), #7c3aed);
  color: #fff; font-size: 22px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(79,70,229,.3);
}
.account-info { width: 100%; }
.account-name { font-weight: 600; font-size: 14px; color: var(--mail-text); margin-bottom: 2px; }
.account-addr { font-size: 12px; color: var(--mail-text-tertiary); word-break: break-all; }

.folder-nav { padding: 12px; border-bottom: 1px solid var(--mail-border); }
.folder-nav:last-of-type { border-bottom: none; }
.nav-title { font-size: 11px; font-weight: 600; color: var(--mail-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; padding: 0 8px; margin-bottom: 6px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: 8px; cursor: pointer;
  font-size: 14px; color: var(--mail-text-secondary);
  transition: all .15s; position: relative;
}
.nav-item:hover { background: var(--mail-hover); color: var(--mail-text); }
.nav-item.active { background: var(--mail-primary-light); color: var(--mail-primary); font-weight: 600; }
.nav-item .el-icon { font-size: 18px; }
.nav-badge {
  margin-left: auto; background: var(--mail-border); color: var(--mail-text-secondary);
  font-size: 11px; font-weight: 600; padding: 1px 7px; border-radius: 10px;
  min-width: 20px; text-align: center;
}
.nav-item.active .nav-badge { background: var(--mail-primary); color: #fff; }
.category-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.sidebar-footer { margin-top: auto; padding: 12px 16px; border-top: 1px solid var(--mail-border); }
.sync-status { font-size: 12px; color: var(--mail-text-tertiary); display: flex; align-items: center; gap: 6px; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.mail-list-panel {
  width: 420px; min-width: 360px;
  background: var(--mail-surface);
  border-right: 1px solid var(--mail-border);
  display: flex; flex-direction: column;
}
.list-toolbar { padding: 12px 16px; border-bottom: 1px solid var(--mail-border); display: flex; align-items: center; gap: 8px; }
.list-toolbar-left { flex: 1; }
.search-input :deep(.el-input__wrapper) { border-radius: 8px; background: var(--mail-bg); box-shadow: none; border: none; }

.selection-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: var(--mail-primary-light);
  font-size: 13px; color: var(--mail-primary); border-bottom: 1px solid #dbeafe;
}
.slide-down-enter-active, .slide-down-leave-active { transition: all .2s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-100%); }

.mail-list { flex: 1; overflow-y: auto; }
.mail-list::-webkit-scrollbar { width: 4px; }
.mail-list::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }

.mail-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 12px 16px; border-bottom: 1px solid #f3f4f6;
  cursor: pointer; transition: all .12s;
}
.mail-item:hover { background: var(--mail-hover); }
.mail-item-active { background: var(--mail-active) !important; box-shadow: inset 3px 0 0 var(--mail-primary); }
.mail-item-unread .mail-item-sender,
.mail-item-unread .mail-item-subject { font-weight: 700; }
.mail-item-check { padding-top: 2px; opacity: 0; transition: opacity .15s; }
.mail-item:hover .mail-item-check { opacity: 1; }
.mail-item-active .mail-item-check { opacity: 1; }
.mail-item-star { padding-top: 2px; flex-shrink: 0; }
.star-icon { cursor: pointer; transition: transform .15s; }
.star-icon:hover { transform: scale(1.2); }

.sender-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  color: #fff; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.mail-item-body { flex: 1; min-width: 0; overflow: hidden; }
.mail-item-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 2px; }
.mail-item-sender { font-size: 14px; color: var(--mail-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.mail-item-time { font-size: 11px; color: var(--mail-text-tertiary); white-space: nowrap; flex-shrink: 0; margin-left: 8px; }
.mail-item-subject { font-size: 13px; color: var(--mail-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px; }
.mail-item-preview { font-size: 12px; color: var(--mail-text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mail-item-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.category-chip { font-size: 11px !important; padding: 0 6px !important; height: 18px !important; line-height: 18px !important; border: none !important; }
.attach-icon { color: var(--mail-text-tertiary); font-size: 14px; }

.mail-list-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; color: var(--mail-text-tertiary); gap: 8px; }
.mail-list-empty p { margin: 0; font-size: 14px; }

.mail-detail-panel { flex: 1; background: var(--mail-bg); display: flex; flex-direction: column; overflow: hidden; }
.mail-detail-empty { display: flex; align-items: center; justify-content: center; }
.detail-toolbar { display: flex; justify-content: space-between; padding: 10px 20px; background: var(--mail-surface); border-bottom: 1px solid var(--mail-border); }
.detail-toolbar-left, .detail-toolbar-right { display: flex; gap: 8px; align-items: center; }

.detail-content { flex: 1; overflow-y: auto; padding: 24px 32px; }
.detail-content::-webkit-scrollbar { width: 6px; }
.detail-content::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }

.detail-header { margin-bottom: 20px; }
.detail-subject { font-size: 22px; font-weight: 700; color: var(--mail-text); margin: 0 0 12px 0; line-height: 1.3; }
.detail-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag-item { font-size: 12px !important; }

.sender-card {
  display: flex; gap: 16px; padding: 20px;
  background: var(--mail-surface); border-radius: var(--mail-radius);
  box-shadow: var(--mail-shadow-sm); margin-bottom: 16px;
}
.sender-avatar-large {
  width: 48px; height: 48px; border-radius: 50%;
  color: #fff; font-size: 18px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.sender-info { flex: 1; min-width: 0; }
.sender-name { font-size: 15px; font-weight: 600; color: var(--mail-text); margin-bottom: 4px; }
.sender-email { font-weight: 400; color: var(--mail-text-secondary); font-size: 13px; }
.sender-meta { font-size: 13px; color: var(--mail-text-secondary); display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 2px; }
.sender-time { font-size: 12px; color: var(--mail-text-tertiary); }

.attachment-section { background: var(--mail-surface); border-radius: var(--mail-radius); padding: 16px 20px; margin-bottom: 16px; box-shadow: var(--mail-shadow-sm); }
.section-label { font-size: 13px; font-weight: 600; color: var(--mail-text-secondary); margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
.attachment-list { display: flex; flex-wrap: wrap; gap: 10px; }
.attachment-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 14px;
  background: var(--mail-bg); border-radius: 8px; cursor: pointer;
  transition: all .15s; border: 1px solid transparent;
}
.attachment-item:hover { border-color: var(--mail-primary); background: var(--mail-primary-light); }
.attachment-icon { color: var(--mail-primary); }
.attachment-name { font-size: 13px; color: var(--mail-text); max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attachment-size { font-size: 11px; color: var(--mail-text-tertiary); font-weight: 600; }

.classify-collapse { background: var(--mail-surface); border-radius: var(--mail-radius); margin-bottom: 16px; border: none; box-shadow: var(--mail-shadow-sm); }
.classify-collapse :deep(.el-collapse-item__header) { padding: 12px 20px; font-size: 13px; font-weight: 600; border: none; }
.classify-collapse :deep(.el-collapse-item__wrap) { border: none; background: transparent; }
.classify-detail { padding: 0 20px 16px; display: flex; flex-direction: column; gap: 10px; }
.classify-row { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.classify-label { color: var(--mail-text-tertiary); width: 70px; flex-shrink: 0; font-size: 12px; }
.classify-value { color: var(--mail-text-secondary); font-size: 12px; line-height: 1.5; }

.mail-content { background: var(--mail-surface); border-radius: var(--mail-radius); padding: 24px; box-shadow: var(--mail-shadow-sm); min-height: 200px; }
.mail-content-text { white-space: pre-wrap; font-size: 14px; line-height: 1.8; color: var(--mail-text); }
.mail-content-html { font-size: 14px; line-height: 1.8; color: var(--mail-text); }
.mail-content-html :deep(img) { max-width: 100%; }
.mail-content-html :deep(a) { color: var(--mail-primary); }

.empty-state { text-align: center; color: var(--mail-text-tertiary); }
.empty-icon { margin-bottom: 16px; }
.empty-state h3 { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: var(--mail-text-secondary); }
.empty-state p { margin: 0; font-size: 14px; }

.compose-form :deep(.el-form-item) { margin-bottom: 18px; }
.compose-from { font-size: 14px; color: var(--mail-text-secondary); padding: 8px 0; }
.classify-alert { margin-bottom: 15px; }

@media (max-width: 1200px) {
  .mail-sidebar { width: 200px; min-width: 200px; }
  .mail-list-panel { width: 340px; min-width: 300px; }
}
@media (max-width: 900px) {
  .mail-sidebar { display: none; }
  .mail-list-panel { width: 100%; }
  .mail-detail-panel { position: fixed; inset: 0; z-index: 100; }
}
</style>
