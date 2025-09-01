<template>
  <div class="post-detail-layout">
    <!-- 左侧栏 -->
    <div class="post-sidebar">
      <el-avatar :src="post.icon || defaultAvatar" size="large" class="user-avatar" />
      <div class="username">{{ post.userName || '匿名用户' }}</div>
      <div class="post-stats">
        <el-row :gutter="20">
          <el-col :span="12">
            <div>浏览 {{ post.view || 0 }}</div>
          </el-col>
          <el-col :span="12">
            <div>回复 {{ post.comment || 0 }}</div>
          </el-col>
        </el-row>
      </div>
      <div class="sidebar-actions">
        <el-button @click="toggleLike" :loading="likeLoading" :type="post.userIsLiked ? 'primary' : 'default'" circle>
          <el-icon>
            <Pointer />点赞
          </el-icon>
        </el-button>
        <!-- 快速定位到评论输入框 -->
        <el-button @click="scrollToCommentInput" circle>
          <i class="el-icon-chat-dot-round"></i>评论
        </el-button>
        <el-button @click="toggleFavorite" :loading="favoriteLoading"
          :type="post.userIsFavorited ? 'primary' : 'default'" circle>
          <el-icon>
            <Star />收藏
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="post-main">
      <h1 class="post-title">{{ post.title }}</h1>
      <el-divider />
      <div class="post-content" v-html="post.content"></div>
      <div v-if="post.images && post.images.length" class="post-images">
        <!-- 修改为每张图片独占一行 -->
        <div v-for="(img, idx) in post.images" :key="idx" class="post-image-container">
          <img :src="img" class="post-img" @click="previewImage(img)" />
        </div>
      </div>
      <el-divider />

      <!-- 评论区 -->
      <div class="comments-section">
        <div v-for="comment in comments" :key="comment.commentId" class="comment-row">
          <!-- 左侧：评论者 -->
          <div class="comment-userinfo">
            <el-avatar :src="comment.icon || defaultAvatar" size="small" />
            <div class="comment-username">{{ comment.userName || '匿名用户' }}</div>
          </div>
          <!-- 右侧：内容+操作 -->
          <div class="comment-contentbox">
            <div v-if="comment.answerId !== 0">回复：{{ comment.responder || '用户' }} </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div v-if="comment.replies && comment.replies.length" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.commentId" class="reply-row">
                <el-avatar :src="reply.icon || defaultAvatar" size="mini" />
                <div class="reply-info">
                  <div class="reply-username">{{ reply.userName || '匿名用户' }}</div>
                  <div class="reply-content">{{ reply.content }}</div>
                  <div class="reply-meta">
                    <span>{{ formatDate(reply.createTime) }}</span>
                    <el-button size="mini" type="text" @click="toggleReplyBox(comment)">回复</el-button>
                  </div>
                </div>
                <el-button size="mini" type="text" @click="likeComment(reply.commentId)">
                  <el-icon>
                    <Pointer />点赞
                  </el-icon> {{ reply.likeCount || 0 }}
                </el-button>
              </div>
            </div>
            <div class="comment-meta">
              <span>{{ formatDate(comment.createTime) }}</span>
              <el-button size="mini" type="text" @click="toggleReplyBox(comment)">回复</el-button>
              <el-button size="mini" type="text" @click="likeComment(comment.commentId)">
                <i class="el-icon-thumbs-up"></i> {{ comment.likeCount || 0 }}
              </el-button>
            </div>
            <!-- 回复输入框 -->
            <div v-if="comment.showReplyBox" class="reply-box">
              <el-input v-model="comment.replyContent" type="textarea" :rows="2" placeholder="回复..." />
              <el-button size="mini" type="primary" @click="replyToComment(comment.commentId, comment.replyContent)"
                :loading="comment.replyLoading" :disabled="!comment.replyContent.trim()">回复</el-button>
              <el-button size="mini" type="text" @click="comment.showReplyBox = false">取消</el-button>
            </div>
          </div>
        </div>
        <!-- 分页 -->
        <el-pagination :current-page="commentPage" :page-size="commentPageSize" :total="totalComments"
          @current-change="handleCommentPageChange" layout="prev, pager, next" />
        <!-- 评论输入框 -->
        <div class="comment-inputbox" ref="commentInput">
          <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="写下你的评论..." />
          <el-button type="primary" @click="submitComment" :loading="commentLoading"
            :disabled="!commentContent.trim()">发表评论</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  getPostByIdService,
  addCommentService,
  addCommentLikeService,
  addLikeService,
  addFavoriteService,
  deleteCommentService,
  getCommentsByPostIdService
} from '@/api/forum.js';

