<template>
  <div class="order-list-container">
    <div class="page-header">
      <h1>我的订单</h1>
      <el-input v-model="searchKeyword" placeholder="搜索订单号或商品名称" class="search-input" @keyup.enter="handleSearch">
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table v-loading="isLoading" :data="displayOrders" stripe style="width: 100%" class="order-table">
      <el-table-column prop="orderId" label="订单号" width="200" />
      <el-table-column label="商品信息" width="300">
        <template #default="{ row }">
          <div class="product-info">
            <img :src="row.productImageUrl || defaultImage" alt="商品图片" class="product-image" />
            <div class="product-details">
              <div class="product-name">{{ row.productName || '未知商品' }}</div>
              <div class="product-meta">
                <span>数量: {{ row.orderQuantity || 0 }}</span>
                <span>单价: ¥{{ row.orderPrice || 0 }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="180" />
      <el-table-column prop="orderStatus" label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.orderStatus || '未知状态')">{{ row.orderStatus || '未知状态' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <div v-if="row.orderStatus === '已取消'">
            <el-button type="text" @click="handleViewOrder(row)">查看</el-button>
          </div>
          <div v-if="row.orderStatus === '未支付'">
            <el-button type="primary" @click="handlePayOrder(row)">付款</el-button>
            <el-button type="danger" @click="handleCancelOrder(row)">取消</el-button>
          </div>
          <div v-if="row.orderStatus === '已支付'">
            <el-button type="text" @click="handleViewOrder(row)">查看详情</el-button>
            <el-button type="warning" @click="handleRefundOrder(row)">退款</el-button>
          </div>
          <div v-if="row.orderStatus === '已发货' || row.orderStatus === '已完成'">
            <el-button type="text" @click="handleViewOrder(row)">查看详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
      @current-change="handleCurrentChange" />

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="订单详情" width="500px">
      <div v-if="currentOrder">
        <div class="detail-header">
          <h3>订单号: {{ currentOrder.orderId || '未知订单号' }}</h3>
          <el-tag :type="getStatusType(currentOrder.orderStatus || '未知状态')">{{ currentOrder.orderStatus || '未知状态'
          }}</el-tag>
        </div>
        <div class="detail-product">
          <img :src="currentOrder.productImageUrl || defaultImage" alt="商品图片" class="detail-image" />
          <div class="detail-product-info">
            <div class="detail-product-name">{{ currentOrder.productName || '未知商品' }}</div>
            <div class="detail-product-meta">
              <span>数量: {{ currentOrder.orderQuantity || 0 }}</span>
              <span>单价: ¥{{ currentOrder.orderPrice || 0 }}</span>
              <span>总价: ¥{{ (currentOrder.orderQuantity || 0) * (currentOrder.orderPrice || 0) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-info">
          <div class="info-item">
            <span class="info-label">下单时间:</span>
            <span>{{ formatDate(currentOrder.createTime) }}</span>
          </div>
          <div class="info-item" v-if="currentOrder.orderStatus === '已支付' || currentOrder.orderStatus === '已完成'">
            <span class="info-label">支付时间:</span>
            <span>{{ formatDate(currentOrder.paymentTime) }}</span>
          </div>
          <div class="info-item" v-if="currentOrder.refundTime">
            <span class="info-label">退款时间:</span>
            <span>{{ formatDate(currentOrder.refundTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">订单状态:</span>
            <span>{{ currentOrder.orderStatus || '未知状态' }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" v-if="currentOrder?.orderStatus === '待支付'" @click="handlePayOrder(currentOrder)">
          去付款
        </el-button>
        <el-button type="danger" v-if="currentOrder?.orderStatus === '待支付'" @click="handleCancelOrder(currentOrder)">
          取消订单
        </el-button>
        <el-button type="warning" v-if="currentOrder?.orderStatus === '已支付'" @click="handleRefundOrder(currentOrder)">
          申请退款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAllOrdersService, updateOrderStatusService } from '@/api/order.js';

// 初始化数据
const ordersData = ref([]); // 存储接口返回的订单列表
const total = ref(0); // 总记录数
const currentPage = ref(1);
const pageSize = ref(10);
const searchKeyword = ref('');
const isLoading = ref(false);
const dialogVisible = ref(false);
const currentOrder = ref(null);
const defaultImage = 'https://picsum.photos/200/200?random=0'; // 默认商品图片

// 计算当前显示的订单列表（带搜索过滤）
const displayOrders = computed(() => {
  if (!ordersData.value.length) return [];

  const keyword = searchKeyword.value.toLowerCase();
  return ordersData.value
    .filter(order =>
      order.orderId.toLowerCase().includes(keyword) ||
      (order.productName || '').toLowerCase().includes(keyword)
    )
    .slice(
      (currentPage.value - 1) * pageSize.value,
      currentPage.value * pageSize.value
    );
});

// 页面加载时获取订单数据
onMounted(() => {
  loadOrders();
});

// 监听搜索关键词变化，重新加载数据
watch(searchKeyword, () => {
  if (searchKeyword.value) {
    currentPage.value = 1;
    loadOrders();
  }
});

// 加载订单数据（调用接口）
const loadOrders = async () => {
  isLoading.value = true;
  try {
    // 构造分页参数
    const orderPageDTO = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value // 搜索关键词
    };

    const response = await getAllOrdersService(orderPageDTO);
    console.log('获取订单列表:', response);
    if (response.data.code === 1) {
      ordersData.value = response.data.data.records || [];
      total.value = response.data.data.total || 0;
    } else {
      ElMessage.error(response.data.msg || '获取订单列表失败');
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试');
    console.error('订单列表接口调用失败', error);
  } finally {
    isLoading.value = false;
  }
};

// 搜索订单
const handleSearch = () => {
  currentPage.value = 1;
  loadOrders();
};

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val;
  loadOrders();
};

// 当前页变更
const handleCurrentChange = (val) => {
  currentPage.value = val;
  loadOrders();
};

// 查看订单详情
const handleViewOrder = (order) => {
  currentOrder.value = { ...order }; // 浅拷贝防止响应式问题
  dialogVisible.value = true;
};

// 支付订单
const handlePayOrder = async (order) => {
  if (order.orderStatus !== '未支付') {
    ElMessage.info('该订单当前状态无法支付');
    return;
  }

  await ElMessageBox.confirm('确定要支付该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => {
    // 取消支付
  });

  try {
    // 调用更新订单状态接口
    await updateOrderStatusService({
      orderId: order.orderId,
      orderStatus: '已支付'
    });

    // 刷新订单状态
    order.orderStatus = '已支付';
    order.paymentTime = new Date().toISOString();
    ElMessage.success('支付成功');
    dialogVisible.value = false;
    loadOrders(); // 重新加载订单列表
  } catch (error) {
    ElMessage.error('支付失败，请稍后重试');
    console.error('支付接口调用失败', error);
  }
};

// 取消订单
const handleCancelOrder = async (order) => {
  if (order.orderStatus !== '未支付') {
    ElMessage.info('该订单当前状态无法取消');
    return;
  }

  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => {
    // 取消操作
  });

  try {
    // 调用更新订单状态接口
    await updateOrderStatusService({
      orderId: order.orderId,
      orderStatus: '已取消'
    });

    // 刷新订单状态
    order.orderStatus = '已取消';
    ElMessage.success('订单取消成功');
    dialogVisible.value = false;
    loadOrders(); // 重新加载订单列表
  } catch (error) {
    ElMessage.error('取消订单失败，请稍后重试');
    console.error('取消订单接口调用失败', error);
  }
};

// 申请退款
const handleRefundOrder = async (order) => {
  if (order.orderStatus !== '已支付') {
    ElMessage.info('该订单当前状态无法申请退款');
    return;
  }

  await ElMessageBox.confirm('确定要申请退款吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => {
    // 取消退款申请
  });

  try {
    // 调用更新订单状态接口
    await updateOrderStatusService({
      orderId: order.orderId,
      orderStatus: '已取消'
    });

    // 刷新订单状态
    order.orderStatus = '已取消';
    order.refundTime = new Date().toISOString();
    ElMessage.success('退款申请已提交');
    dialogVisible.value = false;
    loadOrders(); // 重新加载订单列表
  } catch (error) {
    ElMessage.error('退款申请失败，请稍后重试');
    console.error('退款接口调用失败', error);
  }
};

// 获取状态对应的标签类型
const getStatusType = (status) => {
  const typeMap = {
    '已取消': 'danger',
    '未支付': 'warning',
    '已支付': 'success',

  };
  return typeMap[status] || '';
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    return date.toLocaleString();
  } catch (error) {
    console.error('日期格式化失败', error);
    return dateString;
  }
};
</script>

<style scoped>
/* 样式保持不变，仅新增按钮样式间距 */
.order-list-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 操作按钮间距 */
.el-table .el-button+.el-button {
  margin-left: 5px;
}
</style>