<template>
  <div class="reservation-container">
    <!-- 搜索栏 -->
    <el-row class="search-bar">
      <el-col :span="12">
        <el-input v-model="searchKeyword" placeholder="输入预约原因搜索" @keyup.enter="fetchAppointments" />
      </el-col>
      <el-col :span="12">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" @change="fetchAppointments" />
      </el-col>
    </el-row>

    <!-- 预约列表（渲染指定字段） -->
    <el-table :data="appointments" stripe empty-text="暂无预约记录" @row-click="handleRowClick" v-loading="loading">

      <!-- 渲染 userName -->
      <el-table-column prop="username" label="预约人" width="120" />
      <!-- 渲染 userPhone -->
      <el-table-column prop="phoneNumber" label="联系电话" width="120" />

      <!-- 渲染 visitDate（已格式化） -->
      <el-table-column prop="visitDate" label="预约日期" width="180" />
      <!-- 渲染 visitReason -->
      <el-table-column prop="visitReason" label="预约原因" min-width="200" />
      <!-- 渲染 status（转换为状态文本） -->
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          {{ formatStatus(scope.row.status) }}
        </template>
      </el-table-column>
      <!-- 渲染 createTime（已格式化） -->
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <!-- 操作栏 -->
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" type="primary" @click.stop="openDetail(scope.row.appointmentId)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情/编辑弹窗 -->
    <AppointmentDetail ref="detailRef" @update-success="fetchAppointments" @create-success="fetchAppointments" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAppointmentListService } from '@/api/appoint.js';

const router = useRouter();

// 定义 appointments 列表存储数据
const appointments = ref([]);
const loading = ref(false);
const searchKeyword = ref('');
const dateRange = ref([]);

// 定义日期格式化函数
const formatDate = (dateStr) => {
  if (!dateStr) return '';

  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 格式化状态函数
const formatStatus = (status) => {
  const statusMap = {
    0: '待申请',
    1: '待审批',
    2: '已通过',
    3: '已拒绝',
    4: '已完成'
  };
  return statusMap[status] || '未知状态';
};

// 获取预约列表
const fetchAppointments = async () => {
  loading.value = true;

  try {
    const appointmentDTO = {
      visitReason: searchKeyword.value,
      visitDate: dateRange.value[0] || '',
      status: dateRange.value[1] || ''
    };

    const response = await getAppointmentListService(appointmentDTO);
    if (response.data.code === 1) {
      // 格式化日期数据
      appointments.value = response.data.data.map(item => ({
        ...item,
        visitDate: formatDate(item.visitDate),
        createTime: formatDate(item.createTime)
      }));
    } else {
      ElMessage.error('获取预约列表失败：' + response.data.message);
    }
  } catch (error) {
    console.error('获取预约列表异常:', error);
    ElMessage.error('网络请求失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 打开详情弹窗
const openDetail = (appointmentId) => {
  router.push(`/user/reservation/${appointmentId}`);
};

// 行点击事件
const handleRowClick = (row) => {
  openDetail(row.appointmentId);
};

// 组件挂载后获取数据
onMounted(() => {
  fetchAppointments();
});
</script>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}

.reservation-container {
  padding: 20px;
}
</style>