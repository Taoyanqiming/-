<template>
  <el-container class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="nav-header">
      <div class="nav-wrapper">
        <div class="menu-left">
          <el-menu :default-active="activeIndex" class="nav-menu" mode="horizontal" @select="handleSelect"
            :style="{ whiteSpace: 'nowrap' }">
            <el-menu-item v-for="(item, index) in navItems" :key="index" :index="item.index" class="nav-menu-item">
              {{ item.label }}
            </el-menu-item>
          </el-menu>
        </div>

        <div class="menu-right">
          <el-dropdown placement="bottom-end" @command="handleCommand">
            <span class="el-dropdown-link">
              <el-avatar :size="40" :src="userInfoStore.userInfo.icon || defaultAvatar" class="header-avatar" />
              {{ userInfoStore.userInfo.userName || '请登录' }}
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="info">
                  <i class="el-icon-user"></i> 个人信息
                </el-dropdown-item>
                <el-dropdown-item command="messages">
                  <i class="el-icon-message"></i> 我的消息
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <i class="el-icon-switch-button"></i> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主内容区域 -->
    <el-main class="main-content">
      <router-view></router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useTokenStore } from '@/stores/token.js';
import { useUserInfoStore } from '@/stores/userInfo.js';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UserFilled } from '@element-plus/icons-vue';

const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const router = useRouter();
const activeIndex = ref('0'); // 默认激活首页

// 导航菜单项配置
const navItems = [
  { index: '0', label: '首页', path: '/user' },
  { index: '1', label: '预约入校', path: '/user/reservation' },
  { index: '2', label: '纪念商城', path: '/user/mall' },
  { index: '3', label: '中心论坛', path: '/user/forum' }
];

// 导航栏点击事件处理
const handleSelect = (key) => {
  const item = navItems.find(item => item.index === key);
  if (item) {
    router.push(item.path)
      .then(() => {
        activeIndex.value = key; // 更新激活状态
        ElMessage.success(`跳转至 ${item.label}`);
      })
      .catch(error => {
        ElMessage.error('页面跳转失败，请检查网络');
        console.error('路由错误:', error);
      });
  }
};

// 下拉菜单命令处理
const handleCommand = (command) => {
  switch (command) {
    case 'info':
      router.push('/user/info');
      break;

    case 'messages':
      router.push('/user/messages');
      break;

    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        tokenStore.removeToken(); // 清除Token
        userInfoStore.clearInfo(); // 修正方法名（原代码中定义的是 clearInfo）
        router.push('/login'); // 跳转登录页
        ElMessage.success('退出登录成功');
      }).catch(() => {
        ElMessage.info('已取消退出');
      });
      break;

    default:
      break;
  }
};

// 挂载时检查用户登录状态
onMounted(() => {
  if (!tokenStore.token && router.currentRoute.value.path !== '/login') {

    router.push('/login');
  }
});
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  position: relative;
}

.nav-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  z-index: 999;
  box-sizing: border-box;
}

.nav-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

.menu-left {
  flex-grow: 1;
  overflow-x: auto;
  white-space: nowrap;
  -ms-overflow-style: none;
  /* IE 隐藏滚动条 */
  scrollbar-width: none;
  /* Firefox 隐藏滚动条 */
}

.menu-left::-webkit-scrollbar {
  display: none;
  /* Chrome/Safari 隐藏滚动条 */
}

.nav-menu {
  display: flex;
  gap: 0 24px;
}

.nav-menu-item {
  height: 64px;
  line-height: 64px;
  font-size: 16px;
  color: #333333;
  transition: color 0.2s;
  padding: 0 12px;
  min-width: max-content;
  /* 保持菜单项最小宽度 */
}

.nav-menu-item:hover {
  color: #409eff;
}

.menu-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 16px;
  color: #333;
}

.header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #f0f2f5;
}

.el-dropdown-menu {
  width: 160px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.el-dropdown-item {
  padding: 8px 24px;
  font-size: 14px;
  color: #666;
}

.el-dropdown-item i {
  margin-right: 8px;
  font-size: 16px;
  color: #999;
}

.main-content {
  padding: 24px;
  background-color: #f8f9fa;
  margin-top: 64px;
  /* 预留导航栏高度 */
  min-height: calc(100vh - 64px);
}
</style>