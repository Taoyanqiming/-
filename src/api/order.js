import instance from '@/utils/request.js'; // 确保导入正确的实例

// 1. 分页查询订单
export const getAllOrdersService = (orderPageDTO) => {
  return instance.post("/order/list", orderPageDTO);
};

// 2. 根据Id查询秒杀记录，用于限购
export const searchSeckillRecordService = (seckillId, userId) => {
  return instance.get("/order/search", {
    params: {
      seckillId,
      userId
    }
  });
};

// 3. 查看订单详情
export const searchOrderInfoService = (orderId) => {
  return instance.get("/order/order/info", {
    params: {
      orderId
    }
  });
};


// 5. 更新订单状态，付款or取消
export const updateOrderStatusService = (orderStatusDTO) => {
  return instance.put("/order/order/ok", orderStatusDTO);
};