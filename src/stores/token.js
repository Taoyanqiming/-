import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useTokenStore = defineStore('token', () => {
    const token = ref('');
    const setToken = (newToken) => {
        token.value = newToken;
        // 输出当前 token 的值
        console.log('当前设置的 token 值:', token.value);
        // 从 localStorage 中获取存储的 token 并输出
        const storedToken = localStorage.getItem('token.token');
        console.log('从 localStorage 中获取的 token 值:', storedToken);
    };
    const removeToken = () => {
        token.value = '';
        // 输出移除 token 后的 token 值
        console.log('移除 token 后，当前 token 的值:', token.value);
        // 从 localStorage 中获取存储的 token 并输出
        const storedToken = localStorage.getItem('token.token');
        console.log('移除 token 后，从 localStorage 中获取的 token 值:', storedToken);
    };
    return {
        token,
        setToken,
        removeToken
    };
}, {
    persist: {
        storage: localStorage, // 可以选择 localStorage 或 sessionStorage
        paths: ['token'] // 指定要持久化的状态字段
    }
});