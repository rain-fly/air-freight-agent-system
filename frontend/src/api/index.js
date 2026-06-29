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