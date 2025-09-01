package com.sky.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户信息自动注入切面
 * 通过反射将请求头中的 userId 注入到方法参数中的 DTO 对象
 */
@Aspect
@Component
public class UserInfoAspect {

    // 定义需要注入 userId 的目标包（根据实际项目调整）
    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            "com.sky.dto"
    ));

    // 匹配 GET 和 PUT 请求的 Controller 方法
    @Pointcut("execution(* com.sky.controllers..*(..)) && (@annotation(org.springframework.web.bind.annotation.GetMapping) || @annotation(org.springframework.web.bind.annotation.PutMapping))")
    public void controllerMethod() {} // 统一命名为controllerMethod

    @Before("controllerMethod()") // 应用于所有匹配的方法
    public void autoInjectUserId(JoinPoint joinPoint) {
        // 获取请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        // 从请求头获取 userId（Gateway 已注入）
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null) {
            throw new RuntimeException("用户未登录或缺少用户ID");
        }
        Integer userId = Integer.parseInt(userIdStr); // 使用parseInt更明确

        // 遍历方法参数，注入到符合条件的DTO
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null && isTargetPackageClass(arg.getClass())) {
                injectUserId(arg, userId);
            }
        }
    }

    /**
     * 检查类是否属于目标包
     */
    private boolean isTargetPackageClass(Class<?> clazz) {
        String packageName = clazz.getPackage().getName();
        return TARGET_PACKAGES.stream().anyMatch(packageName::startsWith);
    }

    /**
     * 通过反射注入 userId 到对象
     */
    private void injectUserId(Object target, Integer userId) {
        try {
            // 优先查找 setUserId 方法
            Field userIdField = findField(target.getClass(), "userId");
            if (userIdField != null) {
                userIdField.setAccessible(true);
                userIdField.set(target, userId);
                return;
            }

            // 未找到 userId 字段，尝试查找其他名称（如：creatorId、authorId）
            Field[] fields = target.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (isUserIdField(field)) {
                    field.setAccessible(true);
                    field.set(target, userId);
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("注入用户ID失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找指定名称的字段
     */
    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * 判断字段是否为用户ID字段（根据字段类型和名称推断）
     */
    private boolean isUserIdField(Field field) {
        // 检查字段类型是否为 Integer 或 int
        if (!field.getType().equals(Integer.class) && !field.getType().equals(int.class)) {
            return false;
        }

        // 检查字段名称是否包含 "userId" 或类似名称
        String fieldName = field.getName().toLowerCase();
        return fieldName.contains("userid");

    }
}