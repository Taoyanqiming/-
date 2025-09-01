<template>
  <div class="user-profile-container">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <span>用户信息</span>
          <el-button style="float: right; padding: 3px 0" type="text" @click="startEdit" v-if="!editMode">
            编辑
          </el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" class="demo-ruleForm">
        <!-- 基本信息 -->

        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="!editMode"></el-input>
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" :disabled="!editMode"></el-input>
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="formData.phoneNumber" disabled></el-input>
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="formData.role" disabled placeholder="请选择角色">
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="普通用户" value="user"></el-option>
          </el-select>
        </el-form-item>

        <!-- 详情信息 -->
        <el-form-item label="出生日期" prop="birthday">
          <el-date-picker v-model="formData.birthday" type="date" placeholder="选择日期"
            :disabled="!editMode"></el-date-picker>
        </el-form-item>

        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="formData.sex" :disabled="!editMode">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
            <el-radio :label="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" :disabled="!editMode"></el-input>
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="formData.idCard" :disabled="!editMode"></el-input>
        </el-form-item>

        <el-form-item label="联系地址" prop="address">
          <el-input v-model="formData.address" :disabled="!editMode" type="textarea"></el-input>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item v-if="editMode">
          <el-button type="primary" @click="submitForm">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue';
import { getUserInfo, updateUserService, updateDetailService } from '@/api/users.js';
import { ElMessage } from 'element-plus';

// 表单引用和数据
const formRef = ref(null);
const formData = reactive({
  username: '',
  email: '',
  phoneNumber: '',
  role: '',
  birthday: '',
  sex: 0,
  realName: '',
  idCard: '',
  address: ''
});

// 保存原始数据，用于取消编辑时恢复
const originalData = reactive({});

// 表单验证规则
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
});

// 编辑模式和加载状态
const editMode = ref(false);
const loading = ref(false);

// 生命周期钩子：组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo();
});

// 获取用户信息
// 在获取数据后转换日期格式
const fetchUserInfo = async () => {
  loading.value = true;
  try {
    const response = await getUserInfo();
    if (response.data.code === 1) {
      const userData = response.data.data;

      // 转换字符串日期为 Date 对象
      if (userData.birthday) {
        userData.birthday = new Date(userData.birthday);
      }

      Object.assign(formData, userData);
      Object.assign(originalData, toRaw(userData));
    } else {
      ElMessage.error(response.data.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息出错:', error);
    ElMessage.error('网络请求失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 开始编辑，保存当前数据
const startEdit = () => {
  // 保存当前数据作为原始数据（使用toRaw避免响应式引用）
  Object.assign(originalData, toRaw(formData));
  editMode.value = true;
};

// 提交表单
const submitForm = async () => {
  await formRef.value.validate((valid) => {
    if (!valid) return false;

    // 准备提交数据
    const userDTO = {
      username: formData.username,
      email: formData.email
    };

    const UserDetailsDTO = {
      birthday: formData.birthday,
      sex: Number(formData.sex),
      realName: formData.realName,
      idCard: formData.idCard,
      address: formData.address
    };

    // 并发调用两个更新接口
    Promise.all([
      updateUserService(userDTO),
      updateDetailService(UserDetailsDTO)
    ])
      .then(([userResponse, detailResponse]) => {
        // 检查两个接口是否都成功
        const isUserSuccess = userResponse.data.code === 1;
        const isDetailSuccess = detailResponse.data.code === 1;

        if (isUserSuccess && isDetailSuccess) {
          // 保存成功后更新原始数据
          Object.assign(originalData, toRaw(formData));
          // 关闭编辑模式并提示成功
          editMode.value = false;
          ElMessage.success('用户信息更新成功');
        } else {
          const userMsg = userResponse.data.message || '更新基本信息失败';
          const detailMsg = detailResponse.data.message || '更新详情信息失败';
          ElMessage.error(`${isUserSuccess ? '' : userMsg} ${isDetailSuccess ? '' : detailMsg}`);
        }
      })
      .catch((error) => {
        console.error('更新用户信息出错:', error);
        ElMessage.error('网络请求失败，请稍后重试');
      });
  });
};

// 取消编辑，恢复原始数据
const cancelEdit = () => {
  // 恢复原始数据
  Object.assign(formData, originalData);
  editMode.value = false;
};
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.box-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.demo-ruleForm {
  margin-top: 20px;
}
</style>