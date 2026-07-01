import axios from 'axios'

const api = axios.create({
    baseURL: '/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 响应拦截器
api.interceptors.response.use(
    response => response,
    error => {
        console.error('API Error:', error.response?.data || error.message)
        return Promise.reject(error)
    }
)

export default api

// ===== 询价与运单 API =====
export const shipmentApi = {
    calculateVolumeWeight(grossWeight, length, width, height) {
        return api.post('/shipments/calculate-volume-weight', null, {
            params: { grossWeight, length, width, height }
        })
    },
    getQuotation(data) {
        return api.post('/shipments/quotation', data)
    },
    createShipment(data) {
        return api.post('/shipments/create-from-quotation', data)
    },
    getAll() {
        return api.get('/shipments')
    },
    getById(id) {
        return api.get(`/shipments/${id}`)
    },
    getByStatus(status) {
        return api.get(`/shipments/status/${status}`)
    }
}

// ===== 订舱 API =====
export const bookingApi = {
    submit(data) {
        return api.post('/bookings', data)
    },
    confirm(id) {
        return api.put(`/bookings/${id}/confirm`)
    },
    getAll() {
        return api.get('/bookings')
    },
    getById(id) {
        return api.get(`/bookings/${id}`)
    },
    generateAwb(type, airlineCode) {
        return api.get('/bookings/generate-awb', { params: { type, airlineCode } })
    }
}

// ===== 财务 API =====
export const financialApi = {
    generateInvoice(shipmentId) {
        return api.post(`/financials/invoice/${shipmentId}`)
    },
    getAll() {
        return api.get('/financials')
    },
    getByShipment(shipmentId) {
        return api.get(`/financials/shipment/${shipmentId}`)
    },
    swiftPayment(id, swiftCode, bankAccount) {
        return api.post(`/financials/${id}/swift-payment`, null, {
            params: { swiftCode, bankAccount }
        })
    }
}

// ===== 客户 API =====
export const customerApi = {
    create(data) {
        return api.post('/customers', data)
    },
    update(id, data) {
        return api.put(`/customers/${id}`, data)
    },
    getAll() {
        return api.get('/customers')
    },
    getById(id) {
        return api.get(`/customers/${id}`)
    }
}

// ===== 邮箱账号 API =====
export const mailAccountApi = {
    getAll() {
        return api.get('/mail/accounts')
    },
    create(data) {
        return api.post('/mail/accounts', data)
    },
    update(id, data) {
        return api.put(`/mail/accounts/${id}`, data)
    },
    delete(id) {
        return api.delete(`/mail/accounts/${id}`)
    },
    testConnection(data) {
        return api.post('/mail/accounts/test-connection', data)
    },
    testSavedConnection(id) {
        return api.post(`/mail/accounts/${id}/test`)
    },
    testSend(data, to) {
        return api.post('/mail/accounts/test-send', data, { params: { to } })
    },
    testSavedSend(id, to) {
        return api.post(`/mail/accounts/${id}/test-send`, null, { params: { to } })
    }
}

// ===== 邮件收发 API =====
export const mailApi = {
    receive(accountId) {
        return api.post(`/mail/receive/${accountId}`)
    },
    send(accountId, data) {
        return api.post(`/mail/send/${accountId}`, null, { params: data })
    },
    getMessages(accountId) {
        return api.get(`/mail/messages/${accountId}`)
    },
    search(keyword) {
        return api.get('/mail/search', { params: { keyword } })
    }
}

// ===== 邮件分类 API =====
export const mailClassifyApi = {
    // 单封分类
    classify(mailId, method = 'HYBRID') {
        return api.post(`/mail/classify/${mailId}`, null, { params: { method } })
    },
    // 批量分类
    classifyBatch(mailIds, method = 'HYBRID') {
        return api.post('/mail/classify/batch', null, { params: { mailIds: mailIds.join(','), method } })
    },
    // 账号下所有未分类邮件分类
    classifyAccount(accountId, method = 'HYBRID') {
        return api.post(`/mail/classify/account/${accountId}`, null, { params: { method } })
    },
    // 手动分类
    manualClassify(mailId, category, tags) {
        return api.put(`/mail/classify/${mailId}/manual`, null, { params: { category, tags } })
    }
}

// ===== LLM配置 API =====
export const llmConfigApi = {
    getAll() {
        return api.get('/llm-config')
    },
    getActive() {
        return api.get('/llm-config/active')
    },
    create(data) {
        return api.post('/llm-config', data)
    },
    update(id, data) {
        return api.put(`/llm-config/${id}`, data)
    },
    activate(id) {
        return api.post(`/llm-config/${id}/activate`)
    },
    testConnection(id) {
        return api.post(`/llm-config/${id}/test`)
    },
    delete(id) {
        return api.delete(`/llm-config/${id}`)
    }
}

// ===== 分类规则 API =====
export const classificationRuleApi = {
    getAll() {
        return api.get('/classification-rules')
    },
    getActive() {
        return api.get('/classification-rules/active')
    },
    create(data) {
        return api.post('/classification-rules', data)
    },
    update(id, data) {
        return api.put(`/classification-rules/${id}`, data)
    },
    delete(id) {
        return api.delete(`/classification-rules/${id}`)
    },
    initDefault() {
        return api.post('/classification-rules/init-default')
    }
}
