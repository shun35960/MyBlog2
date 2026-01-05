package com.example.MyBlog.Service;

import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    String storeImage(MultipartFile file, String uploadedBy);

    GridFsResource getImage(String fileId);

    void deleteImage(String fileId);
}
