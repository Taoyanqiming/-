package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.UserPageQueryFailedException;
import com.sky.result.Result;
import com.sky.utils.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class UserExceptionHandler extends GlobalExceptionHandler {

    // 处理用户分页查询失败异常
    @ExceptionHandler(UserPageQueryFailedException.class)
    public Result<String> handleUserPageQueryFailed(UserPageQueryFailedException ex) {
        log.error("用户分页查询失败：{}", ex.getMessage(), ex); // 记录异常详情（包括堆栈）
        return Result.error(MessageConstant.USER_PAGE_QUERY_FAILED); // 返回统一错误消息
    }
}
