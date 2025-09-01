package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BusinessException;
import com.sky.feign.OrderFeignService;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.SeckillProductMapper;
import com.sky.rabbitmq.MallSender;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ProductService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.SeckillProductVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    OrderFeignService orderFeignService;
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private MallSender mallSender;
    @Autowired
    private DefaultRedisScript<Long> seckillScript;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 分页获取普通商品
     * @return
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());
        Page<Product> page = productMapper.pageQuery(productPageQueryDTO);
        long total = page.getTotal();
        List<Product> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 分页获取秒杀商品
     *
     * @return 包含分页信息和秒杀商品列表的 PageResult 对象
     */
    @Override
    public PageResult getSeckillProducts(SeckillPageQueryDTO seckillPageQueryDTO) {
        // 开启分页查询，设置当前页码和每页显示数量
        PageHelper.startPage(seckillPageQueryDTO.getPage(), seckillPageQueryDTO.getPageSize());
        // 调用 SeckillProductMapper 的 pageQuery 方法进行分页查询，返回一个 Page 对象
        Page<SeckillProductVO> page = seckillProductMapper.pageQuery(seckillPageQueryDTO);
        // 获取查询结果的总记录数
        long total = page.getTotal();
        // 获取当前页的秒杀商品记录列表
        List<SeckillProductVO> records = page.getResult();
        // 创建一个 PageResult 对象，包含总记录数和当前页的记录列表
        return new PageResult(total, records);
    }

    /**
     * 获取单个秒杀商品
     */
    public SeckillProductVO getSeckillProductById(Integer seckillId) {
        if (seckillId == null) {
            return null;
        }
        String seckillKey = "seckill:product:" + seckillId;

        // 1. 优先从 Redis 中获取商品信息
        Map<Object, Object> productMap = redisTemplate.opsForHash().entries(seckillKey);
        if (productMap != null && !productMap.isEmpty()) {
            // 从 Redis 中获取到数据，转换为对象
            SeckillProductVO product = convertMapToSeckillProduct(productMap);
            if (product != null) {
                return product;
            }
        }

        // 2. Redis中没有数据，查询数据库
        SeckillProductVO product = seckillProductMapper.getSeckillProductById(seckillId);

        // 3. 数据库查询到数据，回写到Redis
        if (product != null) {
            saveProductToRedis(product);
        }

        return product;
    }

    /**
     * 将商品信息保存到Redis
     */
    private void saveProductToRedis(SeckillProductVO product) {
        String seckillKey = "seckill:product:" + product.getSeckillId();
        Map<String, Object> seckillProductMap = new HashMap<>();
        seckillProductMap.put("seckillId", product.getSeckillId().toString());
        seckillProductMap.put("productId", product.getProductId().toString());
        seckillProductMap.put("productName", product.getProductName());
        seckillProductMap.put("productPrice", product.getProductPrice().toString());
        seckillProductMap.put("seckillPrice", product.getSeckillPrice().toString());
        seckillProductMap.put("seckillStartTime", product.getSeckillStartTime().toString());
        seckillProductMap.put("seckillEndTime", product.getSeckillEndTime().toString());
        seckillProductMap.put("seckillStock", product.getSeckillStock().toString());
        seckillProductMap.put("seckillLimit", product.getSeckillLimit().toString());
        seckillProductMap.put("seckillImageUrl", product.getSeckillImageUrl());

        redisTemplate.opsForHash().putAll(seckillKey, seckillProductMap);
        // 设置合理的过期时间，比如秒杀结束后10分钟
        LocalDateTime endTime = product.getSeckillEndTime();
        long expireSeconds = endTime.atZone(ZoneId.systemDefault()).toEpochSecond() -
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() + 600;
        if (expireSeconds > 0) {
            redisTemplate.expire(seckillKey, expireSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 获取单个product详情
     * @param productId
     * @return
     */
    public Product getProductById(Integer productId){
        if (productId == null) {
            return null;
        }
        return productMapper.getProductById(productId);

    }
    /**
     * 将 Map 转换为 SeckillProduct 对象
     */
    private SeckillProductVO convertMapToSeckillProduct(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        SeckillProductVO product = new SeckillProductVO();

        // 已有字段设置保持不变
        product.setSeckillId(Integer.valueOf(map.get("seckillId").toString()));
        product.setProductId(Integer.valueOf(map.get("productId").toString()));
        product.setProductName(String.valueOf(map.get("productName").toString()));
        product.setSeckillPrice(new BigDecimal(map.get("seckillPrice").toString()));
        product.setProductPrice(new BigDecimal(map.get("productPrice").toString()));
        product.setSeckillStock(Integer.valueOf(map.get("seckillStock").toString()));
        product.setSeckillLimit(Integer.valueOf(map.get("seckillLimit").toString()));
        product.setSeckillImageUrl(map.get("seckillImageUrl").toString());

        // 修复：正确设置时间字段
        String startTimeStr = (String) map.get("seckillStartTime");
        String endTimeStr = (String) map.get("seckillEndTime");

        if (startTimeStr != null) {
            product.setSeckillStartTime(LocalDateTime.parse(startTimeStr));
        }

        if (endTimeStr != null) {
            product.setSeckillEndTime(LocalDateTime.parse(endTimeStr));
        }

        return product;
    }

    /**
     * 限时秒杀
     * @param seckillCreateDTO 购买信息
     * @return
     */
    @Override
    public Result<String> purchaseProduct(SeckillCreateDTO seckillCreateDTO) {
        Integer userId = seckillCreateDTO.getUserId();
        Integer seckillId = seckillCreateDTO.getSeckillId();
        Integer quantity = seckillCreateDTO.getPurchaseQuantity();
        String orderId = UUID.randomUUID().toString() + userId;

        // ========== 打印请求参数 ==========
        System.out.println("\n===== 秒杀请求开始 ======");
        System.out.println("请求参数：");
        System.out.println("  userId: " + userId);
        System.out.println("  seckillId: " + seckillId);
        System.out.println("  quantity: " + quantity);
        System.out.println("  orderId: " + orderId);

        if (seckillId == null) {
            System.out.println("错误：秒杀ID为空");
            return Result.error("秒杀ID不能为空");
        }

        // ========== 打印当前系统时间 ==========
        LocalDateTime now = LocalDateTime.now();
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = currentTimeMillis / 1000;
        System.out.println("\n当前系统时间：");
        System.out.println("  本地时间：" + now);
        System.out.println("  毫秒级时间戳：" + currentTimeMillis);
        System.out.println("  秒级时间戳：" + currentTimeSeconds);

        // ========== 读取 Redis 中的秒杀时间 ==========
        String startTimeKey = "seckill:start_time:" + seckillId;
        String endTimeKey = "seckill:end_time:" + seckillId;
        String stockKey = "seckill:stock:" + seckillId;
        String limitKey = "seckill:limit:" + seckillId;

        String startTimeValue = redisTemplate.opsForValue().get(startTimeKey);
        String endTimeValue = redisTemplate.opsForValue().get(endTimeKey);
        String stockValue = redisTemplate.opsForValue().get(stockKey);
        String limitValue = redisTemplate.opsForValue().get(limitKey);

        System.out.println("\nRedis 数据检查：");
        System.out.println("  " + startTimeKey + ": " + startTimeValue);
        System.out.println("  " + endTimeKey + ": " + endTimeValue);
        System.out.println("  " + stockKey + ": " + stockValue);
        System.out.println("  " + limitKey + ": " + limitValue);

        // ========== 执行 Lua 脚本 ==========
        List<String> args = Arrays.asList(
                seckillId.toString(),
                userId.toString(),
                orderId,
                quantity.toString()
        );
        System.out.println("\n执行 Lua 脚本，参数：" + args);

        Long result = redisTemplate.execute(
                seckillScript,
                Collections.emptyList(),
                args.toArray(new String[0])
        );

        System.out.println("\nLua 脚本返回结果：" + result);

        // ========== 处理结果 ==========
        if (result != null && result == 0) {
            System.out.println("秒杀成功，生成订单：" + orderId);
            // 生成订单逻辑...
            Order order=Order.builder()
                    .orderId(orderId)
                    .userId(seckillCreateDTO.getUserId())
                    .seckillId(seckillCreateDTO.getSeckillId())
                    .orderQuantity(seckillCreateDTO.getPurchaseQuantity())
                    .build();
            mallSender.sendOrderMessage(order);
            return Result.success("秒杀成功");
        } else {
            String errorMsg = "未知错误";
            switch (result != null ? result.intValue() : -1) {
                case 1:
                    errorMsg = "秒杀未开始";
                    break;
                case 2:
                    errorMsg = "秒杀已结束";
                    break;
                case 3:
                    errorMsg = "库存不足";
                    break;
                case 4:
                    errorMsg = "超过限购数量";
                    break;
            }
            System.out.println("秒杀失败，原因：" + errorMsg);
            return Result.error(errorMsg);
        }
    }

    /**
     * 普通商品下单
     *
     */
    @Override
    public void purchaseNormalProduct(PurchaseDTO purchaseDTO){
        //根据商品Id查询商品信息
        Product product = productMapper.getProductById(purchaseDTO.getProductId());
        if (product == null) {
            return;
        }
        productMapper.decreaseProductStock(purchaseDTO.getProductId(),purchaseDTO.getQuantity());
        //商品下单
        String orderId = UUID.randomUUID().toString();
        Order order=Order.builder()
                .userId(purchaseDTO.getUserId())
                .orderId(orderId)
                .productId(product.getProductId())
                .orderQuantity(purchaseDTO.getQuantity())
                .build();
        mallSender.sendOrderMessage(order);

    }

    /**
     * 创建普通商品
     * @param productDTO 商品创建信息
     * @return 创建结果，包含商品ID
     */
    @Override
    public Product  createProduct(ProductCreateDTO productDTO) {

        // 构建商品对象
        Product product = Product.builder()
                .productName(productDTO.getProductName())
                .productPrice(productDTO.getProductPrice())
                .productStock(productDTO.getProductStock())
                .description(productDTO.getDescription())
                .productLimit(productDTO.getProductLimit())
                .build();

        // 插入商品到数据库
         productMapper.insert(product);
        // 返回结果
        return product;
    }


    /**
     * 发布秒杀商品
     * @param seckSetDTO 秒杀商品创建信息
     * @return 操作结果
     */
    @Override
    public Result<Integer> publishSeckillProduct(SeckSetDTO seckSetDTO) {
        // 参数校验
        if (seckSetDTO == null) {
            return Result.error("参数不能为空");
        }

        // 检查商品是否存在
        Product product = productMapper.getProductById(seckSetDTO.getProductId());
        if (product == null) {
            return Result.error("商品不存在");
        }

        // 构建秒杀商品对象
        SeckillProduct seckillProduct = SeckillProduct.builder()
                .productId(seckSetDTO.getProductId())
                .seckillPrice(seckSetDTO.getSeckillPrice())
                .seckillStartTime(seckSetDTO.getSeckillStartTime())
                .seckillEndTime(seckSetDTO.getSeckillEndTime())
                .seckillStock(seckSetDTO.getSeckillStock())
                .seckillLimit(seckSetDTO.getSeckillLimit())
                .seckillImageUrl(product.getProductImageUrl())
                .build();

        // 保存秒杀商品到数据库
        seckillProductMapper.insertSeck(seckillProduct);

        // 转换时间为秒级时间戳（UTC 时区）
        LocalDateTime startTime = seckillProduct.getSeckillStartTime();
        LocalDateTime endTime = seckillProduct.getSeckillEndTime();
        long startSeconds = startTime.atZone(ZoneId.of("UTC")).toEpochSecond();
        long endSeconds = endTime.atZone(ZoneId.of("UTC")).toEpochSecond();
        Integer seckillId = seckillProduct.getSeckillId();
        // 构建 Redis Key
        String seckillKey = "seckill:product:" + seckillId;
        String stockKey = "seckill:stock:" + seckillId;
        String startTimeKey = "seckill:start_time:" + seckillId;
        String endTimeKey = "seckill:end_time:" + seckillId;
        String limitKey = "seckill:limit:" + seckillId;



        // 构建秒杀商品信息 Map 时，将所有数值类型转换为字符串
        Map<String, Object> seckillProductMap = new HashMap<>();
        seckillProductMap.put("seckillId", seckillProduct.getSeckillId().toString());
        seckillProductMap.put("productId", seckillProduct.getProductId().toString());
        seckillProductMap.put("productName", product.getProductName().toString());
        seckillProductMap.put("productPrice", product.getProductPrice().toString());
        seckillProductMap.put("seckillPrice", seckillProduct.getSeckillPrice().toString());
        seckillProductMap.put("seckillStartTime", startTime.toString());
        seckillProductMap.put("seckillEndTime", endTime.toString());
        seckillProductMap.put("seckillStock", String.valueOf(seckillProduct.getSeckillStock())); // 使用 String.valueOf()
        seckillProductMap.put("seckillLimit", String.valueOf(seckillProduct.getSeckillLimit())); // 使用 String.valueOf()
        seckillProductMap.put("seckillImageUrl", seckillProduct.getSeckillImageUrl());
        // 存储到 Redis（Hash 类型）
        redisTemplate.opsForHash().putAll(seckillKey, seckillProductMap);

        // 存储独立的时间戳、库存、限购（确保存储为 String）
        redisTemplate.opsForValue().set(stockKey, String.valueOf(seckillProduct.getSeckillStock()));
        redisTemplate.opsForValue().set(startTimeKey, String.valueOf(startSeconds)); // 秒级时间戳
        redisTemplate.opsForValue().set(endTimeKey, String.valueOf(endSeconds));     // 秒级时间戳
        redisTemplate.opsForValue().set(limitKey, String.valueOf(seckillProduct.getSeckillLimit()));


        // 其他逻辑（列表缓存、过期时间）...（同原代码）

        return Result.success( seckillId,"秒杀商品发布成功");
    }

    /**
     * 修改秒杀库存,库存减少
     * @param seckillId
     * @param account
     */
    public void decreaseRepertory(Integer seckillId, Integer account) {
        seckillProductMapper.decreaseSeckillStock(seckillId,account);
    }

    /**
     * 修改限时商品库存退货，库存增加
     * @param seckillId
     * @param account
     */
    public void increaseRepertory(Integer seckillId, Integer account) {
        if (account > 0) {
            // 构建 Redis Key
            String seckillKey = "seckill:product:" + seckillId;
            String stockKey = "seckill:stock:" + seckillId;

            // 分布式锁（按 seckillId 锁定，避免同一秒杀商品的并发问题）
            RLock lock = redissonClient.getLock("lock:seckill_cancel:" + seckillId);

            try {
                // 尝试获取锁，超时时间5秒，自动释放时间10秒
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new RuntimeException("系统繁忙，请稍后重试");
                }
                // 1. 原子性恢复库存（独立库存键）
                redisTemplate.execute((RedisCallback<Long>) connection ->
                        connection.stringCommands().incrBy(
                                stockKey.getBytes(),
                                account.longValue()
                        )
                );
                // 2. 更新 Hash 中的库存字段（可选，保持数据冗余一致性）
                redisTemplate.opsForHash().increment(seckillKey, "seckillStock", account);
                seckillProductMapper.increaseSeckillStock(seckillId,account);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("获取分布式锁被中断"+ e);
                throw new RuntimeException("系统内部错误");
            } finally {
                // 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    // 头像尺寸限制（5MB）
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    // 允许的图片类型
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/bmp"
    };

    public String uploadPic( MultipartFile file,Integer productId) {

        // 验证文件有效性
        validateFile(file);

        // 生成文件名和路径
        String fileName = generateFileName(file);
        String filePath = "user/avatar/" + productId + "/" + fileName;

        try {
            // 构建访问URL
            String avatarUrl =aliOssUtil.upload(file.getBytes(), filePath);
            productMapper.updateProductPic(productId, avatarUrl);
            return avatarUrl;
        } catch (Exception e) {

            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }
    /**
     * 验证文件有效性
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new BusinessException("不支持的图片格式，仅支持jpg、png、gif、bmp");
        }
    }
    /**
     * 生成文件名
     */
    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";

        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return UUID.randomUUID() + suffix;
    }
}
