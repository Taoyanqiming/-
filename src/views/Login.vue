<template>
  <el-row class="login-page">
    <el-col :span="12" class="bg"></el-col>
    <el-col :span="6" :offset="3" class="form">
      <!-- 注册表单 -->
      <el-form ref="form" size="large" autocomplete="off" v-if="isRegister" :model="registerData"
        :rules="registerRules">
        <el-form-item>
          <h1>注册</h1>
        </el-form-item>
        <el-form-item prop="username">
          <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username"></el-input>
        </el-form-item>
        <el-form-item prop="phoneNumber">
          <el-input :prefix-icon="Lock" type="phoneNumber" placeholder="请输入手机号"
            v-model="registerData.phoneNumber"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input :prefix-icon="Lock" type="password" placeholder="请输入密码" v-model="registerData.password"></el-input>
        </el-form-item>
        <el-form-item prop="rePassword">
          <el-input :prefix-icon="Lock" type="password" placeholder="请再次输入密码"
            v-model="registerData.rePassword"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="button" type="primary" auto-insert-space @click="register">注册</el-button>
        </el-form-item>
        <el-form-item class="flex">
          <el-link type="info" :underline="false" @click="isRegister = false; clearRegisterData()">← 返回</el-link>
        </el-form-item>
      </el-form>

      <!-- 登录表单 -->
      <el-form ref="form" size="large" autocomplete="off" v-else :model="loginData" :rules="loginRules">
        <el-form-item>
          <h1>登录</h1>
        </el-form-item>
        <el-form-item prop="phoneNumber">
          <el-input :prefix-icon="User" placeholder="请输入手机号" v-model="loginData.phoneNumber"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input name="password" :prefix-icon="Lock" type="password" placeholder="请输入密码"
            v-model="loginData.password"></el-input>
        </el-form-item>
        <el-form-item class="flex">
          <el-checkbox>记住我</el-checkbox>
          <el-link type="primary" :underline="false">忘记密码？</el-link>
        </el-form-item>
        <el-form-item>
          <el-button class="button" type="primary" auto-insert-space @click="login">登录</el-button>
        </el-form-item>
        <el-form-item class="flex">
          <el-link type="info" :underline="false" @click="isRegister = true; clearLoginData()">注册 →</el-link>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'
import { useUserInfoStore } from '@/stores/userInfo.js';
import { useRouter } from 'vue-router'
import { userLoginService, userRegisterService } from '@/api/users.js'

// 控制注册与登录表单的显示，默认显示注册
const isRegister = ref(false)

// 定义登录数据模型
const loginData = ref({
  phoneNumber: '',
  password: ''
})

// 定义注册数据模型
const registerData = ref({
  phoneNumber: '',
  username: '',
  password: '',
  rePassword: ''
})

// 定义表单校验规则
const loginRules = {
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { min: 11, max: 16, message: '长度为11位非空字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
  ]
}

const registerRules = {
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { min: 11, max: 16, message: '长度为11位非空字符', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
  ],
  rePassword: [
    {
      validator: (rule, value, callback) => {
        if (value === '') {
          callback(new Error('请再次确认密码'))
        } else if (value !== registerData.value.password) {
          callback(new Error('请确保两次输入的密码一致'))
        } else {
          callback()
        }
      }, trigger: 'blur'
    }
  ]
}

// 登录函数
const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore();

// 在 Login.vue 的 login 函数中
const login = async () => {
  try {
    const result = await userLoginService(loginData.value);
    console.log('登录响应:', result); // 打印完整响应结构

    if (result.data && result.data.code === 1) {
      const userData = result.data.data; // 提取实际用户数据
      ElMessage.success('登录成功');

      // 存储Token和用户信息
      tokenStore.setToken(userData.token);

      // 使用解构赋值和默认值，确保所有字段都有值
      userInfoStore.setInfo({
        userId: userData.userId || '',
        userName: userData.userName || userData.phoneNumber || '未设置用户名',
        email: userData.email || '',
        phoneNumber: userData.phoneNumber || '',
        role: userData.role || 'user',
        icon: userData.icon, // 确保有默认头像
      });

      // 统一跳转到用户主页
      router.push('/user/home');
    } else {
      ElMessage.error(result.data.msg || '登录失败，请检查账号密码');
    }
  } catch (error) {
    ElMessage.error('网络请求失败，请重试');
    console.error('登录请求错误:', error);
  }
};
// 注册函数（保持原有逻辑）
const register = async () => {
  let result = await userRegisterService(registerData.value)
  ElMessage.success(result.msg ? result.msg : '注册成功')
}

// 清空数据模型
const clearLoginData = () => {
  loginData.value = {
    phoneNumber: '',
    password: ''
  }
}

const clearRegisterData = () => {
  registerData.value = {
    phoneNumber: '',
    username: '',
    password: '',
    rePassword: ''
  }
}
</script>

<style>
.bg {
  background-image: url("@/assets/login_bg.jpg");
  height: 100vh;
}

.form {
  margin-top: 20vh;
}
</style>