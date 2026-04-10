package com.example.MyBlog.Service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private GridFSBucket gridFSBucket;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private ImageServiceImpl imageService;

    // ---- storeImage ----

    @Test
    void storeImage_正常系_ファイルIDが返される() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        ObjectId objectId = new ObjectId();
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString(), any()))
                .thenReturn(objectId);

        // Act
        String result = imageService.storeImage(file, "user1");

        // Assert
        assertEquals(objectId.toHexString(), result);
        verify(gridFsTemplate).store(any(InputStream.class), anyString(), anyString(), any());
    }

    @Test
    void storeImage_nullファイル_IllegalArgumentExceptionがスローされる() {
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(null, "user1"));
        assertEquals("ファイルが選択されていません", ex.getMessage());
    }

    @Test
    void storeImage_空ファイル_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
        assertEquals("ファイルが選択されていません", ex.getMessage());
    }

    @Test
    void storeImage_ファイルサイズ超過_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(200L * 1024 * 1024); // 200MB (上限100MB超え)

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
        assertTrue(ex.getMessage().contains("ファイルサイズが大きすぎます"));
    }

    @Test
    void storeImage_非対応ContentType_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
        assertTrue(ex.getMessage().contains("サポートされていないファイル形式です"));
    }

    @Test
    void storeImage_ContentTypeがnull_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
    }

    @Test
    void storeImage_ファイル名がnull_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
        assertEquals("ファイル名が無効です", ex.getMessage());
    }

    @Test
    void storeImage_ファイル名が空白のみ_IllegalArgumentExceptionがスローされる() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("   ");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.storeImage(file, "user1"));
        assertEquals("ファイル名が無効です", ex.getMessage());
    }

    @Test
    void storeImage_IOExceptionが発生_RuntimeExceptionがスローされる() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> imageService.storeImage(file, "user1"));
        assertEquals("Failed to store image", ex.getMessage());
    }

    // ---- getImage ----

    @Test
    void getImage_正常系_GridFsResourceが返される() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        GridFSFile gridFSFile = mock(GridFSFile.class);
        GridFsResource resource = mock(GridFsResource.class);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFSFile);
        when(gridFsTemplate.getResource(gridFSFile)).thenReturn(resource);

        // Act
        GridFsResource result = imageService.getImage(fileId);

        // Assert
        assertEquals(resource, result);
        verify(gridFsTemplate).findOne(any(Query.class));
        verify(gridFsTemplate).getResource(gridFSFile);
    }

    @Test
    void getImage_ファイルが存在しない_IllegalArgumentExceptionがスローされる() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> imageService.getImage(fileId));
    }

    @Test
    void getImage_不正なfileId_IllegalArgumentExceptionがスローされる() {
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.getImage("invalid-id-format"));
        assertEquals("Invalid file ID format", ex.getMessage());
    }

    // ---- deleteImage ----

    @Test
    void deleteImage_正常系_例外なしで完了する() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        doNothing().when(gridFsTemplate).delete(any(Query.class));

        // Act & Assert
        assertDoesNotThrow(() -> imageService.deleteImage(fileId));
        verify(gridFsTemplate).delete(any(Query.class));
    }

    @Test
    void deleteImage_不正なfileId_IllegalArgumentExceptionがスローされる() {
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageService.deleteImage("invalid-id-format"));
        assertEquals("Invalid file ID format", ex.getMessage());
    }
}
