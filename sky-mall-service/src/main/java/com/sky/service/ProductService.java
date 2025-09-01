package com.sky.service;


import com.sky.dto.*;
import com.sky.entity.Product;
import com.sky.entity.SeckillProduct;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.SeckillProductVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {
    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO);

    /**
     * 获取秒杀商品列表
     * @return 秒杀商品列表
     */
    PageResult getSeckillProducts(SeckillPageQueryDTO seckillPageQueryDTO);

    /**
     * 购买商品
     * @param seckillCreateDTO 购买信息
     * @return 购买是否成功
     */
    Result<String> purchaseProduct(SeckillCreateDTO seckillCreateDTO);

    void purchaseNormalProduct(PurchaseDTO purchaseDTO);

    /**
     * 创建普通产品
     * @param productDTO
     * @return
     */
    Product  createProduct(ProductCreateDTO productDTO);

    /**
     * 创建秒杀商品
     * @param seckSetDTO
     * @return
     */
    Result<Integer> publishSeckillProduct(SeckSetDTO seckSetDTO);

    /**
     * 获取单个秒杀商品详情
     * @param seckillId
     * @return
     */
    SeckillProductVO getSeckillProductById(Integer seckillId);

    /**
     * 修改秒杀库存
     * @param seckillId
     * @param account
     */
    void decreaseRepertory(Integer seckillId, Integer account);
    void increaseRepertory(Integer seckillId, Integer account);

    /**
     * 获取单个product详情
     * @param productId
     * @return
     */
    Product getProductById(Integer productId);

    String uploadPic(MultipartFile file,Integer productId);
}