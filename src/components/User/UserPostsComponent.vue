<template>
  <div class="user-posts-component">
    <!-- 顶部导航栏（水平排列，带筛选按钮） -->
    <div class="header-row">
      <!-- 标题与总数 -->
      <div class="header-group">
        <div class="header-label">我的发帖</div>
        <div class="header-count">{{ total }}</div>
      </div>

      <!-- 筛选按钮（触发不同排序） -->
      <div class="header-filters">
        <div class="filter-item" :class="{ active: sortType === 'latest' }" @click="handleSort('latest')">
          最新发布
        </div>
        <div class="filter-item" :class="{ active: sortType === 'hot' }" @click="handleSort('hot')">
          最多点赞
        </div>
        <div class="filter-item" :class="{ active: sortType === 'mostViewed' }" @click="handleSort('mostViewed')">
          最多浏览
        </div>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="posts-list">
      <el-table :data="posts" style="width: 100%; margin-top: 16px;">
        <el-table-column label="帖子" min-width="300">
          <template #default="{ row }">
            <div class="post-row" @click="goToPostDetail(row.postId)">
              <img :src="row.images || defaultAvatar" alt="post image" class="post-image" />
              <div class="post-content">
                <div class="post-title">{{ row.title }}</div>
                <div class="post-body">{{ row.content }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <div class="post-meta">
              <span>
                <i class="el-icon-view"></i> 浏览量 {{ row.view || 0 }}
              </span>
              <span>
                <i class="el-icon-heart" :class="{ 'is-active': row.liked }"></i>
                点赞 {{ row.liked || 0 }}
              </span>
              <span>
                <i class="el-icon-star" :class="{ 'is-active': row.favorited }"></i>
                收藏 {{ row.favorite || 0 }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <el-pagination v-if="total > 0" class="pagination" :current-page="currentPage" :page-sizes="[10, 20, 30]"
        :page-size="pageSize" :total="total" @size-change="handlePageChange" @current-change="handlePageChange"
        layout="total, sizes, prev, pager, next, jumper" />

      <!-- 空状态提示 -->
      <div v-if="posts.length === 0 && !isLoading" class="empty-tip">
        暂无发布的帖子
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { getMyPosts } from '@/api/users.js';

// 路由实例
const router = useRouter();

// 默认头像
const defaultAvatar = 'https://picsum.photos/80/80?random=avatar';

// 状态管理
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const isLoading = ref(false);
const posts = ref([]);
const sortType = ref('latest'); // 默认排序类型

// 生命周期：组件挂载后加载数据
onMounted(() => {
  fetchPosts();
});

// 获取帖子列表
const fetchPosts = async () => {
  isLoading.value = true;
  try {
    // 构造请求参数（包含 page, pageSize, sortType）
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      sortType: sortType.value
    };

    const res = await getMyPosts(params);
    console.log('帖子列表响应:', res);

    if (res.data.code === 1) {
      const data = res.data.data;
      posts.value = data.records.map(post => ({
        ...post,
        image: post.images?.length > 0 ? post.images[0] : defaultAvatar
      }));
      total.value = data.total;
    } else {
      ElMessage.error(res.data.message || '加载帖子失败');
    }
  } catch (error) {
    console.error('网络请求失败:', error);
    ElMessage.error('请检查网络连接后重试');
  } finally {
    isLoading.value = false;
  }
};

// 分页处理函数
const handlePageChange = (val) => {
  currentPage.value = val; // 更新页码
  fetchPosts(); // 触发数据重新获取
};

// 排序切换函数
const handleSort = (type) => {
  sortType.value = type; // 更新排序类型
  currentPage.value = 1; // 切换排序后重置为第一页
  fetchPosts(); // 触发数据重新获取
};

// 跳转帖子详情页
const goToPostDetail = (postId) => {
  console.log('跳转帖子详情，ID:', postId);
  router.push(`/user/forum/${postId}`);
};
</script>

<style scoped>
.user-posts-component {
  padding: 24px;
  background: #fff;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.header-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.header-label {
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.header-count {
  font-size: 18px;
  color: #666;
}

.header-filters {
  display: flex;
  gap: 16px;
}

.filter-item {
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 24px;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  transition: all 0.2s;
}

.filter-item.active {
  border-color: #409eff;
  color: #409eff;
  font-weight: 500;
  background-color: #f0f5ff;
}

.filter-item:hover {
  border-color: #409eff;
}

.post-row {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
}

.post-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 16px;
  background: #f2f2f2;
}

.post-content {
  flex: 1;
}

.post-title {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 4px;
}

.post-body {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  max-height: 4em;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-meta {
  font-size: 13px;
  color: #888;
  margin-top: 8px;
}

.post-meta span {
  margin-right: 16px;
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
  padding: 24px;
  background-color: #fff;
  border-radius: 8px;
}

.pagination {
  margin-top: 24px;
  text-align: center;
}
</style>