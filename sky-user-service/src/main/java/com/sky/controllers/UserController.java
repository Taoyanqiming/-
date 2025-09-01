package com.sky.controllers;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserDTO;
import com.sky.dto.UserDetailsDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.*;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import com.sky.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录： userLoginDTO:{}", userLoginDTO);
        User user = userService.login(userLoginDTO);

        // 生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getUserId());
        claims.put(JwtClaimsConstant.USER_ROLE, user.getRole());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .userId(user.getUserId())
                .userName(user.getUsername())
                .role(user.getRole())
                .icon(user.getIcon())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .token(token)
                .build();

        return Result.success(userLoginVO, "登录成功");
    }
    /**
     * 用户头像上传
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,@RequestHeader("X-User-Id") Integer userId) {

        log.info("用户{}上传头像", userId);

        String url= userService.uploadAvatar(userId, file);
        return Result.success(url);
    }
    /**
     * 修改用户信息
     * @param userDTO
     * @return
     */
    @PutMapping("/update")
    public Result update(@RequestBody UserDTO userDTO) {
        log.info("编辑用户信息：{}", userDTO);
        try {
            userService.update(userDTO);
            return Result.successMsg("修改成功");
        } catch (UserUpdateFailedException e) {
            return Result.error(e.getMessage());
        }
    }
    /**
     * 修改用户信息
     * @param userDetailsDTO
     * @return
     */
    @PutMapping("/update/detail")
    public Result updateDatail(@RequestBody UserDetailsDTO userDetailsDTO) {
        log.info("编辑用户信息：{}", userDetailsDTO);
        try {
            userService.updateDetail(userDetailsDTO);
            return Result.successMsg("修改成功");
        } catch (UserUpdateFailedException e) {
            return Result.error(e.getMessage());
        }
    }
    /**
     * 退出
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.successMsg("退出成功");
    }

    /**
     * 注册
     * @param userDTO
     * @return
     */
    @PostMapping("/register")
    public Result save(@RequestBody UserDTO userDTO) {
        User user = userService.getByNumber(userDTO.getPhoneNumber());
        if (user != null) {
            return Result.error("用户已存在");
        }
        log.info("新增用户：{}", userDTO);
        try {
            userService.save(userDTO);
            return Result.successMsg("创建成功");
        } catch (UserSaveFailedException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<UserVO> select(@RequestHeader("X-User-Id") Integer userId) {

       return Result.success(userService.select(userId));

    }


}
