<template>
  <div class="seckill-detail-container">
    <div class="dialog-content">
      <div class="product-image-container">
        <img :src="detailProduct.seckillImageUrl || defaultImg" class="detail-image" alt="秒杀商品大图" />
      </div>
      <div class="product-info-container">
        <h2 class="detail-title">{{ detailProduct.productName || '未命名秒杀商品' }}</h2>
        <div class="detail-price">
          <span class="seckill-price">¥{{ detailProduct.seckillPrice }}</span>
          <span class="original-price">原价¥{{ detailProduct.productPrice }}</span>
        </div>
        <div class="detail-stock">
          <span>库存: {{ detailProduct.seckillStock || 0 }}</span>
        </div>

        <!-- 秒杀时间显示（仅作参考，不影响购买逻辑） -->
        <div class="detail-time" v-if="detailProduct.seckillStartTime && detailProduct.seckillEndTime">
          <span>秒杀时间: </span>
          <span>{{ formatDateTime(detailProduct.seckillStartTime) }} - {{ formatDateTime(detailProduct.seckillEndTime)
          }}</span>
        </div>

        <!-- 库存状态显示 -->
        <div class="detail-status">
          <div v-if="detailProduct.seckillStock > 0" class="status-available">
            <span class="status-text">库存充足,立即抢购</span>
          </div>
          <div v-else class="status-sold-out">
            <span class="status-text">已抢光</span>
          </div>
        </div>

        <div class="detail-description">
          <p>{{ detailProduct.description || '暂无商品描述' }}</p>
        </div>
        <div class="detail-actions">
          <el-input-number v-model="purchaseCount" :min="1" :max="detailProduct.seckillStock || 1" label="购买数量" />
          <el-button type="primary" @click="handlePurchase"
            :disabled="detailProduct.seckillStock <= 0 || purchaseCount.value > detailProduct.seckillStock">
            {{ detailProduct.seckillStock <= 0 ? '已抢光' : '立即购买' }} </el-button>
        </div>
      </div>
    </div>

    <!-- 调试信息 -->
    <div class="debug-info" v-if="detailProduct.seckillId">
      <p>商品ID: {{ detailProduct.seckillId }}</p>
      <p>当前库存: {{ detailProduct.seckillStock || 0 }}</p>
      <p>购买数量: {{ purchaseCount.value }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getSeckillProductInfoService,
  purchaseSeckillProductService,
  purchaseNormalProductService
} from '@/api/mall.js';

// 状态管理
const route = useRoute();
const detailProduct = ref({});
const purchaseCount = ref(1);
const defaultImg = 'https://picsum.photos/200/200';

// 格式化时间函数（仅用于显示）
const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

// 加载商品详情
const loadProductDetail = async () => {
  try {
    const seckillId = route.params.seckillId;
    const res = await getSeckillProductInfoService(seckillId);
    if (res.data.code === 1) {
      detailProduct.value = res.data.data;
    }
  } catch (error) {
    console.error('加载商品详情失败:', error);
  }
};

// 处理购买（仅校验库存）
const handlePurchase = async () => {
  if (!detailProduct.value.seckillId || purchaseCount.value <= 0) {
    ElMessage.error('参数错误，请重试');
    return;
  }

  try {
    // 校验库存
    if (detailProduct.value.seckillStock <= 0) {
      ElMessage.error('商品已抢光');
      return;
    }

    if (purchaseCount.value > detailProduct.value.seckillStock) {
      ElMessage.error(`最多可购买 ${detailProduct.value.seckillStock} 件`);
      return;
    }

    // 调用购买接口
    const seckillCreateDTO = {
      productId: detailProduct.value.productId,
      purchaseQuantity: purchaseCount.value
    };

    const purchaseRes = await purchaseNormalProductService(seckillCreateDTO);
    if (purchaseRes.data.code === 1) {
      console.log('购买成功:', purchaseRes.data.msg);
      ElMessage.success('购买成功，请查看订单');
      // 刷新库存
      await loadProductDetail();
    } else {
      console.error('购买失败:', purchaseRes.data.msg);
      ElMessage.error(purchaseRes.data.msg);
    }
  } catch (error) {
    console.error('网络请求失败:', error);
    ElMessage.error('网络错误，请稍后再试');
  }
};

onMounted(() => {
  loadProductDetail();

  // 监听库存变化
  watch(() => detailProduct.value.seckillStock, () => {
    if (detailProduct.value.seckillStock <= 0) {
      purchaseCount.value = 1;
    }
  }, { deep: true });
});
</script>

<style scoped>
/* 样式调整 */
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
  font-size: 1.8em;
  font-weight: bold;
  color: #ff3300;
  display: flex;
  align-items: center;
  gap: 20px;
}

.seckill-price {
  color: #ff3300;
}

.original-price {
  color: #999;
  text-decoration: line-through;
  font-size: 0.8em;
}

.detail-stock {
  font-size: 1.1em;
  color: #666;
  margin-bottom: 10px;
}

.detail-time {
  font-size: 1.1em;
  color: #666;
  margin-bottom: 10px;
}

.detail-status {
  margin-bottom: 20px;
  padding: 10px;
  border-radius: 4px;
}

.status-available {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.status-sold-out {
  background-color: #ffebee;
  color: #b71c1c;
}

.status-text {
  font-weight: bold;
}

.detail-description {
  flex: 1;
  color: #333;
  line-height: 1.6;
  margin-bottom: 20px;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  padding: 15px 0;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 10px;
}

.debug-info {
  margin-top: 20px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}
</style>