<template>
  <el-container style="height: 100vh; width: 100%;">
    <!-- 左侧导航栏 -->

    <el-aside width="200px" style="background: #f5f7fa;">
      <el-menu :default-active="currentPath" class="el-menu-vertical-demo" @select="handleMenuSelect" router>
        <el-menu-item index="/user/reservation/info">
          <el-icon>
            <Document />
          </el-icon>
          <span>我的预约</span>
        </el-menu-item>
        <el-menu-item index="/user/reservation/create">
          <el-icon>
            <Plus />
          </el-icon>
          <span>创建预约</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-main><router-view /></el-main>

  </el-container>
</template>

<script setup>
import { ref } from 'vue'
const activeMenu = ref('myReservations')

// 导航切换
function handleMenuSelect(index) {
  activeMenu.value = index
}

</script>

<style scoped>
.el-menu-vertical-demo {
  border-right: none;
}
</style>