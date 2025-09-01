<template>
  <div class="forum-page">
    <!-- 第一行：搜索框 -->
    <div class="search-bar">
      <el-button type="danger" icon="el-icon-search" @click="goToCreatePost" class="create">发帖</el-button>
      <el-input v-model="searchQuery" placeholder="搜索帖子" clearable @keyup.enter="handleSearch">
        <template #append>
          <el-button icon="el-icon-search" @click="handleSearch">确认</el-button>
        </template>
      </el-input>

    </div>

    <!-- 第二行：走马灯 -->
    <el-carousel height="200px" class="carousel">
      <!-- 轮播图项直接使用静态图片路径 -->
      <el-carousel-item>
        <img src="@/assets/eg1.png" class="carousel-img" alt="轮播图1" @click="goToPostDetail(1)" />
      </el-carousel-item>
      <el-carousel-item>
        <img src="@/assets/eg2.png" class="carousel-img" alt="轮播图2" @click="goToPostDetail(2)" />
      </el-carousel-item>
    </el-carousel>

    <!-- 第三行：筛选框 -->
    <div class="filter-row">
      <el-select v-model="sortType" placeholder="排序方式" style="width: 120px;">
        <el-option label="最新" value="newest"></el-option>
        <el-option label="热度" value="mostViewed"></el-option>
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="起始日期"
        end-placeholder="截止日期" style="width: 260px;" value-format="YYYY-MM-DD" />
      <el-button type="primary" @click="handleFilter">筛选</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <!-- 第四行：帖子列表 -->
        <div class="posts-list">
          <el-card v-for="post in posts" :key="post.postId" class="post-item" shadow="hover">
            <div class="post-main" @click="goToPostDetail(post.postId)">
              <el-avatar :src="post.icon || defaultAvatar" size="large" class="post-avatar" />
              <div class="post-info">
                <div class="post-header">
                  <div class="post-title">{{ post.title }}</div>
                  <div class="post-author">
                    <span>{{ post.userName || '匿名用户' }}</span>
                    <span class="post-time">{{ formatDate(post.createTime) }}</span>
                  </div>
                </div>
                <div class="post-content">
                  <div v-if="post.content" class="post-text">{{ post.content }}</div>
                  <div class="post-images" v-if="post.images && post.images.length > 0">
                    <img v-for="(img, idx) in post.images" :key="idx" :src="img" class="post-img"
                      @click.stop="previewImage(img)" />
                  </div>
                </div>
                <div class="post-meta">
                  <span><i class="el-icon-view"></i>浏览量 {{ post.view || 0 }}</span>
                  <span><i class="el-icon-heart" :class="{ 'is-active': post.liked }"></i>点赞 {{ post.liked || 0
                  }}</span>
                  <span><i class="el-icon-star" :class="{ 'is-active': post.favorite }"></i>收藏 {{ post.favorite || 0
                  }}</span>
                  <span class="update-time">更新于: {{ formatDate(post.updateTime) }}</span>
                </div>
              </div>
            </div>
          </el-card>
          <div v-if="posts.length === 0 && !isLoading" class="empty-tip">暂无相关帖子</div>
          <el-pagination v-if="total > 0" class="pagination" :current-page="currentPage" :page-sizes="[10, 20, 30]"
            :page-size="pageSize" :total="total" @size-change="handlePageSizeChange"
            @current-change="handleCurrentPageChange" layout="total, sizes, prev, pager, next, jumper" />
        </div>
      </el-col>
      <el-col :span="8">
        <!-- 今日热榜卡片 -->
        <el-card style="max-width: 480px; margin-bottom: 24px;">
          <template #header>
            <div class="card-header">
              <h3 class="hot-list-title">今日热榜</h3>
            </div>
          </template>
          <ul>
            <li v-for="(post, index) in topPosts" :key="post.postId || index">
              {{ index + 1 }}.
              <span @click="goToPostDetail(post.postId)" :class="{
                'color-red': index === 0,  /* 第1名红色 */
                'color-orange': index === 1, /* 第2名橙色 */
                'color-green': index === 2   /* 第3名绿色 */
              }">
                <!-- 标题截断：前10字 + 省略号 -->
                {{ truncateTitle(post.title) }}
              </span>
            </li>
          </ul>
        </el-card>
        <!-- 新增：公告栏卡片 -->
        <el-card style="max-width: 480px;">
          <template #header>
            <div class="card-header">
              <h3 class="hot-list-title">公告栏</h3>
            </div>
          </template>
          <div class="notice-content">
            <!-- 这里可以渲染公告数据，示例为静态文本 -->
            <p>📢 欢迎使用论坛！请遵守社区规范，文明发言。</p>
            <p>✨ 最新活动：参与话题讨论赢取积分奖励！</p>
            <!-- 如需动态数据，可参考热榜逻辑添加响应式数据 -->
          </div>
        </el-card>
      </el-col>
    </el-row>


  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { getPostsByPageService, getTopPostsByViewToday } from '@/api/forum.js';

