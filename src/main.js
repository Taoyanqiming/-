import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './assets/main.scss';

import router from '@/router';
import { createApp } from 'vue';
import App from './App.vue';
import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

const app = createApp(App);
const pinia = createPinia();

// 使用持久化插件
pinia.use(piniaPluginPersistedstate);

app.use(pinia);
app.use(ElementPlus);
app.use(router);
app.mount('#app');

export { pinia }; // 导出 pinia 实例