import defaultAvatar from '@/assets/default.png';

const route = useRoute(); // 获取当前路由信息
const router = useRouter(); // 路由导航
const commentInput = ref(null); // 评论输入框ref，用于滚动定位

// 响应式数据
const postId = computed(() => route.params.postId); // 从路由参数获取postId
const post = ref({}); // 帖子详情数据
const comments = ref([]); // 评论列表
const isLoading = ref(false); // 加载状态
const isLiked = ref(false); // 帖子是否已点赞
const isFavorite = ref(false); // 帖子是否已收藏
const likeLoading = ref(false); // 点赞按钮加载状态
const favoriteLoading = ref(false); // 收藏按钮加载状态
const commentLoading = ref(false); // 发表评论加载状态
const commentContent = ref(''); // 主评论输入内容
const commentPage = ref(1); // 评论分页页码
const commentPageSize = ref(10); // 每页评论数
const totalComments = ref(0); // 总评论数
// 工具函数：格式化时间
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};
// 图片预览：新窗口打开图片
const previewImage = (imgUrl) => {
  window.open(imgUrl, '_blank');
};
// 滚动到评论输入框（用于点击按钮快速定位)
const scrollToCommentInput = () => {
  nextTick(() => {
    if (commentInput.value) {
      commentInput.value.$el.scrollIntoView({ behavior: 'smooth' });
    }
  });
};
// 监听postId变化（用于切换帖子时重新加载数据）
watch(postId, (newId) => {
  if (!newId) return;
  fetchPostDetail(newId);
  fetchComments(newId);
});

onMounted(() => {
  if (postId.value) {
    fetchPostDetail(postId.value);
    fetchComments(postId.value);
  }
});

const fetchPostDetail = async (id) => {
  isLoading.value = true;
  try {
    const response = await getPostByIdService(id);
    console.log("帖子信息", response);
    post.value = response.data.data || {};
  } catch (error) {
    ElMessage.error('网络请求失败，请检查网络');
  } finally {
    isLoading.value = false;
  }
};

const fetchComments = async (id) => {
  try {
    const params = {
      postId: id,
      page: commentPage.value,
      pageSize: commentPageSize.value,
    };
    const response = await getCommentsByPostIdService(params);
    console.log("评论信息", response);
    comments.value = response.data.data.records || [];
    totalComments.value = response.data.data.total || 0;
    comments.value.forEach(comment => {
      comment.showReplyBox = false;
      comment.replyContent = '';
      comment.replyLoading = false;
    });
  } catch (error) {
    ElMessage.error('加载评论失败，请重试');
  }
};

// 帖子点赞/取消点赞
const toggleLike = async () => {
  if (!postId.value) return; // 确保postId存在
  likeLoading.value = true; // 按钮加载状态
  try {
    const likeDTO = { postId: postId.value }; // 点赞参数（假设后端需要postId）
    await addLikeService(likeDTO); // 调用点赞接口
    // 更新本地状态
    isLiked.value = !isLiked.value; // 切换点赞状态
    post.value.liked = isLiked.value
      ? (post.value.liked || 0) + 1 // 点赞数+1
      : Math.max(0, post.value.liked - 1); // 取消点赞时不小于0
    post.value.userIsLiked = isLiked.value; // 更新VO中的用户点赞状态
    ElMessage.success(isLiked.value ? '点赞成功' : '已取消点赞'); // 提示用户
  } catch (error) {
    ElMessage.error('操作失败，请重试');
  } finally {
    likeLoading.value = false; // 结束加载状态
  }
};

