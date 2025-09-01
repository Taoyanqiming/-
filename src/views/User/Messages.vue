<template>
  <div class="messages-container">
    <el-container>
      <el-aside width="220px" class="sidebar">
        <el-menu :default-active="activeMenu" class="el-menu-vertical-demo" @select="handleMenuSelect">
          <el-menu-item index="myMessages">
            <template #title>
              <span>我的消息</span>
              <el-badge :value="unreadCounts.myMessages" :hidden="unreadCounts.myMessages <= 0" />
            </template>
          </el-menu-item>
          <el-menu-item index="replies">
            <template #title>
              <span>回复我的</span>
              <el-badge :value="unreadCounts.replies" :hidden="unreadCounts.replies <= 0" />
            </template>
          </el-menu-item>
          <el-menu-item index="likes">
            <template #title>
              <span>收到的赞</span>
              <el-badge :value="unreadCounts.likes" :hidden="unreadCounts.likes <= 0" />
            </template>
          </el-menu-item>
          <el-menu-item index="orders">
            <template #title>
              <span>订单通知</span>
              <el-badge :value="unreadCounts.orders" :hidden="unreadCounts.orders <= 0" />
            </template>
          </el-menu-item>
          <el-menu-item index="system">
            <template #title>
              <span>系统消息</span>
              <el-badge :value="unreadCounts.system" :hidden="unreadCounts.system <= 0" />
            </template>
          </el-menu-item>
          <el-menu-item index="appoint">
            <template #title>
              <span>预约消息</span>
              <el-badge :value="unreadCounts.appoint" :hidden="unreadCounts.appoint <= 0" />
            </template>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <div class="toolbar">
          <el-button type="primary" @click="markAllAsRead" :disabled="!hasUnread">
            全部标为已读
          </el-button>
          <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page="currentPage" :page-sizes="[10, 20, 30]" :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper" :total="total" v-if="total > 0">
          </el-pagination>
        </div>
        <div class="message-list">
          <el-card class="message-card" v-for="message in currentMessages" :key="message.id"
            :class="{ 'unread-message': message.isRead === 0 }" @click="navigateToSource(message)">

            <div class="message-header">
              <div v-if="message.icon" class="avatar-container">
                <img :src="message.icon" alt="用户头像" class="avatar-image" />
              </div>
              <div class="message-info">
                <div class="message-sender">{{ message.username }}</div>
                <div class="message-content">{{ message.content }}</div>
              </div>
              <div class="message-actions">
                <span class="message-time">{{ formatTime(message.createTime) }}</span>
                <el-button type="text" size="small" @click.stop="deleteMessage(message.id)">
                  <i class="el-icon-delete"></i> 删除
                </el-button>
              </div>
            </div>

          </el-card>

          <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page="currentPage" :page-sizes="[10, 20, 30]" :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper" :total="total" v-if="total > 0">
          </el-pagination>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  getMessagePageService,
  markMessagesAsReadBatchService,
  deleteMessageService,
  getUnreadCountService,
  getCountByTypeService
} from '@/api/message.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

const activeMenu = ref('myMessages')
const loading = ref(false)
const currentMessages = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const tableSelection = ref([])
const unreadCounts = ref({
  myMessages: 0,
  replies: 0,
  likes: 0,
  orders: 0,
  system: 0,
  appoint: 0
})

const messageTypeMap = {
  myMessages: null,
  replies: 1,
  likes: 2,
  orders: 3,
  system: 0,
  appoint: 4
}

const messageIconMap = {
  0: 'el-icon-bell',
  1: 'el-icon-chat-line-round',
  2: 'el-icon-heart',
  3: 'el-icon-shopping-cart',
  4: 'el-icon-date',
  default: 'el-icon-message'
}

const getMessageIconClass = (type) => {
  return messageIconMap[type] || messageIconMap.default
}

