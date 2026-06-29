import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Quotation from '@/views/Quotation.vue'
import Shipments from '@/views/Shipments.vue'
import Bookings from '@/views/Bookings.vue'
import Financials from '@/views/Financials.vue'
import Customers from '@/views/Customers.vue'

const routes = [
    { path: '/', name: 'Dashboard', component: Dashboard, meta: { title: '首页概览' } },
    { path: '/quotation', name: 'Quotation', component: Quotation, meta: { title: '询价报价' } },
    { path: '/shipments', name: 'Shipments', component: Shipments, meta: { title: '运单管理' } },
    { path: '/bookings', name: 'Bookings', component: Bookings, meta: { title: '订舱管理' } },
    { path: '/financials', name: 'Financials', component: Financials, meta: { title: '财务管理' } },
    { path: '/customers', name: 'Customers', component: Customers, meta: { title: '客户管理' } },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router