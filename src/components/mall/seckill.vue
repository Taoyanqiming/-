<template>
  <div class="seckill-container">
    <!-- 全局倒计时横幅 -->
    <div class="global-countdown" v-if="nextSession">
      <span class="banner-title">距离下一场秒杀开始还有</span>
      <span class="timer-number">{{ nextHours }}:</span>
      <span class="timer-number">{{ nextMinutes }}:</span>
      <span class="timer-number">{{ nextSeconds }}</span>
      <span class="session-info">（{{ nextSession.name }}）</span>
    </div>

    <!-- 秒杀场次切换 -->
    <div class="session-buttons">
      <el-button v-for="session in seckillSessions" :key="session.id"
        :class="{ 'active': activeSession === session.id }" @click="handleSessionClick(session)">
        {{ session.name }}
        <span v-if="isCurrentSession(session)" class="current-tag">进行中</span>
      </el-button>
    </div>

    <!-- 秒杀倒计时 -->
    <div class="seckill-timer" v-if="currentSession">
      <span class="session-title">{{ currentSession.name }}</span>
      <span class="timer-text">剩余时间：</span>
      <span class="timer-number">{{ hours }}:</span>
      <span class="timer-number">{{ minutes }}:</span>
      <span class="timer-number">{{ seconds }}</span>
    </div>

    <!-- 秒杀商品列表 -->
    <el-row :gutter="20">
      <el-col v-for="product in productList" :key="product.seckillId" :span="6">
        <el-card class="seckill-card" shadow="hover" @click="handleProductClick(product)">
          <div class="card-badge">
            <span class="seckill-badge">限时秒杀</span>
            <span class="discount-badge" v-if="product.discountRate">
              {{ product.discountRate * 10 }}折
            </span>
          </div>
          <img :src="product.seckillImageUrl || defaultImg" class="product-image" alt="秒杀商品图片" />
          <div class="card-content">
            <h3 class="product-name">
              {{ product.productName || '未命名秒杀商品' }}

            </h3>
            <span class="stock-info" v-if="product.seckillStock !== undefined">限量: {{ product.seckillStock }}</span>
            <div class="price-info">
              <span class="seckill-price">¥{{ product.seckillPrice.toFixed(2) }}</span>
              <span class="original-price" v-if="product.productPrice !== undefined">¥{{ product.productPrice.toFixed(2)
              }}</span>
            </div>
            <el-button :disabled="product.seckillStock <= 0 || !isSeckillActive" type="primary" size="small"
              @click.stop="purchaseSeckill(product)">
              {{ product.seckillStock <= 0 ? '已抢光' : (isSeckillActive ? '立即抢购' : '未开始') }} </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页组件 -->
    <el-pagination background @size-change="handleSizeChange" @current-change="handleCurrentChange"
      :current-page="currentPage" :page-sizes="[8, 16, 24]" :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper" :total="total">
    </el-pagination>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue';
import { getSeckillProductsService } from '@/api/mall.js';

// 状态管理
const productList = ref([]);
const currentPage = ref(1);
const pageSize = ref(8);
const total = ref(0);
const defaultImg = 'https://picsum.photos/200/200';
const timer = ref(null);
const hours = ref('00');
const minutes = ref('00');
const seconds = ref('00');

// 全局倒计时变量
const nextSession = ref(null);
const nextHours = ref('00');
const nextMinutes = ref('00');
const nextSeconds = ref('00');
const nextTimer = ref(null);

// 格式化时间函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
};

const sessionTimes = ['08:00', '11:00', '13:00', '15:00', '17:00'];
const seckillSessions = ref([]);
const activeSession = ref(1);

// 计算当前场次
const currentTime = computed(() => new Date());
const currentSession = computed(() => {
  const now = currentTime.value;
  return seckillSessions.value.find(session => {
    const start = new Date(session.startTime);
    const end = new Date(session.endTime);
    return now.getTime() >= start.getTime() && now.getTime() <= end.getTime();
  });
});

// 判断是否在秒杀活动期间
const isSeckillActive = computed(() => {
  const now = currentTime.value;
  const active = seckillSessions.value.find(s => s.id === activeSession.value);

  if (!active) return false;

  const startTime = new Date(active.startTime);
  const endTime = new Date(active.endTime);

  return now.getTime() >= startTime.getTime() && now.getTime() <= endTime.getTime();
});

// 判断是否为当前进行中的场次
const isCurrentSession = (session) => {
  const now = currentTime.value;
  const startTime = new Date(session.startTime);
  const endTime = new Date(session.endTime);

  return now.getTime() >= startTime.getTime() && now.getTime() <= endTime.getTime();
};

// 更新场次时间
const updateSessionTimes = () => {
  const today = new Date();
  seckillSessions.value = sessionTimes.map((time, index) => {
    const sessionDate = new Date(today);
    const [hour, minute] = time.split(':').map(Number);
    sessionDate.setHours(hour, minute, 0, 0);

    const startTime = formatDateTime(sessionDate);
    const endTime = formatDateTime(new Date(sessionDate.getTime() + 3600000));

    return {
      id: index + 1,
      name: `${time}场`,
      startTime,
      endTime,
      localStartTime: formatDateTime(sessionDate),
      localEndTime: formatDateTime(new Date(sessionDate.getTime() + 3600000))
    };
  });
};

