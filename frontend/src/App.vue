<template>
  <el-container style="height: 100vh">
    <!-- 侧边栏 -->
    <el-aside width="220px" style="background-color: #1f2d3d; color: #fff">
      <div style="padding: 20px; text-align: center; border-bottom: 1px solid #334257">
        <h2 style="margin: 0; font-size: 18px; color: #fff">✈ 空运代理系统</h2>
        <p style="font-size: 12px; color: #8a9db0; margin-top: 5px">AirFreight Agent</p>
      </div>
      <el-menu
        :default-active="currentRoute"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
        style="border-right: none"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-menu-item index="/quotation">
          <el-icon><Document /></el-icon>
          <span>询价报价</span>
        </el-menu-item>
        <el-menu-item index="/shipments">
          <el-icon><List /></el-icon>
          <span>运单管理</span>
        </el-menu-item>
        <el-menu-item index="/bookings">
          <el-icon><TakeawayBox /></el-icon>
          <span>订舱管理</span>
        </el-menu-item>
        <el-menu-item index="/financials">
          <el-icon><Money /></el-icon>
          <span>财务管理</span>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/mail">
          <el-icon><Message /></el-icon>
          <span>邮件客户端</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 50px">
        <span style="font-weight: 600; font-size: 14px">空运代理智能管理系统</span>
        <el-tag type="success" size="small">运行中</el-tag>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentRoute = computed(() => route.path)
</script>

<style>
body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.el-menu-item {
  font-size: 14px;
}
.el-menu-item .el-icon {
  margin-right: 8px;
}
</style>