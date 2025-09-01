package com.sky.service.impl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.*;
import com.sky.entity.Order;
import com.sky.entity.Product;
import com.sky.entity.SeckillProduct;
import com.sky.entity.UserSeckillRecord;
import com.sky.feign.ProductFeignService;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserSeckillRecordMapper;
import com.sky.rabbitmq.orderSender;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.RabbitMQConfig;
import com.sky.vo.OrderVO;
import com.sky.vo.ProductVO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Autowired
    private orderSender orderSenderMsg;


    @RabbitListener(queues = RabbitMQConfig.CREATE_ORDER_QUEUE)
    public void createOrder(Order order) {
        // 前置参数校验
        if (order == null) {
            log.error("订单数据为空，无法创建订单");
            return;
        }

        Integer seckillId = order.getSeckillId();
        Integer productId = order.getProductId();

        // 参数合法性校验
        if (seckillId == null && productId == null) {
            log.error("订单缺少商品ID和秒杀ID，无法创建订单");
            return;
        }

        try {
            if (seckillId == null) {
                // 处理普通订单
                handleNormalOrder(order, productId);
            } else {
                // 处理秒杀订单
                handleSeckillOrder(order, seckillId);
            }
        } catch (Exception e) {
            log.error("创建订单异常", e);
        }
    }

    /**
     * 处理普通订单
     */
    private void handleNormalOrder(Order order, Integer productId) {
        Result<Product> result = productFeignService.getProduct(productId);
        handleFeignResult(result, productId, order, false);
    }

    /**
     * 处理秒杀订单
     */
    private void handleSeckillOrder(Order order, Integer seckillId) {
        Result<SeckillProduct> result = productFeignService.getSeckillProducts(seckillId);
        handleFeignResult(result, seckillId, order, true);
    }

    /**
     * 统一处理Feign调用结果
     */
    private <T> void handleFeignResult(Result<T> result, Integer id, Order order, boolean isSeckill) {
        if (result == null || result.getCode() != 1) {
            log.error("Feign调用失败，ID: {}, 返回码：{}，错误信息：{}",
                    id,
                    result != null ? result.getCode() : "null",
                    result != null ? result.getMsg() : "无");
            return;
        }

        T data = result.getData();
        if (data == null) {
            log.error("{}商品不存在，ID: {}", isSeckill ? "秒杀" : "普通", id);
            return;
        }

        // 提取公共订单创建逻辑
        createOrderCore(order, isSeckill ? (SeckillProduct) data : (Product) data);

        // 秒杀订单特有逻辑
        if (isSeckill) {
            SeckillProduct seckillProduct = (SeckillProduct)(Object)data;
            Integer seckillId = seckillProduct.getSeckillId();
            System.out.println("seckillId是否存在"+seckillId);
            Integer account = order.getOrderQuantity();
            productFeignService.decreaseRepor(seckillId, account);

            // 生成秒杀记录
            String recordId = UUID.randomUUID().toString() + seckillProduct.getSeckillId();
            SeckillCreateDTO seckillCreateDTO = new SeckillCreateDTO();
            seckillCreateDTO.setSeckillId(seckillProduct.getSeckillId());
            seckillCreateDTO.setPurchaseQuantity(account);
            seckillCreateDTO.setRecordId(recordId);
            userSeckillRecordMapper.insertRecord(seckillCreateDTO);

            log.info("秒杀订单处理完成，秒杀ID: {}", seckillProduct.getSeckillId());
        }
    }

    /**
     * 核心订单创建逻辑
     */
    private void createOrderCore(Order order, Object productObj) {
        Integer account = order.getOrderQuantity();
        BigDecimal total;

        if (productObj instanceof SeckillProduct) {
            SeckillProduct seckillProduct = (SeckillProduct) productObj;
            total = seckillProduct.getSeckillPrice().multiply(new BigDecimal(account));
            order.setProductId(seckillProduct.getProductId());
        } else {
            Product product = (Product) productObj;
            total = product.getProductPrice().multiply(new BigDecimal(account));
            order.setProductId(product.getProductId());
        }

        // 统一设置订单公共属性
        order.setOrderPrice(total);
        order.setCreateTime(LocalDateTime.now());
        order.setOrderStatus("未支付");

        log.info("创建订单: {}", order);
        orderMapper.insertOrder(order);
        log.info("订单插入完成，订单ID: {}", order.getOrderId());
        MessageDTO messageDTO = MessageDTO.builder()
                .receiver(order.getUserId())
                .type(3)
                .sourceModule("/order")
                .content("您有新的订单需要处理")
                .sourceId(1)
                .build();
        orderSenderMsg.sendOrderMessage(messageDTO);
    }

    /**
     * 更新订单状态
     * @param orderStatusDTO
     */
    public void updateOrderStatus(OrderStatusDTO orderStatusDTO){
        String orderStatus = orderStatusDTO.getOrderStatus();
        if(orderStatus == "已取消"){
            OrderVO order = orderMapper.getOrderById(orderStatusDTO.getOrderId());
            //退库存
            //判断字段是否为空
            productFeignService.ReturnRepor(order.getSeckillId(),order.getOrderQuantity());
        }
        orderMapper.updateStatus(orderStatusDTO);
    }

    // 定时任务：每 1 分钟检查一次订单状态，10 分钟内未支付的订单设置为已取消
    @Scheduled(cron = "0 0/1 * * * *")
    public void checkOrderStatus() {
        // 计算 10 分钟前的 LocalDateTime
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        orderMapper.findAll().forEach(order -> {
            LocalDateTime createTime = order.getCreateTime();
            if (createTime.isBefore(tenMinutesAgo)) {
                //增加库存
                productFeignService.ReturnRepor(order.getSeckillId(),order.getOrderQuantity());
                OrderStatusDTO orderStatusDTO = OrderStatusDTO.builder()
                        .orderStatus("已取消")
                        .orderId(order.getOrderId())
                        .build();
                orderMapper.updateStatus(orderStatusDTO);
            }
        });
    }

    /**
     * 订单分页
     * @param orderPageDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrderPageDTO orderPageDTO){
        PageHelper.startPage(orderPageDTO.getPage(),orderPageDTO.getPageSize());
        Page<OrderVO> page = orderMapper.pageQuery(orderPageDTO);
        long total = page.getTotal();
        java.util.List<OrderVO> orders = page.getResult();
        return new PageResult(total,orders);
    }

    /**
     * 秒杀记录详情
     * @return
     */
    public UserSeckillRecord searchOrder(Integer userId,Integer seckillId){
        return userSeckillRecordMapper.getRecordByUserIdAndSeckillId(userId,seckillId);

    }

    /**
     * 创建秒杀记录
     * @param seckillCreateDTO
     */
    public void createSeckill(SeckillCreateDTO seckillCreateDTO){
        String RecordId = UUID.randomUUID().toString()+seckillCreateDTO.getSeckillId();
        seckillCreateDTO.setRecordId(RecordId);
        userSeckillRecordMapper.insertRecord(seckillCreateDTO);
    }

    /**
     * 查看订单详情
     * @param orderId
     * @return
     */
    public OrderVO findOrder(String orderId){
        return orderMapper.getOrderById(orderId);
    }
}