// 更新下一场次信息
const updateNextSession = () => {
  if (seckillSessions.value.length === 0) return;

  const now = currentTime.value;
  const next = seckillSessions.value.find(session => {
    const start = new Date(session.startTime);
    return start.getTime() > now.getTime();
  });

  nextSession.value = next || seckillSessions.value[0]; // 无下一场时显示最早的场次

  // 计算倒计时
  const nextStartTime = new Date(nextSession.value.startTime);
  const diff = nextStartTime - now;

  if (diff <= 0) {
    nextHours.value = nextMinutes.value = nextSeconds.value = '00';
    return;
  }

  nextHours.value = Math.floor(diff / 3600000).toString().padStart(2, '0');
  nextMinutes.value = Math.floor((diff % 3600000) / 60000).toString().padStart(2, '0');
  nextSeconds.value = Math.floor((diff % 60000) / 1000).toString().padStart(2, '0');
};

// 加载秒杀商品列表
const loadProducts = async (sessionId = null) => {
  try {
    const targetSession = sessionId
      ? seckillSessions.value.find(s => s.id === sessionId)
      : seckillSessions.value.find(s => s.id === activeSession.value);

    if (!targetSession) {
      productList.value = [];
      total.value = 0;
      return;
    }

    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      startTime: targetSession.startTime,
      endTime: targetSession.endTime
    };

    const res = await getSeckillProductsService(params);

    if (res.data.code === 1) {
      productList.value = res.data.data.records;
      total.value = res.data.data.total;

      if (productList.value.length > 0) {
        startTimer();
      } else {
        stopTimer();
        hours.value = minutes.value = seconds.value = '00';
      }
    } else {
      productList.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('加载秒杀商品列表失败:', error);
    productList.value = [];
    total.value = 0;
  }
};

// 场次切换处理
const handleSessionClick = (session) => {
  activeSession.value = session.id;
  currentPage.value = 1;
  loadProducts(session.id);
  updateNextSession();
};

// 开始倒计时
const startTimer = () => {
  stopTimer();
  const active = seckillSessions.value.find(s => s.id === activeSession.value);

  if (!active) return;

  const endTime = new Date(active.endTime);
  timer.value = setInterval(() => {
    const now = currentTime.value;
    const diff = endTime - now;

    if (diff <= 0) {
      stopTimer();
      hours.value = minutes.value = seconds.value = '00';
      loadProducts();
      return;
    }

    hours.value = Math.floor(diff / 3600000).toString().padStart(2, '0');
    minutes.value = Math.floor((diff % 3600000) / 60000).toString().padStart(2, '0');
    seconds.value = Math.floor((diff % 60000) / 1000).toString().padStart(2, '0');
  }, 1000);
};

// 停止倒计时
const stopTimer = () => {
  if (timer.value) {
    clearInterval(timer.value);
    timer.value = null;
  }
};

// 分页处理
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1;
  loadProducts();
  updateNextSession();
};

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
  loadProducts();
  updateNextSession();
};

// 购买秒杀商品
const purchaseSeckill = (product) => {
  console.log('购买秒杀商品:', product);
};

const router = useRouter();
const handleProductClick = (product) => {
  router.push({ path: `/user/mall/seckProduct/info/${product.seckillId}` });
};

// 生命周期钩子
onMounted(() => {
  updateSessionTimes();
  loadProducts();
  updateNextSession();
  nextTimer.value = setInterval(updateNextSession, 1000);
});

onUnmounted(() => {
  stopTimer();
  if (nextTimer.value) {
    clearInterval(nextTimer.value);
    nextTimer.value = null;
  }
});

// 监听场次变化
watch([seckillSessions, activeSession], () => {
  loadProducts();
  updateNextSession();
}, { deep: true });
</script>

<style scoped>
/* 全局倒计时横幅 */
.global-countdown {
  background: #fff3d8;
  padding: 15px 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  font-size: 1.1em;
  color: #ff6600;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.banner-title {
  font-weight: bold;
}

.timer-number {
  font-size: 1.5em;
  font-weight: bold;
  color: #ff3300;
  padding: 0 5px;
}

.session-info {
  color: #666;
  font-size: 0.9em;
  margin-left: 10px;
}

.session-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.session-buttons .el-button {
  min-width: 80px;
}

.session-buttons .el-button.active {
  background-color: #ff3300;
  border-color: #ff3300;
  color: white;
}

.current-tag {
  margin-left: 5px;
  background-color: #ffeb3b;
  color: #333;
  padding: 1px 3px;
  border-radius: 3px;
  font-size: 0.8em;
}

.seckill-timer {
  background: #ffebee;
  padding: 12px 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  font-size: 1.1em;
  color: #ff3300;
  display: flex;
  align-items: center;
  gap: 10px;
}

.session-title {
  font-weight: bold;
  margin-right: 10px;
}

.timer-text {
  color: #666;
}

.timer-number {
  font-size: 1.5em;
  font-weight: bold;
  color: #ff3300;
  padding: 0 5px;
}

.seckill-card {
  height: 100%;
  transition: transform 0.3s ease;
}

.seckill-card:hover {
  transform: translateY(-5px);
}

.card-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 5px;
}

.seckill-badge {
  background: #ff3300;
  color: white;
  padding: 2px 5px;
  font-size: 0.8em;
  border-radius: 3px;
}

.discount-badge {
  background: #ff9900;
  color: white;
  padding: 2px 5px;
  font-size: 0.8em;
  border-radius: 3px;
}

.product-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.product-name {
  font-size: 1em;
  font-weight: bold;
  margin: 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.seckill-price {
  font-size: 1.2em;
  font-weight: bold;
  color: #ff3300;
}

.original-price {
  font-size: 0.9em;
  color: #999;
  text-decoration: line-through;
}

.el-pagination {
  margin-top: 20px;
}
</style>