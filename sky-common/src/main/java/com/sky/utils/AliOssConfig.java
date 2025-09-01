package com.sky.utils;

import com.sky.properties.AliOssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliOssConfig {

    @Autowired
    private AliOssProperties aliOssProperties;

    @Bean
    public AliOssUtil aliOssUtil() {
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName()
        );
    }
}
