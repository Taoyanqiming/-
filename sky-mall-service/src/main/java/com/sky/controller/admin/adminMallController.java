package com.sky.controller.admin;
import com.sky.dto.ProductCreateDTO;
import com.sky.dto.SeckSetDTO;
import com.sky.entity.Product;
import com.sky.result.Result;
import com.sky.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品相关操作的控制器
 */
@RestController
@RequestMapping("/admin/mall")
public class adminMallController {

    @Autowired
    private ProductService productService;


}