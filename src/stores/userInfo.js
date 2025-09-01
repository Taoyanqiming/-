import { defineStore } from 'pinia';
import { Icon } from 'vant';
import { ref } from 'vue';

export const useUserInfoStore = defineStore('userInfo', () => {
  const userInfo = ref({
    userId: '',
    userName: '',
    email: '',
    phoneNumber: '',
    role: '',
    icon:'' 

  });


  const setInfo = (newInfo) => {
    userInfo.value = newInfo; // 这里会更新用户信息
    console.log('用户信息已更新，当前用户信息：', userInfo.value);
  }

  const clearInfo = () => {
    userInfo.value = {
      userId: '',
      userName: '',
      email: '',
      phoneNumber: '',
      role: '',
      icon:'' 
    };
    console.log('用户信息已清空，当前用户信息：', userInfo.value);
  }

  return {
    userInfo, setInfo, clearInfo
  };
}, {
  persist: true // 持久化
});
