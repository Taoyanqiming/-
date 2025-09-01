package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.dto.UserDTO;
import com.sky.dto.UserDetailsDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserPageQueryDTO;
import com.sky.entity.Details;
import com.sky.entity.User;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BusinessException;
import com.sky.exception.PasswordErrorException;
import com.sky.exception.UserPageQueryFailedException;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.UserVO;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private UserMapper userMapper;
    /**
     * 用户登录
     */
    public User login(UserLoginDTO userLoginDTO) {
        String phoneNumber = userLoginDTO.getPhoneNumber();
        String password = userLoginDTO.getPassword();
        //根据用户手机号查询数据
        User user = userMapper.getByPhone(phoneNumber);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //密码对比
//        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

       return user;
     }
    /**
     * 新增用户，注册
     * @param userDTO
     * @return
     */
    @Override
    public void save(UserDTO userDTO){
        User user = new User();
        BeanUtils.copyProperties(userDTO,user);
        user.setPassword(PasswordConstant.DEFAULT_PASSWORD);
        userMapper.insert(user);
    }

    /**
     * 用户分页展示
     * @param userPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(UserPageQueryDTO userPageQueryDTO) {
    try{
        PageHelper.startPage(userPageQueryDTO.getPage(),userPageQueryDTO.getPageSize());
        Page<User> page = userMapper.pageQuery(userPageQueryDTO);
        long total = page.getTotal();
        List<User> records = page.getResult();
        return new PageResult(total,records);
    } catch (Exception e) {
        // 数据库查询异常
        log.error("用户分页查询失败", e);
        throw new UserPageQueryFailedException("用户分页查询失败，请稍后重试");
    }
    }

    /**
     * 根据phoneNumber查询用户信息
     * @param phoneNumber
     * @return
     */
    @Override
    public User getByNumber(String phoneNumber) {
        User user = userMapper.getByPhone(phoneNumber);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    /**
     * 修改用户信息
     * @param userDTO
     * @return
     */
    @Override
    public void update(UserDTO userDTO) {

        User user = new User();
        BeanUtils.copyProperties(userDTO,user);
        userMapper.update(user);

    }
    @Override
    public void updateDetail(UserDetailsDTO userDetailsDTO){

        Details details = new Details();
        BeanUtils.copyProperties(userDetailsDTO,details);
        userMapper.updateDetails(details);
    }

    public UserVO select(Integer userId){
        return userMapper.select(userId);
    }

    // 头像尺寸限制（5MB）
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    // 允许的图片类型
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/bmp"
    };

    public String uploadAvatar(Integer userId, MultipartFile file) {
        // 验证用户是否存在
        UserVO user = userMapper.select(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 验证文件有效性
        validateFile(file);

        // 生成文件名和路径
        String fileName = generateFileName(file);
        String filePath = "user/avatar/" + userId + "/" + fileName;

        try {
            // 构建访问URL
            String avatarUrl =aliOssUtil.upload(file.getBytes(), filePath);
            userMapper.updateUserAvatar(userId, avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("上传头像失败", e);
            throw new BusinessException("头像上传失败，请稍后重试");
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
