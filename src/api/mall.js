import instance from '@/utils/request.js';

// 获取全部商品列表
export const getAllProductsService = (productPageQueryDTO) => {
  return instance.post("/mall/product/list", productPageQueryDTO);
};

// 获取秒杀商品列表
export const getSeckillProductsService = (pageQueryDTO) => {
  return instance.post("/mall/seckill/list", pageQueryDTO);
};

// 获取单个秒杀商品信息
export const getSeckillProductInfoService = (seckillId) => {
  return instance.get(`/mall/seckill/info/${seckillId}`); 
};

// 获取单个普通商品信息
export const getProductInfoService = (productId) => {
  return instance.get(`/mall/product/info/${productId}`);
};

// 购买秒杀商品
export const purchaseSeckillProductService = (seckillCreateDTO) => {
  return instance.post("/mall/purchase/seckill", seckillCreateDTO);
};

// 购买普通商品
export const purchaseNormalProductService = (purchaseDTO) => {
  return instance.post("/mall/purchase/normal", purchaseDTO);
};





// 创建普通商品
export const createProductService = (productCreateDTO) => {
  return instance.post("/mall/product/create", productCreateDTO);
};

// 图片上传
export const uploadAvatarService = (file, productId) => {
  const formData = new FormData();
  formData.append('file', file);
  return instance.post(`/mall/avatar/${productId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};
// 设置限时秒杀
export const setSeckillProductService = (seckSetDTO) => {
  return instance.post("/mall/product/set", seckSetDTO);
};  