package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("admin/product/")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    //http://api.gmall.com/admin/product/fileUpload
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws IOException, MyException {

        String configFile  = this.getClass().getResource("/tracker.conf").getFile();
        String path = null;

        if (configFile != null) {
            //初始化
            ClientGlobal.init(configFile);
            //创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServce
            TrackerServer trackerServce = trackerClient.getConnection();
            //获取文件的后缀名
            String extName = FilenameUtils.getExtension(file.getOriginalFilename());
            //创建storageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServce, null);
            path = storageClient1.upload_appender_file1(file.getBytes(), extName, null);

            System.out.println(fileUrl + path);
        }

        return Result.ok(fileUrl + path);
    }
}
