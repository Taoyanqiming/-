<template>
  <div class="seckill-detail-container">
    <div class="dialog-content">
      <div class="product-image-container">
        <img :src="Product.productImageUrl || defaultImg" class="detail-image" alt="商品大图" />
      </div>
      <div class="product-info-container">
        <h2 class="detail-title">{{ Product.productName || '未命名商品' }}</h2>
        <div class="detail-price">
          <span class="seckill-price">¥{{ Product.productPrice }}</span>
        </div>
        <div class="detail-stock">
          <span>库存: {{ Product.productStock || 0 }}</span>
        </div>
        <div class="detail-description">
          <p>{{ Product.description || '暂无商品描述' }}</p>
        </div>
        <div class="detail-actions">
          <el-input-number v-model="purchaseCount" :min="1" :max="Product.productStock || 1"
            label="购买数量"></el-input-number>
          <el-button type="primary" @click="handlePurchase" :disabled="Product.productStock <= 0"
            :class="{ 'disabled-btn': Product.productStock <= 0 }">
            {{ Product.productStock > 0 ? '立即购买' : '已抢光' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import {
  getProductInfoService,
  purchaseNormalProductService
} from '@/api/mall.js';
import { ElMessage } from 'element-plus';

const route = useRoute();
const Product = ref({});
const purchaseCount = ref(1);
const defaultImg = 'https://picsum.photos/200/200';

// 加载商品详情（顶级函数）
const loadProductDetail = async () => {
  try {
    const productId = route.params.productId;
    console.log('加载商品详情，商品ID:', productId);
    const res = await getProductInfoService(productId);
    console.log('获取商品详情:', res);
    if (res.data.code === 1) {
      Product.value = res.data.data;
      console.log('商品详情:', Product.value);
    } else {
      console.error('获取商品详情失败:', res.data.msg);
      ElMessage.error(res.data.msg || '获取商品详情失败');
    }
  } catch (error) {
    console.error('网络请求失败:', error);
    ElMessage.error('网络请求失败，请重试');
  }
};

// 处理购买（顶级函数）
const handlePurchase = async () => {
  if (!Product.value.productId || purchaseCount.value <= 0 || Product.value.productStock <= 0) {
    ElMessage.warning('请选择有效的购买数量或商品库存不足');
    return;
  }

  const purchaseDTO = {
    productId: Product.value.productId,
    quantity: purchaseCount.value
  };

  try {
    const res = await purchaseNormalProductService(purchaseDTO);
    if (res.data.code === 1) {
      console.log('购买成功:', res.data.msg);
      ElMessage.success('购买成功！请尽快完成支付');
      purchaseCount.value = 1;
    } else {
      console.error('购买失败:', res.data.msg);
      ElMessage.error(res.data.msg || '购买失败，请重试');
    }
  } catch (error) {
    console.error('网络请求失败:', error);
    ElMessage.error('网络请求失败，请检查网络连接');
  }
};

// 生命周期钩子（顶级调用）
onMounted(() => {
  loadProductDetail();
});
</script>

<style scoped>
/* 样式保持不变 */
.dialog-content {
  display: flex;
  gap: 30px;
  padding: 30px;
}

.product-image-container {
  flex: 1;
  position: relative;
  max-width: 400px;
}

.detail-image {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.detail-title {
  font-size: 1.5em;
  font-weight: bold;
  margin-bottom: 20px;
}

.detail-price {
  background: #ffebee;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 30px;
}

.disabled-btn {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>