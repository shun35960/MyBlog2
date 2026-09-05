package com.example.MyBlog.Controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.MyBlog.Config.SecurityConfig;
import com.example.MyBlog.Service.ImageService;

@WebMvcTest(ImageController.class)
@Import({SecurityConfig.class})
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ImageService imageService;

    // --- uploadImage ---

    @Test
    void uploadImage_成功_201とfileIdが返される() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes());
        when(imageService.storeImage(any(), eq("testuser"))).thenReturn("fileId123");

        // Act & Assert
        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .with(user("testuser").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value("fileId123"))
                .andExpect(jsonPath("$.url").value("/api/images/fileId123"))
                .andExpect(jsonPath("$.message").value("uploadSuccess"));

        verify(imageService).storeImage(any(), eq("testuser"));
    }

    @Test
    void uploadImage_バリデーションエラー_400とエラーメッセージが返される() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "data".getBytes());
        when(imageService.storeImage(any(), any())).thenThrow(new IllegalArgumentException("Invalid file type"));

        // Act & Assert
        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .with(user("testuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid file type"));
    }

    @Test
    void uploadImage_サービス例外_500とuploadFailedが返される() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes());
        when(imageService.storeImage(any(), any())).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .with(user("testuser")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("uploadFailed"));
    }

    // --- getImage ---

    @Test
    void getImage_成功_200と画像データが返される() throws Exception {
        // Arrange
        GridFsResource resource = mock(GridFsResource.class);
        when(resource.getContentType()).thenReturn(MediaType.IMAGE_JPEG_VALUE);
        when(resource.contentLength()).thenReturn(100L);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("image data".getBytes()));
        when(imageService.getImage("fileId123")).thenReturn(resource);

        // Act & Assert
        mockMvc.perform(get("/api/images/fileId123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));

        verify(imageService).getImage("fileId123");
    }

    @Test
    void getImage_contentTypeがnull_octetStreamで返される() throws Exception {
        // Arrange
        GridFsResource resource = mock(GridFsResource.class);
        when(resource.getContentType()).thenReturn(null);
        when(resource.contentLength()).thenReturn(50L);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(imageService.getImage("fileId123")).thenReturn(resource);

        // Act & Assert
        mockMvc.perform(get("/api/images/fileId123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    @Test
    void getImage_存在しないfileId_404が返される() throws Exception {
        // Arrange
        when(imageService.getImage("notExist")).thenThrow(new IllegalArgumentException("not found"));

        // Act & Assert
        mockMvc.perform(get("/api/images/notExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getImage_IOエラー_500が返される() throws Exception {
        // Arrange
        GridFsResource resource = mock(GridFsResource.class);
        when(resource.getContentType()).thenReturn(MediaType.IMAGE_JPEG_VALUE);
        when(resource.contentLength()).thenReturn(100L);
        when(resource.getInputStream()).thenThrow(new IOException("read error"));
        when(imageService.getImage("fileId123")).thenReturn(resource);

        // Act & Assert
        mockMvc.perform(get("/api/images/fileId123"))
                .andExpect(status().isInternalServerError());
    }

    // --- deleteImage ---

    @Test
    void deleteImage_成功_200とdeleteSuccessが返される() throws Exception {
        // Arrange
        doNothing().when(imageService).deleteImage("fileId123");

        // Act & Assert
        mockMvc.perform(delete("/api/images/fileId123")
                        .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleteSuccess"))
                .andExpect(jsonPath("$.fileId").value("fileId123"));

        verify(imageService).deleteImage("fileId123");
    }

    @Test
    void deleteImage_存在しないfileId_404が返される() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("not found")).when(imageService).deleteImage("notExist");

        // Act & Assert
        mockMvc.perform(delete("/api/images/notExist")
                        .with(user("testuser")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteImage_サービス例外_500とdeleteFailedが返される() throws Exception {
        // Arrange
        doThrow(new RuntimeException("DB error")).when(imageService).deleteImage("fileId123");

        // Act & Assert
        mockMvc.perform(delete("/api/images/fileId123")
                        .with(user("testuser")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("deleteFailed"));
    }
}