const hasUnread = computed(() => {
  return Object.values(unreadCounts.value).some(count => count > 0)
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString()
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const type = messageTypeMap[activeMenu.value]
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      deleteFlag: 0
    }

    if (type !== null) {
      params.type = type
    }
    console.log('获取消息列表参数', params)
    const res = await getMessagePageService(params)
    console.log('获取消息列表', res)
    if (res.data.code === 1) {
      currentMessages.value = res.data.data.records || []

      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '获取消息失败')
    }
  } catch (error) {
    console.error('获取消息列表失败', error)
    ElMessage.error('获取消息列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const fetchUnreadCounts = async () => {
  try {
    const res = await getUnreadCountService()
    console.log('获取未读消息数量', res)
    if (res.data.code === 1) {
      const counts = res.data.data || {}
      unreadCounts.value = {
        myMessages: Object.values(counts).reduce((a, b) => a + b.count, 0),
        replies: counts[1]?.count || 0,
        likes: counts[2]?.count || 0,
        orders: counts[3]?.count || 0,
        system: counts[0]?.count || 0,
        appoint: counts[4]?.count || 0
      }
    }
  } catch (error) {
    console.error('获取未读消息数量失败', error)
  }
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
  currentPage.value = 1
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  fetchMessages()
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchMessages()
}

watch(activeMenu, () => {
  fetchMessages()
})

const markSelectedAsRead = async () => {
  if (tableSelection.value.length === 0) {
    ElMessage.info('请选择要标记的消息')
    return
  }

  const unreadIds = tableSelection.value
    .filter(item => item.isRead === 0)
    .map(item => item.id)

  if (unreadIds.length === 0) {
    ElMessage.info('所选消息均已读')
    return
  }

  try {
    console.log('批量标记为已读的消息ID', unreadIds)
    const res = await markMessagesAsReadBatchService(unreadIds)
    if (res.data.code === 1) {
      ElMessage.success('标记成功')
      fetchMessages()
      fetchUnreadCounts()
      tableSelection.value = []
    } else {
      ElMessage.error(res.msg || '标记失败')
    }
  } catch (error) {
    console.error('标记消息失败', error)
    ElMessage.error('标记消息失败，请稍后再试')
  }
}

const markAllAsRead = async () => {
  // 收集当前页面所有未读消息的ID
  const unreadMessages = currentMessages.value.filter(item => item.isRead === 0)
  if (unreadMessages.length === 0) {
    ElMessage.info('当前页没有未读消息')
    return
  }

  const unreadIds = unreadMessages.map(item => item.messageId)

  try {
    // 调用接口批量标记为已读
    console.log('批量标记为已读的消息ID', unreadIds)
    const res = await markMessagesAsReadBatchService(unreadIds)
    if (res.data.code === 1) {
      ElMessage.success('标记成功')
      // 刷新消息列表和未读计数
      fetchMessages()
      fetchUnreadCounts()
    } else {
      ElMessage.error(res.msg || '标记失败')
    }
  } catch (error) {
    console.error('标记全部消息失败', error)
    ElMessage.error('标记全部消息失败，请稍后再试')
  }
}

const deleteMessage = async (messageId) => {
  ElMessageBox.confirm(
    '确定要删除这条消息吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteMessageService(messageId)
      if (res.data.code === 1) {
        ElMessage.success('删除成功')
        fetchMessages()
        fetchUnreadCounts()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除消息失败', error)
      ElMessage.error('删除消息失败，请稍后再试')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

const navigateToSource = (message) => {
  if (message.sourceModule && message.sourceId) {
    const basePath = message.sourceModule.replace(/^\//, '');
    const postId = message.sourceId;
    if (basePath === 'order') {
      router.push({ path: `/user/mall/order` });
    }
    const targetPath = `/user/${basePath}/${postId}`;
    console.log('跳转到目标路径:', targetPath);
    router.push({ path: targetPath });
  }
};

const handleSelectionChange = (val) => {
  tableSelection.value = val
}

onMounted(() => {
  fetchMessages()
  fetchUnreadCounts()
})
</script>

<style scoped>
/* 头像样式 */
.avatar-container {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  margin-right: 10px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar-icon {
  font-size: 20px;
  color: #909399;
}

.message-card {
  margin-bottom: 10px;
}

/* 消息头部布局 */
.message-header {
  display: flex;
  align-items: flex-start;
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
}

.message-info {
  flex: 1;
  margin-right: 15px;
}

.message-actions {
  min-width: 100px;
  text-align: right;
}

.message-sender {
  font-weight: 500;
  margin-bottom: 5px;
  color: #303133;
}

.message-content {
  color: #606266;
  font-size: 14px;

}

.message-time {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: 5px;
}
</style>