const router = useRouter();
const searchQuery = ref('');
const sortType = ref('newest');
const dateRange = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const isLoading = ref(false);
const posts = ref([]);
const carouselPosts = ref([]);
const defaultAvatar = 'https://picsum.photos/64/64?random=user';
// 新增：热榜数据
const topPosts = ref([]);

// 修复：获取热榜数据的方法
const fetchTopPosts = async () => {
  try {
    const res = await getTopPostsByViewToday();
    console.log('热榜数据:', res);

    // 关键点：访问内层的 data 数组（res.data.data）
    const postsArray = res?.data?.data || []; // 确保是数组或空数组

    // 过滤无效数据（如 postId 缺失的项）
    topPosts.value = postsArray.filter(post => {
      return post && typeof post.postId === 'number'; // 校验 postId 为有效数字
    });
  } catch (error) {
    console.error('热榜请求失败:', error);
    topPosts.value = []; // 失败时清空数据
  }
};
const fetchPosts = async () => {
  isLoading.value = true;
  try {
    const postPageQueryDTO = {
      title: searchQuery.value,
      page: currentPage.value,
      pageSize: pageSize.value,
      sortType: sortType.value,
      startTime: formatDateToLocalDateTime(dateRange.value[0]),
      endTime: formatDateToLocalDateTime(dateRange.value[1]),
    };

    const res = await getPostsByPageService(postPageQueryDTO);
    console.log(res);

    if (res.status === 200) {
      posts.value = (res.data.data.records || []).map(post => ({
        ...post,
        icon: post.icon || defaultAvatar,
        userName: post.userName || '匿名用户',
        images: post.images ? (Array.isArray(post.images) ? post.images : [post.images]) : [],
        likedCount: post.liked || 0,
        favoriteCount: post.favorite || 0,
      }));
      total.value = res.data.data.total || 0;
    } else {
      ElMessage.error(res.message || '加载帖子失败');
    }
  } catch (e) {
    console.error('请求失败:', e.response?.data || e.message);
    ElMessage.error('网络请求失败，请检查参数或联系管理员');
  } finally {
    isLoading.value = false;
  }
};
// 在 script 标签内添加
const truncateTitle = (title) => {
  const maxLength = 30; // 最大显示字数
  return title?.length > maxLength
    ? title.slice(0, maxLength) + '...'
    : title || '暂无标题';
};

// 格式化日期为 LocalDateTime 格式 (yyyy-MM-dd'T'HH:mm:ss)
const formatDateToLocalDateTime = (date) => {
  if (!date) return null;
  const d = new Date(date);
  return d.toISOString();  // 转换为 ISO 8601 格式
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchPosts();
};

const handleFilter = () => {
  currentPage.value = 1;
  fetchPosts();
};

const resetFilter = () => {
  searchQuery.value = '';
  sortType.value = 'newest';
  dateRange.value = [];
  currentPage.value = 1;
  fetchPosts();
};

const handlePageSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  fetchPosts();
};

const handleCurrentPageChange = (page) => {
  currentPage.value = page;
  fetchPosts();
};
const goToCreatePost = () => {
  router.push(`/user/create/forum/`);
};
const goToPostDetail = (postId) => {
  router.push(`/user/forum/${postId}`);
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

const previewImage = (imgUrl) => {
  window.open(imgUrl, '_blank');
};

onMounted(() => {
  fetchPosts();
  fetchTopPosts();
});
</script>

<style scoped>
.forum-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}


.search-bar {

  width: 70%;
  /* 宽度改为100% */
  display: flex;
  /* 使用flex布局 */
  align-items: center;
  /* 垂直居中 */
  gap: 16px;
  /* 按钮和搜索框之间的间距 */
  margin: 0 0 16px 0;
  /* 调整边距 */
}

.create {

  margin-right: 0;
  /* 移除按钮右侧边距 */
}

.carousel {
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
}

.carousel-placeholder {
  width: 100%;
  height: 200px;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
  font-size: 24px;
  border-radius: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 20px 0;
}

.posts-list {
  margin-top: 16px;
}

.post-item {
  margin-bottom: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.post-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.post-main {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 16px;
}

.post-avatar {
  flex-shrink: 0;
}

.post-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.post-title {
  font-weight: bold;
  font-size: 18px;
  color: #333;
}

.post-author {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-text {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  max-height: 4em;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.post-img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #eee;
  transition: transform 0.2s;
}

.post-img:hover {
  transform: scale(1.05);
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

.update-time {
  margin-left: auto;
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
  margin: 24px 0 0 0;
  text-align: center;
}

/* 热榜前三名颜色 */
.color-red {
  color: #FF4D4F;
}

.color-orange {
  color: #FF9800;
}

.color-green {
  color: #409EFF;
}

/* 热榜列表样式优化 */
.el-card .hot-list-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 500;
}

.el-card ul {
  list-style: none;
  padding: 0 24px 24px;
  margin: 0;
}

.el-card li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.el-card li:hover {
  color: #409EFF;
}

.el-card span {
  display: inline-block;
  max-width: 200px;
  /* 限制标题宽度 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
