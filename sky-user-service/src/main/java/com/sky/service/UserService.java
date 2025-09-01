package com.sky.service;

import com.sky.dto.UserDTO;
import com.sky.dto.UserDetailsDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserPageQueryDTO;
import com.sky.entity.User;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    User login(UserLoginDTO userLoginDTO);
    /**
     * 新增用户，注册
     * @param userDTO
     * @return
     */
    void save(UserDTO userDTO);
    /**
     * 用户分页展示
     * @param userPageQueryDTO
     * @return
     */
    PageResult pageQuery(UserPageQueryDTO userPageQueryDTO);
    /**
     * 根据手机号查询用户信息
     * @param phoneNumber
     * @return
     */
    User getByNumber(String phoneNumber);
    /**
     * 修改用户信息
     * @param userDTO
     * @return
     */
    void update(UserDTO userDTO);

    UserVO select(Integer userId);

    void updateDetail(UserDetailsDTO userDetailsDTO);

    /**
     * 用户头像上传
     * @param userId
     * @param file
     * @return
     */
    String uploadAvatar(Integer userId, MultipartFile file);
}
