<template>

  <el-row :gutter="20">
    <!-- 左侧导航栏 -->
    <el-col :span="6">
      <el-menu :default-active="activeCategory" class="el-menu-vertical-demo" @select="handleCategoryChange">
        <el-menu-item index="normal">普通商品</el-menu-item>
        <el-menu-item index="seckill">秒杀商品</el-menu-item>
        <el-menu-item index="order">我的订单</el-menu-item>
      </el-menu>
    </el-col>
    <!-- 右侧商品列表 -->
    <el-col :span="18">
      <router-view />
    </el-col>
  </el-row>


</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const activeCategory = ref('normal'); // 初始化默认选中普通商品

// 导航栏点击事件处理
const handleCategoryChange = (key) => {
  activeCategory.value = key; // 更新选中状态
  switch (key) {
    case 'normal':
      router.push({ path: '/user/mall/product' }); // 跳转到普通商品列表
      break;
    case 'seckill':
      router.push({ path: '/user/mall/seckProduct' }); // 跳转到秒杀商品列表
      break;
    case 'order':
      router.push({ path: '/user/mall/order' }); // 跳转到创建订单页面
      break;
  }
};

// 初始化时默认加载普通商品页面
onMounted(() => {
  router.push({ path: '/user/mall/product' });
});
</script>

<style scoped></style>