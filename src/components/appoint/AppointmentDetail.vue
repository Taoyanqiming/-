<template>
  <div class="appointment-detail-container">
    <!-- 步骤条 -->
    <el-steps :active="getActiveStep" align-center style="margin-bottom: 20px;">
      <el-step title="待申请" description="创建预约申请"></el-step>
      <el-step title="审批中" description="等待管理员审核"></el-step>
      <el-step title="已完成" description="预约处理完成"></el-step>
    </el-steps>

    <!-- 表单区域 -->
    <el-card class="box-card" style="width: 100%; margin-top: 5px;">
      <template #header>
        <div class="clearfix">
          <span>{{ isCreate ? '创建预约' : '预约详情' }}</span>
          <el-button v-if="canEdit && !isCreate" type="text" @click="toggleEditMode"
            style="float: right; padding: 3px 0">
            {{ editMode ? '取消编辑' : '修改预约' }}
          </el-button>
          <el-button v-if="isCreate" type="text" @click="goBack" style="float: right; padding: 3px 0">
            返回
          </el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" :disabled="!canEdit || !editMode">
        <!-- 可见预约ID -->
        <el-form-item label="预约ID" v-if="!isCreate">
          <el-input v-model="form.appointmentId" disabled />
        </el-form-item>

        <!-- 姓名（从用户信息获取，不可编辑） -->
        <el-form-item label="姓名">
          <el-input v-model="form.username" disabled placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.phoneNumber" disabled placeholder="请输入姓名" />
        </el-form-item>
        <!-- 预约日期 -->
        <el-form-item label="预约日期" prop="visitDate">
          <el-date-picker v-model="form.visitDate" type="date" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>

        <!-- 预约原因 -->
        <el-form-item label="预约原因" prop="visitReason">
          <el-input v-model="form.visitReason" type="textarea" :rows="3" placeholder="请输入预约原因" />
        </el-form-item>

        <!-- 状态显示 -->
        <el-form-item label="状态">
          <el-tag :type="getStatusType(form.status)">{{ formatStatus(form.status) }}</el-tag>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="action-buttons" v-if="(canEdit && editMode) || isCreate">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          {{ isCreate ? '创建预约' : '保存修改' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  getAppointmentByIdService,
  updateAppointmentService,
  submitAppointmentService
} from '@/api/appoint.js';
import { useUserInfoStore } from '@/stores/userinfo.js';

const router = useRouter();
const route = useRoute();
const store = useUserInfoStore();
const formRef = ref(null);
// 响应式数据
const appointmentId = computed(() => route.params.appointmentId);
const form = reactive({
  appointmentId: 0,

  username: '',
  phoneNumber: '',
  visitDate: '',
  visitReason: '',
  status: 1 // 0: 待申请（创建时），1: 审批中，2: 已通过，3: 已拒绝，4: 已完成
});
const rules = reactive({
  visitDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  visitReason: [{ required: true, message: '请输入预约原因', trigger: 'blur' }]
});
const loading = ref(false);
const canEdit = ref(false); // 是否允许编辑（状态为1时）
const isCreate = ref(false);
const editMode = ref(false); // 是否处于编辑模式

// 获取用户信息
const currentUser = computed(() => store.user);

// 状态码转文本
const formatStatus = (status) => {
  const statusMap = {
    0: '待申请', // 创建时的初始状态
    1: '审批中',
    2: '已通过',
    3: '已拒绝',
    4: '已完成'
  };
  return statusMap[status] || '未知状态';
};

// 获取状态标签样式
const getStatusType = (status) => {
  const typeMap = {
    0: 'info', // 待申请为普通状态
    1: 'warning', // 审批中为警告
    2: 'success', // 已通过为成功
    3: 'danger', // 已拒绝为危险
    4: 'info' // 已完成为普通
  };
  return typeMap[status] || 'info';
};

// 获取当前激活的步骤
const getActiveStep = computed(() => {
  // 先判断是否是创建预约模式
  if (isCreate.value) {
    return 0; // 创建时固定显示第一步“待申请”
  }

  // 非创建模式，根据状态判断
  const status = parseInt(form.status);
  switch (status) {
    case 1:
      return 1; // 审批中（第二步）
    case 2:
    case 3:
    case 4:
      return 2; // 已通过/已拒绝/已完成（第三步）
    default:
      return 0; // 其他异常状态默认显示第一步
  }
});

// 重置表单
const resetForm = () => {
  form.appointmentId = 0;
  form.userId = currentUser.value?.id || 0;
  form.username = currentUser.value?.name || '';
  form.visitDate = '';
  form.visitReason = '';
  form.status = 0; // 创建时默认状态为待申请（0）
  canEdit.value = isCreate.value;
  editMode.value = isCreate.value;
};

// 切换编辑模式
const toggleEditMode = () => {
  editMode.value = !editMode.value;
};

// 获取预约详情
const fetchAppointmentDetail = async (id) => {
  loading.value = true;

  try {
    const response = await getAppointmentByIdService(id);
    console.log('获取预约详情:', response);
    if (response.data.code === 1) {
      const data = response.data.data;
      console.log('form.status:', data.status);

      // 确保状态值被正确解析
      if (data.status !== undefined) {
        data.status = parseInt(data.status);
      }

      // 格式化日期
      data.visitDate = data.visitDate ? data.visitDate.split('T')[0] : '';

      // 复制数据到表单
      Object.assign(form, data);

      // 判断是否可编辑（状态为1时允许修改，假设审批中允许修改）
      canEdit.value = data.status === 1;
      editMode.value = false; // 默认不处于编辑模式
      isCreate.value = false;
    } else {
      ElMessage.error('获取预约详情失败：' + response.data.message);
      router.back(); // 失败时返回上一页
    }
  } catch (error) {
    console.error('获取详情失败:', error);
    ElMessage.error('网络请求失败，请重试');
    router.back();
  } finally {
    loading.value = false;
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return; // 确保表单已初始化
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        let response;
        if (isCreate.value) {
          // 创建预约：状态为待申请（0）
          response = await submitAppointmentService({
            visitDate: form.visitDate,
            visitReason: form.visitReason,
            status: 1 // 初始状态为待申请
          });
        } else {
          // 修改预约：保持原有状态，仅允许修改审批中的记录（status=1）
          response = await updateAppointmentService({
            appointmentId: form.appointmentId,
            visitDate: form.visitDate,
            visitReason: form.visitReason,
            status: 1 // 不修改状态，仅修改其他信息
          });
        }
        if (response.data.code === 1) {
          ElMessage.success(isCreate.value ? '预约创建成功（待申请）' : '修改成功');
          router.push('/user/reservation'); // 跳转到列表页
        } else {
          ElMessage.error(isCreate.value ? '创建失败' : '修改失败');
        }
      } catch (error) {
        console.error(isCreate.value ? '创建预约失败' : '修改预约失败', error);
        ElMessage.error('网络请求失败，请重试');
      } finally {
        loading.value = false;
      }
    }
  });
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 监听路由参数变化
watch(appointmentId, (newId) => {
  if (newId) {
    // 路由参数存在时，加载预约详情
    fetchAppointmentDetail(Number(newId));
  } else {
    // 参数不存在时，重置为创建模式（待申请状态）
    isCreate.value = true;
    resetForm();
  }
});

// 组件挂载时初始化
onMounted(() => {
  if (appointmentId.value) {
    fetchAppointmentDetail(Number(appointmentId.value));
  } else {
    isCreate.value = true;
    resetForm();
  }
});
</script>

<style scoped>
.appointment-detail-container {
  padding: 20px;
}

.action-buttons {
  margin-top: 30px;
  text-align: right;
}

/* 修改步骤条样式 */
:deep(.el-steps) {
  --el-step-icon-size: 24px;
  /* 调整图标大小 */
}
</style>