<template>
  <div class="user-header">
    <div class="background"></div>
    <div class="user-info">
      <label class="avatar-upload">
        <input type="file" accept="image/*" @change="onAvatarChange" hidden ref="fileInput" />
        <!-- 从Pinia获取头像URL，无数据时显示默认头像 -->
        <img :src="userInfo.icon || defaultAvatar" class="avatar" alt="用户头像" />
      </label>
      <!-- 从Pinia获取昵称，无数据时显示默认文本 -->
      <span class="nickname">{{ userInfo.userName || '请登录' }}</span>
    </div>
  </div>
  <!-- 用户信息页面内部的导航栏 -->
  <div class="user-info-nav">
    <router-link v-for="item in navItems" :key="item.key" :to="item.path"
      :class="['nav-item', { active: $route.path === item.path }]">
      {{ item.label }}
    </router-link>
  </div>

  <!-- 用户信息页面内部的路由视图 -->
  <div class="user-info-content">
    <router-view></router-view>
  </div>

  <!-- 上传加载状态 -->
  <div v-if="uploading" class="uploading-mask">
    <div class="uploading-spinner">
      <div class="spinner"></div>
      <p>上传中...</p>
    </div>
  </div>

</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useUserInfoStore } from '@/stores/userInfo'; // 引入Pinia存储
import { uploadAvatarService } from '@/api/users.js'; // 引入头像上传API

// 从Pinia获取用户信息
const userInfoStore = useUserInfoStore();
const userInfo = userInfoStore.userInfo; // 响应式数据

// 文件输入框引用
const fileInput = ref(null);

// 默认头像图片
const defaultAvatar = 'https://via.placeholder.com/80?text=Avatar';
const avatarUrl = ref(userInfo.icon || defaultAvatar); // 头像URL，优先使用用户已上传的头像

// 上传状态
const uploading = ref(false);
const uploadError = ref('');

// 点击头像触发文件选择
const triggerFileUpload = () => {
  fileInput.value.click();
};

// 头像上传处理
async function onAvatarChange(e) {
  const file = e.target.files[0];
  if (!file) return;

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    uploadError.value = '请选择图片文件';
    setTimeout(() => uploadError.value = '', 3000);
    return;
  }

  // 验证文件大小 (假设限制为2MB)
  const maxSize = 2 * 1024 * 1024; // 2MB
  if (file.size > maxSize) {
    uploadError.value = '图片大小不能超过2MB';
    setTimeout(() => uploadError.value = '', 3000);
    return;
  }

  // 临时显示选择的图片
  const tempUrl = URL.createObjectURL(file);
  avatarUrl.value = tempUrl;

  try {
    // 显示上传中状态
    uploading.value = true;

    // 调用API上传头像
    const response = await uploadAvatarService(file);
    console.log('头像上传响应:', response);
    // 上传成功后更新用户信息
    if (response.data.code === 1) {
      // 假设后端返回新头像的URL
      userInfoStore.setInfo({ ...userInfo, icon: response.data.data });
      avatarUrl.value = response.data.data;

      // 显示成功消息 (这里简化处理，实际项目中可能需要通知组件)
      console.log('头像上传成功');
    }
  } catch (error) {
    // 上传失败，恢复原头像
    avatarUrl.value = userInfo.icon || defaultAvatar;
    uploadError.value = '头像上传失败，请稍后再试';
    setTimeout(() => uploadError.value = '', 3000);
    console.error('头像上传失败:', error);
  } finally {
    // 隐藏上传中状态
    uploading.value = false;
    // 重置文件输入框，允许重复选择同一文件
    fileInput.value.value = '';
  }
}

// 监听用户信息变化，更新头像显示
watch(() => userInfo.icon, (newAvatar) => {
  if (newAvatar) {
    avatarUrl.value = newAvatar;
  }
});

// 只在用户信息页面显示的导航项
const navItems = [
  { key: 'posts', label: '个人发帖', path: '/user/info/posts' },
  { key: 'favorites', label: '我的收藏', path: '/user/info/favorites' },
  { key: 'edit', label: '修改信息', path: '/user/info/edit' }
];

// 组件挂载时初始化
onMounted(() => {
  // 确保初始显示正确的头像
  if (userInfo.icon) {
    avatarUrl.value = userInfo.icon;
  }
});

</script>

<style scoped>
.user-header {
  position: relative;
  height: 180px;
}

.background {
  background: linear-gradient(135deg, #6dd5ed, #2193b0);
  height: 120px;
  border-radius: 0 0 24px 24px;
}

.user-info {
  position: absolute;
  left: 24px;
  bottom: 0;
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 12px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.avatar-upload {
  cursor: pointer;
  margin-right: 16px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #eee;
}

.nickname {
  font-size: 20px;
  font-weight: bold;
}

.user-nav {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-top: 16px;
  background: #fff;
}

.nav-item {
  padding: 16px 32px;
  cursor: pointer;
  font-size: 16px;
  color: #666;
}

.nav-item.active {
  color: #2193b0;
  border-bottom: 2px solid #2193b0;
  font-weight: bold;
}

.user-content {
  padding: 24px;
  background: #fff;
  min-height: 200px;
}

.user-info-nav {
  display: flex;
  gap: 16px;
  /* 按钮之间的间距 */
  margin-top: 24px;
  padding: 0 24px;
}

.avatar {
  width: 60px;
  /* 设置头像宽度 */
  height: 60px;
  /* 设置头像高度 */
  object-fit: cover;
  /* 确保图片填充整个容器，保持比例 */
  border-radius: 50%;
  /* 将图片裁剪为圆形 */
  border: 2px solid #fff;
  /* 添加白色边框，可根据需要调整颜色 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  /* 添加轻微阴影，增强立体感 */
}

.nav-item {
  padding: 10px 24px;
  border: 1px solid #ddd;
  /* 添加边框 */
  border-radius: 24px;
  /* 圆角边框 */
  text-decoration: none;
  /* 删除下划线 */
  color: #333;
  font-size: 16px;
  transition: all 0.3s;
  text-align: center;
  flex: 1;
  /* 平均分配宽度 */
  white-space: nowrap;
  /* 防止文字换行 */
}

.nav-item:hover {
  border-color: #409eff;
  /* 悬停时边框颜色 */
  color: #409eff;
}

.nav-item.active {
  background-color: #409eff;
  /* 激活状态背景色 */
  border-color: #409eff;
  /* 激活状态边框色 */
  color: white;
  /* 激活状态文字颜色 */
  font-weight: 500;
}

/* 移除 router-link 默认的下划线 */
.router-link-exact-active {
  text-decoration: none;
}
</style>