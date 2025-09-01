package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     */
    public String upload(byte[] bytes, String objectName) {
        return upload(new ByteArrayInputStream(bytes), objectName);
    }

    /**
     * 上传输入流
     */
    public String upload(InputStream inputStream, String objectName) {
        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 上传文件
            ossClient.putObject(bucketName, objectName, inputStream);
            log.info("文件上传成功: {}", objectName);
        } catch (OSSException | ClientException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 构建访问URL
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }

    /**
     * 删除文件
     */
    public boolean delete(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
