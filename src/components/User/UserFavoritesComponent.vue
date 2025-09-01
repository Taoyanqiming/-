<template>
  <div class="user-favorites-component">
    <!-- 顶部标题和筛选栏 -->
    <div class="header-row">
      <div class="header-cell label">我的收藏</div>
      <div class="header-cell count">{{ total }}</div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-mask">
      <el-loading-spinner size="large" />
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="errorMessage" class="error-tip">
      <i class="el-icon-error-circle"></i>
      <p>{{ errorMessage }}</p>
      <el-button size="small" type="primary" @click="fetchFavorites">重试</el-button>
    </div>

    <!-- 帖子列表 -->
    <div v-else class="posts-list">
      <el-table v-if="posts.length > 0" :data="posts" style="width: 100%; margin-top: 16px;"
        @row-click="goToPostDetail">
        <el-table-column label="帖子" min-width="300">
          <template #default="{ row }">
            <div class="post-row">
              <img :src="row.icon || defaultAvatar" alt="user avatar" class="post-avatar" />
              <div class="post-content">
                <div class="post-header">
                  <div class="post-title">{{ row.title }}</div>
                  <div class="post-author">
                    <span>{{ row.userName }}</span>
                    <span class="post-time">{{ formatDate(row.createTime) }}</span>
                  </div>
                </div>
                <div class="post-body">{{ row.content }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="180">
          <template #default="{ row }">
            <div class="post-meta">
              <span>
                <i class="el-icon-view"></i> 浏览 {{ row.view || 0 }}
              </span>
              <span>
                <i class="el-icon-heart" :class="{ 'is-active': row.liked }"></i>
                点赞 {{ row.liked || 0 }}
              </span>
              <span>
                <i class="el-icon-star" :class="{ 'is-active': row.favorite }"></i>
                收藏 {{ row.favorite || 0 }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <el-pagination v-if="posts.length > 0 && total > pageSize" class="pagination" :current-page="currentPage"
        :page-sizes="[10, 20, 30]" :page-size="pageSize" :total="total" @size-change="handlePageChange"
        @current-change="handlePageChange" layout="total, sizes, prev, pager, next, jumper" />

      <!-- 空状态提示 -->
      <div v-if="posts.length === 0 && !isLoading && !errorMessage" class="empty-tip">
        <div class="empty-icon">
          <i class="el-icon-collection-empty"></i>
        </div>
        <p>暂无收藏的帖子</p>
        <el-button type="primary" @click="goToForum">浏览帖子</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { getMyFavorites } from '@/api/users.js';

const router = useRouter();
const defaultAvatar = 'https://picsum.photos/80/80?random=avatar';

// 分页和加载状态
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const isLoading = ref(false);
const errorMessage = ref('');

// 数据和筛选
const posts = ref([]);

// 格式化日期函数
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

// 生命周期钩子：初始化加载数据
onMounted(() => {
  fetchFavorites();
});

// 获取收藏列表
const fetchFavorites = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const userFavorDTO = {
      page: currentPage.value,
      pageSize: pageSize.value,
    };

    console.log('请求参数:', userFavorDTO);
    const res = await getMyFavorites(userFavorDTO);
    console.log('获取收藏数据:', res);

    // 检查响应状态
    if (!res || res.status !== 200) {
      throw new Error('接口调用失败');
    }

    // 检查业务状态码
    if (res.data.code === 1) {
      const data = res.data.data;
      console.log('解析数据:', data);

      posts.value = data.records.map(post => ({
        ...post,
        // 处理图片字段 - 使用用户头像而非帖子图片
        icon: post.icon || defaultAvatar,
        // 格式化日期
        createTimeFormatted: formatDate(post.createTime),
        updateTimeFormatted: formatDate(post.updateTime)
      }));

      total.value = data.total;
    } else {
      ElMessage.error(res.data.message || '加载帖子失败');
    }
  } catch (error) {
    console.error('网络请求失败:', error);
    errorMessage.value = '获取收藏列表失败，请稍后重试';

    // 处理特定错误
    if (error.response && error.response.status === 401) {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        router.push('/login');
      });
    }
  } finally {
    isLoading.value = false;
  }
};

// 分页处理函数
const handlePageChange = (val) => {
  currentPage.value = val; // 更新页码
  fetchFavorites(); // 触发数据重新获取
};

// 跳转到帖子详情
const goToPostDetail = (row) => {
  router.push(`/user/forum/${row.postId}`);
};

// 预览图片
const previewImage = (imgUrl) => {
  window.open(imgUrl, '_blank');
};

// 跳转到论坛首页
const goToForum = () => {
  router.push('/user/forum');
};
</script>

<style scoped>
.user-favorites-component {
  padding: 24px;
  background: #fff;
  min-height: 100vh;
}

.header-row {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.header-cell {
  padding: 8px 16px;
  border-right: 1px solid #ebeef5;
  font-size: 16px;
  background: #f5f7fa;
  min-width: 80px;
  text-align: center;
}

.header-cell.label {
  font-weight: bold;
  color: #409eff;
  border-radius: 4px 0 0 4px;
}

.header-cell.count {
  font-weight: bold;
  color: #67c23a;
  border-radius: 0 4px 4px 0;
}

.loading-mask {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
}

.error-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #f56c6c;
}

.posts-list {
  margin-top: 16px;
}

.post-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  cursor: pointer;
  padding: 12px;
  transition: background-color 0.2s;
}

.post-row:hover {
  background-color: #f9fafc;
}

.post-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.post-content {
  flex: 1;
  min-width: 0;
}

.post-header {
  margin-bottom: 4px;
}

.post-title {
  font-weight: bold;
  font-size: 16px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-author {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.post-time {
  color: #999;
}

.post-body {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  max-height: 3em;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-meta {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.post-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-meta .is-active {
  color: #f56c6c;
}

.empty-tip {
  text-align: center;
  color: #aaa;
  margin: 32px 0;
  padding: 48px 24px;
  background-color: #f9fafc;
  border-radius: 8px;
}

.empty-icon {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

.pagination {
  margin: 24px 0 0 0;
  text-align: center;
}
</style>
