package com.aqinyo.controller.admin;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.result.Result;
import com.aqinyo.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/*   使用到"上传文件"到OSS的: 通用接口(要上传到OSS都可复用) --> 用于处理前端的传来的上传文件请求  (下面有详细流程解析)   */

@RestController
@RequestMapping("admin/common")
@Slf4j
@Api(tags = "通用相关接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;  // 依赖注入 工具类对象

    @PostMapping("/upload")
    @ApiOperation("文件上传")   // API描述
    public Result<String> upload(MultipartFile file){   // MultipartFile用于接收"前端发来"要传上去的文件, (形参名 = 接口文档的参数名)
        log.info("文件上传: {}", file);
        try {
            /*   123点 均为了 "防止重名" 的操作   */
            // 1.获取原始文件全名
            String originalFilename = file.getOriginalFilename();
            // 2.截取原始文件的后缀名     (如: .png/.jpg等等)
            String newName = originalFilename.substring(originalFilename.lastIndexOf(".")); // 从后缀名.这里开始截取
            // 3.构造新的文件名称        (采用 UUID随机命名 + 拼接原本原始文件的后缀名 (防止重名) )
            String name = UUID.randomUUID() + newName;

            /*   4.上传OSS并获OSS返回的URL(然后才赋值给uploadPath)  和  5.返回前端URL并落库   */
            // 4.获取OSS返回的文件请求路径
            String uploadPath = aliOssUtil.upload(file.getBytes(), name);/* 调用aliOssUtil工具类的upload方法(带着配置的AccessKey信息,把图片上传给OSS)并获得OSS返回的URL */
            // 5.然后后端拿到了第4点OSS返回给我们的公共访问URL(下载路径) --> 返回给前端,然后就根据这个请求路径URL能请求到图片了 --> 与此同时后端也把这个URL存入数据库的图片字段里
            return Result.success(uploadPath);  /* 上述路线精简化: 前端 → 后端 → 阿里云 OSS → 后端 → 前端 → 最终存入数据库 */

        } catch (IOException e) {
            log.error("文件上传失败: {}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
