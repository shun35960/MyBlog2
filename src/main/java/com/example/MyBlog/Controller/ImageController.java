package com.example.MyBlog.Controller;

import com.example.MyBlog.Service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("ape/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage
            (@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            String fileId = imageService.storeImage(file, username);

            Map<String, String> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("url", "/api/images/" + fileId);
            response.put("message", "画像のアップロード成功");

            log.info("Image uploaded successfully: fileId={}, user={}", fileId, username);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Image upload validation error: {}", e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Image upload failed", e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "画像のアップロードに失敗しました");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String fileId) {
        try {
            GridFsResource resource = imageService.getImage(fileId);

            String contentType = resource.getContentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(resource.contentLength());
            headers.setCacheControl("public, max-age=2592000");

            log.debug("Service image: fileId={}, contentType={}", fileId, contentType);
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (IllegalArgumentException e) {
            log.warn("Image not found: fileId={}", fileId);
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            log.error("Failed to read image: fileId={}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 指定されたIDの画像を削除します。
     * 認証済みユーザーのみアクセス可能です。
     *
     * @param fileId GridFSファイルID
     * @return 削除結果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable String fileId) {
        try {
            imageService.deleteImage(fileId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "画像を削除しました");
            response.put("fileId", fileId);

            log.info("Image deleted successfully: fileId={}", fileId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete image: fileId={}, error={}", fileId, e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "画像が見つかりません");

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Failed to delete image: fileId={}", fileId, e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "画像の削除に失敗しました");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
