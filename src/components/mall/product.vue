<template>
  <div class="product-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchKey" placeholder="请输入商品名称" suffix-icon="el-icon-search"
        @keyup.enter="searchProducts"></el-input>
      <el-button @click="searchProducts">搜索</el-button>
    </div>

    <!-- 商品列表 -->
    <el-row :gutter="20">
      <el-col v-for="product in productList" :key="product.id" :span="6">
        <el-card class="product-card" shadow="hover" @click="navigateToDetail(product)" style="margin-bottom: 10px;">
          <div class="card-content">
            <img :src="product.productImageUrl || defaultImg" class="product-image" alt="商品图片" />
            <h3 class="product-name">{{ product.productName || '未命名商品' }}</h3>
            <div class="product-price">
              <span class="current-price">¥{{ product.productPrice.toFixed(2) }}</span>
              <span class="original-price" v-if="product.originalPrice > product.productPrice">
                ¥{{ product.originalPrice.toFixed(2) }}
              </span>
            </div>
            <div class="product-stock">库存: {{ product.productStock || 0 }}</div>
            <el-button v-if="product.productStock > 0" type="primary" size="small"
              @click.stop="navigateToDetail(product)">
              查看详情
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页组件 -->
    <el-pagination background @size-change="handleSizeChange" @current-change="handleCurrentChange"
      :current-page="currentPage" :page-sizes="[8, 16, 24]" :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper" :total="total"></el-pagination>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  getAllProductsService
} from '@/api/mall.js';

const router = useRouter();
const productList = ref([]);
const searchKey = ref('');
const currentPage = ref(1);
const pageSize = ref(8);
const total = ref(0);
const defaultImg = 'https://picsum.photos/200/200';

// 加载商品列表
const loadProducts = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      productName: searchKey.value
    };
    const res = await getAllProductsService(params);
    if (res.data.code === 1) {
      productList.value = res.data.data.records;
      total.value = res.data.data.total;
    }
  } catch (error) {
    console.error('加载商品列表失败:', error);
  }
};

// 搜索商品
const searchProducts = () => {
  currentPage.value = 1;
  loadProducts();
};

// 跳转到商品详情页
const navigateToDetail = (product) => {
  router.push({
    path: `/user/mall/product/info/${product.productId}`,

  });
};

// 添加到购物车
const addToCart = (product) => {
  // 实现添加购物车逻辑
  console.log('添加到购物车:', product);
};

// 分页处理
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  loadProducts();
};

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
  loadProducts();
};

onMounted(() => {
  loadProducts();
});
</script>

<style scoped>
.search-bar {
  margin-left: 90px;
  width: 75%;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 10px;
}

/* 中大屏幕下，搜索栏水平排列 */
@media (min-width: 768px) {
  .search-bar {
    flex-direction: row;
  }
}

.product-image {
  width: 100%;
  /* 占满容器宽度 */
  height: 200px;
  /* 固定高度为200px */
  object-fit: cover;
  /* 图片等比例缩放，裁剪超出部分 */
  border-radius: 4px;
  /* 圆角处理，提升视觉效果 */
  transition: transform 0.3s;
  /* 鼠标悬停动画 */
}

.product-image:hover {
  transform: scale(1.05);
  /* 鼠标悬停时放大5% */
}
</style>