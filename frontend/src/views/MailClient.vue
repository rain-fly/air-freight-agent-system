<template>
  <el-empty description="正在进入邮箱列表..." />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/mail/messages')
})
</script>
