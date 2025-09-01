// utils/request.js
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useTokenStore } from '@/stores/token';
import router from '@/router';

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000
});

// 请求拦截器：只添加Token
instance.interceptors.request.use(
  config => {
    const tokenStore = useTokenStore();
    const token = tokenStore.token;
    
    if (token) {
      // 只添加Token
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    return config;
  },
  error => {
    console.error('请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器：处理Token失效
instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const { status } = error.response;
      
      // 401或403状态码表示未授权或权限不足
      if (status === 401 || status === 403) {
        const tokenStore = useTokenStore();
        const userStore = useUserInfoStore();
        
        // 清除Token和用户信息
        tokenStore.removeToken();
        userStore.clearInfo();
        
        // 跳转到登录页
        router.push('/user/login');
        ElMessage.error('登录状态已过期，请重新登录');
      }
    }
    
    return Promise.reject(error);
  }
);

export default instance;