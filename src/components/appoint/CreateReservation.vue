<template>
  <div class="appointment-create-container">
    <!-- 步骤条 -->
    <el-steps :active="0" align-center style="margin-bottom: 20px;">
      <el-step title="待申请" description="填写预约信息"></el-step>
      <el-step title="审批中" description="等待管理员审核"></el-step>
      <el-step title="已完成" description="预约处理完成"></el-step>
    </el-steps>

    <!-- 表单区域 -->
    <el-card class="box-card" style="width: 100%; margin-top: 5px;">
      <template #header>
        <div class="clearfix">
          <span>创建预约</span>
          <el-button type="text" @click="goBack" style="float: right; padding: 3px 0">
            返回
          </el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <!-- 姓名 -->
        <el-form-item label="姓名">
          <el-input v-model="UnY.username" disabled placeholder="暂无姓名信息" />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item label="手机号">
          <el-input v-model="UnY.phoneNumber" disabled placeholder="暂无手机号信息" />
        </el-form-item>

        <!-- 预约日期（修改为 datetime 类型） -->
        <el-form-item label="预约日期" prop="visitDate">
          <el-date-picker v-model="form.visitDate" type="datetime" placeholder="选择日期和时间" value-format="YYYY-MM-DD"
            format="YYYY-MM-DD" />
        </el-form-item>

        <!-- 预约原因 -->
        <el-form-item label="预约原因" prop="visitReason">
          <el-input v-model="form.visitReason" type="textarea" :rows="3" placeholder="请输入预约原因" />
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          提交申请
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { submitAppointmentService } from '@/api/appoint.js';
import { useUserInfoStore } from '@/stores/userinfo.js';

const router = useRouter();
const store = useUserInfoStore();
const loading = ref(false);

// 响应式数据
const UnY = reactive({
  username: store.userInfo.userName || '', // 对应 store 中的 userName
  phoneNumber: store.userInfo.phoneNumber || '',
});

// 修改表单数据类型为字符串（适配后端时间格式）
const form = reactive({
  visitDate: '', // 格式：2025-03-26 00:06:28
  visitReason: '',
  status: 0
});

const rules = reactive({
  // 移除 userName 验证（因字段为 disabled，无需验证）
  visitDate: [{ required: true, message: '请选择日期和时间', trigger: 'change' }],
  visitReason: [{ required: true, message: '请输入预约原因', trigger: 'blur' }]
});

// 监听用户信息加载（修正 watch 目标为 userInfo）
watch(() => store.userInfo, (user) => {
  if (user) {
    UnY.username = user.userName; // 更新姓名
    UnY.phoneNumber = user.phoneNumber; // 更新手机号
  }
}, { deep: true });

// 组件挂载时初始化
onMounted(() => { });

// 提交申请（修正表单验证引用）
const formRef = ref(); // 声明表单引用
const handleSubmit = async () => {
  if (!formRef.value) return; // 确保表单已初始化
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 确保 visitDate 为字符串格式（如后端需要）
        const requestData = {
          ...form,
          visitDate: form.visitDate ? form.visitDate.toString() : null // 可选转换
        };
        console.log("提交数据", requestData);
        const response = await submitAppointmentService(requestData);
        console.log("返回", response);
        if (response.data.code === 1) {
          ElMessage.success('预约申请提交成功，等待审核');
          router.push(`/user/reservation/${response.data.data}`);
        } else {
          ElMessage.error('提交失败：' + response.data.message);
        }
      } catch (error) {
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
</script>

<style scoped>
.appointment-create-container {
  padding: 20px;
}

.action-buttons {
  margin-top: 30px;
  text-align: right;
}
</style>