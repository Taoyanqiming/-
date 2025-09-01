import instance from '@/utils/request.js'; // 确保导入正确的实例

// 1. 新增帖子
export const addPostService = (postDTO) => {
  return instance.post("/forum/add", postDTO);
};
// 2. 上传图片
// 原有的图片上传服务函数
export const uploadImagesService = (postId, images) => {
  const formData = new FormData();
  
  // 将每个图片添加到FormData
  images.forEach((file, index) => {
    formData.append(`images`, file);
  });
  
  // 构建正确的URL，只包含postId
  const url = `/forum/avatar/${postId}`;
  
  console.log("【API调用】上传图片URL:", url);
  console.log("【API调用】FormData中图片数量:", formData.getAll('images').length);
  
  return instance.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};


// 2. 新增评论
export const addCommentService = (commentDTO) => {
  return instance.post("/forum/comment/add", commentDTO);
};

// 3. 新增点赞
export const addLikeService = (likeDTO) => {
  return instance.post("/forum/like/add", likeDTO);
};

// 4. 新增评论点赞
export const addCommentLikeService = (likeCommentDTO) => {
  return instance.post("/forum/comment/like/add", likeCommentDTO);
};

// 5. 新增收藏
export const addFavoriteService = (favoriteDTO) => {
  return instance.post("/forum/favorite/add", favoriteDTO);
};

// 6. 删除帖子
export const deletePostService = (postId) => {
  return instance.delete(`/forum/delete/${postId}`);
};

// 7. 删除评论
export const deleteCommentService = (commentId) => {
  return instance.delete(`/forum/comment/delete/${commentId}`);
};

// 8. 根据帖子ID获取帖子信息
export const getPostByIdService = (postId) => {
  return instance.get(`/forum/${postId}`);
};

// 9. 根据帖子ID分页获取评论列表
export const getCommentsByPostIdService = (commentPageQueryDTO) => {
  return instance.post("/forum/comments", commentPageQueryDTO );
};

// 10. 首页分页展示所有帖子
export const getPostsByPageService = (postPageQueryDTO) => {
  return instance.post("/forum/list", postPageQueryDTO); // 直接传递参数对象作为请求体
};
// 11. 返回最热
export const getTopPostsByViewToday = () => {
  return instance.get("/forum/top/view"); // 直接传递参数对象作为请求体
};