const toggleFavorite = async () => {
  if (!postId.value) return;
  favoriteLoading.value = true;
  try {
    const favoriteDTO = {
      postId: postId.value,
    };
    await addFavoriteService(favoriteDTO);
    isFavorite.value = !isFavorite.value;
    post.value.favorite = isFavorite.value ? (post.value.favorite || 0) + 1 : Math.max(0, post.value.favorite - 1);
    post.value.userIsFavorited = isFavorite.value;
    ElMessage.success(isFavorite.value ? '收藏成功' : '已取消收藏');
  } catch (error) {
    ElMessage.error('操作失败，请重试');
  } finally {
    favoriteLoading.value = false;
  }
};
//评论点赞
const likeComment = async (commentId) => {
  const comment = comments.value.find(c => c.commentId === commentId);
  if (!comment) return;
  comment.isLiked = !comment.isLiked;
  try {
    const likeCommentDTO = {
      commentId,
    };
    await addCommentLikeService(likeCommentDTO);
    comment.likeCount = comment.isLiked ? (comment.likeCount || 0) + 1 : Math.max(0, comment.likeCount - 1);
  } catch (error) {
    ElMessage.error('操作失败，请重试');
    comment.isLiked = !comment.isLiked;
  }
};

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容');
    return;
  }
  commentLoading.value = true;
  try {
    const commentDTO = {
      postId: postId.value,
      parentId: 0, // 回复帖子时parentId为0
      answerId: 0, // 回复帖子时answerId为0
      content: commentContent.value.trim(),
      createTime: new Date().toISOString(),
    };

    await addCommentService(commentDTO); // 调用统一接口
    ElMessage.success('评论成功');
    commentContent.value = '';
    await fetchComments(postId.value);
    post.value.comment = (post.value.comment || 0) + 1;
  } catch (error) {
    ElMessage.error('网络请求失败，请检查网络');
  } finally {
    commentLoading.value = false;
  }
};
// 切换回复输入框显示状态
const toggleReplyBox = (comment) => {
  comment.showReplyBox = !comment.showReplyBox;
  if (!comment.showReplyBox) comment.replyContent = '';
};
//回复评论
const replyToComment = async (commentId, content) => {
  if (!content.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  const comment = comments.value.find(c => c.commentId === commentId);
  if (!comment) return;

  comment.replyLoading = true;
  try {
    const replyDTO = {
      postId: postId.value,
      parentId: 1,
      answerId: comment.commentId, // 回复的目标评论ID
      content: content.trim(),
      createTime: new Date().toISOString(),
    };

    // 调用评论接口（假设统一使用 addCommentService，通过参数区分回复类型）
    await addCommentService(replyDTO); // 统一接口，参数决定是回复帖子还是评论

    ElMessage.success('回复成功');
    comment.showReplyBox = false;
    comment.replyContent = '';
    await fetchComments(postId.value); // 重新加载评论列表
  } catch (error) {
    ElMessage.error('网络请求失败，请检查网络');
    comment.isLiked = !comment.isLiked; // 失败时回滚状态
  } finally {
    comment.replyLoading = false;
  }
};

const handleCommentPageChange = async (page) => {
  commentPage.value = page;
  await fetchComments(postId.value);
};
</script>

<style scoped>
.post-detail-layout {
  display: flex;
  gap: 32px;
  background: #f5f7fa;
  min-height: 100vh;
  padding: 32px;
}

.post-sidebar {
  width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  /* 原有背景色，可保留作为备用 */
  border-radius: 12px;
  padding: 24px 0;
  box-shadow: 0 2px 8px #eee;

  /* 新增背景图片样式 */
  background-image: url('@/assets/paopao.png');

  /* 图片覆盖整个容器 */
  background-position: center;
  /* 图片居中 */
  background-repeat: repeat;
  /* 不重复显示 */
  color: #fff;
  /* 文字颜色设为白色，确保在图片上可见 */
}

.user-avatar {
  margin-bottom: 12px;
}

.username {
  font-weight: bold;
  margin-bottom: 16px;
}

.post-stats {
  margin-bottom: 24px;
  color: #888;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-main {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px #eee;
}

.post-title {
  font-size: 2rem;
  margin-bottom: 16px;
}

.post-content {
  margin-bottom: 16px;
}

/* 修改图片容器样式 */
.post-images {
  display: block;
  margin-bottom: 16px;
}

.post-image-container {
  margin-bottom: 16px;
}

.post-img {
  display: block;
  width: 100%;
  max-width: 100%;
  border-radius: 8px;
  cursor: pointer;
  object-fit: contain;
  height: auto;
}

.comments-section {
  margin-top: 32px;
}

.comment-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
}

.comment-userinfo {
  width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.comment-username {
  margin-top: 8px;
  font-size: 14px;
  color: #333;
}

.comment-contentbox {
  flex: 1;
  background: #fafbfc;
  border-radius: 8px;
  padding: 16px;
  margin-left: 16px;
}

.comment-content {
  font-size: 15px;
  margin-bottom: 8px;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #aaa;
  font-size: 13px;
}

.reply-list {
  margin-top: 8px;
}

.reply-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.reply-info {
  margin-left: 8px;
  flex: 1;
}

.reply-username {
  font-size: 13px;
  font-weight: bold;
}

.reply-content {
  font-size: 13px;
  margin: 2px 0;
}

.reply-meta {
  font-size: 12px;
  color: #bbb;
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-box {
  margin-top: 8px;
}

.comment-inputbox {
  margin-top: 32px;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
</style>
