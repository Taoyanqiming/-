package com.sky.feign;

import com.sky.entity.Product;
import com.sky.entity.SeckillProduct;
import com.sky.entity.UserSeckillRecord;
import com.sky.result.Result;
import com.sky.vo.ProductVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name="mall-service")
public interface ProductFeignService { // 注意：这里应该是interface而不是class
    /**
     * 查询秒杀商品
     */
    @GetMapping("/mall/seckill/info/{seckillId}")
    Result<SeckillProduct> getSeckillProducts(@PathVariable("seckillId") Integer seckillId);

    @GetMapping("/mall/product/info/{productId}")
    Result<Product> getProduct(@PathVariable("productId") Integer productId);

    //秒杀商品支付扣减库存
    @PutMapping("/mall/decrease/product")
    Result decreaseRepor(@RequestParam Integer seckillId,@RequestParam Integer account);
    //秒杀商品取消订单增加库存
    @PutMapping("/mall/return/product")
    Result ReturnRepor(@RequestParam Integer seckillId,@RequestParam Integer account);


}