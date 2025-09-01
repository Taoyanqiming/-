import instance from '@/utils/request.js'; // 确保导入正确的实例

// 1. 用户登录
export const userLoginService = (userLoginDTO) => {
  return instance.post("/user/login", userLoginDTO);
};

// 2. 用户登出
export const logoutService = () => {
  return instance.post("/user/logout");
};

// 3. 新增用户（注册）
export const userRegisterService = (userDTO) => {
  return instance.post("/user/add", userDTO);
};
// 4. 修改用户信息
export const updateUserService = (userDTO) => {
  return instance.put("/user/update", userDTO);
};
export const updateDetailService = (UserDetailsDTO) => {
  return instance.put("/user/update/detail", UserDetailsDTO);
};

// 1. 获取用户信息
export const getUserInfo = () => {
  return instance.get("/user/info");
};

//管理员接口：
// 1. 分页展示用户
export const getUsersService = (userPageQueryDTO) => {
  return instance.get("/admin/user/page", { params: userPageQueryDTO }

  );
};

// 1. 获取我发布的帖子
export const getMyPosts = (userPostDTO) => {
  return instance.post("/forum/info/post", userPostDTO);
};

// 2. 获取我收藏的内容
export const getMyFavorites = (userFavorDTO) => {
  return instance.post("/forum/info/favor",  userFavorDTO );
};

/**
 * 用户头像上传
 * @param {File} file - 上传的头像文件
 * @returns {Promise}
 */
export const uploadAvatarService = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return instance.post("/user/avatar", formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};