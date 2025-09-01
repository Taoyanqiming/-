package com.sky.controller;

import com.sky.dto.*;
import com.sky.entity.Product;
import com.sky.entity.SeckillProduct;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ProductService;
import com.sky.vo.SeckillProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品相关操作的控制器
 */
@RestController
@RequestMapping("/mall")
public class MallController {

    @Autowired
    private ProductService productService;

    /**
     * 展示全部商品
     * @return 全部商品列表
     */
    @PostMapping("/product/list")
    public Result<PageResult> getAllProducts(@RequestBody ProductPageQueryDTO productPageQueryDTO) {
        PageResult products = productService.pageQuery(productPageQueryDTO);
        return Result.success(products);
    }

    /**
     * 展示秒杀商品
     * @return 秒杀商品列表
     */
    @PostMapping("/seckill/list")
    public Result<PageResult> getSeckillProducts(@RequestBody SeckillPageQueryDTO PageQueryDTO) {
        PageResult seckillProducts = productService.getSeckillProducts(PageQueryDTO);
        return Result.success(seckillProducts);
    }

    /**
     * 获取单个秒杀商品全部信息
     */
    @GetMapping("/seckill/info/{seckillId}")
    public Result<SeckillProductVO> getSeckillProducts(@PathVariable Integer seckillId) {
        SeckillProductVO seckillProduct = productService.getSeckillProductById(seckillId);
        return Result.success(seckillProduct);
    }

    /**
     * 获取单个普通商品全部信息
     */
    @GetMapping("/product/info/{productId}")
    public Result<Product> getProduct(@PathVariable Integer productId) {
        Product product = productService.getProductById(productId);
        return Result.success(product);
    }

    /**
     * 扣减秒杀库存
     */
    @PutMapping("/decrease/product")
    public Result decreaseRepor(@RequestParam Integer seckillId,@RequestParam Integer account) {
       productService.decreaseRepertory(seckillId,account);
        return Result.success();
    }

    /**
     * 修改限时秒杀商品库存，用于退货
     */
    @PutMapping("/return/product")
    public Result ReturnRepor(@RequestParam Integer seckillId,@RequestParam Integer account) {
        productService.increaseRepertory(seckillId,account);
        return Result.success();
    }

    /**
     * 购买秒杀商品
     * @param seckillCreateDTO 包含用户ID、秒杀商品ID（可选）、购买数量
     * @return 操作结果
     */
    @PostMapping("/purchase/seckill")
    public Result<String> purchaseProduct(@RequestBody SeckillCreateDTO seckillCreateDTO) {
        return productService.purchaseProduct(seckillCreateDTO); // 直接返回 Service 的 Result 对象
    }
    /**
     * 购买普通商品
     * @param purchaseDTO 包含用户ID、秒杀商品ID（可选）、购买数量
     * @return 操作结果
     */
    @PostMapping("/purchase/normal")
    public Result<String> purchaseSecProduct(@RequestBody PurchaseDTO purchaseDTO) {
        productService.purchaseNormalProduct(purchaseDTO);
        return Result.success();
    }

    /**
     * 创建普通商品
     * @param productCreateDTO 包含用户ID、秒杀商品ID（可选）、购买数量
     * @return 操作结果
     */
    @PostMapping("/product/create")
    public Result<Product> purchaseProduct(@RequestBody ProductCreateDTO productCreateDTO) {
        Product product=productService.createProduct(productCreateDTO);
        return Result.success(product);
    }
    /**
     * 设置限时秒杀
     */
    @PostMapping("/product/set")
    public Result<Integer> seckProduct(@RequestBody SeckSetDTO seckSetDTO) {
        return  productService.publishSeckillProduct(seckSetDTO);
    }
    /**
     * 用户头像上传
     */
    @PostMapping("/avatar/{productId}")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, @PathVariable Integer productId) {

        String url= productService.uploadPic(file,productId);
        return Result.success(url);
    }



}