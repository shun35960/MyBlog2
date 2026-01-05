package com.example.MyBlog.Service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * ImageServiceImplは、GridFSを使用した画像管理サービスの実装クラスです。
 * 画像のアップロード、取得、削除機能を提供します。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final GridFSBucket gridFSBucket;
    private final GridFsTemplate gridFsTemplate;

    // 許可される画像形式
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // 最大ファイルサイズ (100MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    /**
     * 画像をGridFSに保存します。
     *
     * @param file       アップロードされたファイル
     * @param uploadedBy アップロードしたユーザー名
     * @return GridFSに保存されたファイルのID (ObjectIdの文字列表現)
     * @throws IllegalArgumentException ファイルが無効な場合
     * @throws RuntimeException         GridFSへの保存に失敗した場合
     */
    @Override
    public String storeImage(MultipartFile file, String uploadedBy) {
        // ファイルのバリデーション
        validateFile(file);

        try {
            // ファイル名のサニタイゼーション
            String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());

            // メタデータの作成
            Document metadata = new Document();
            metadata.put("uploadedBy", uploadedBy);
            metadata.put("originalFilename", sanitizedFilename);
            metadata.put("contentType", file.getContentType());
            metadata.put("fileSize", file.getSize());
            metadata.put("uploadDate", new java.util.Date());

            // GridFsTemplateを使用してファイルを保存
            InputStream inputStream = file.getInputStream();
            ObjectId fileId = gridFsTemplate.store(
                    inputStream,
                    sanitizedFilename,
                    file.getContentType(),
                    metadata
            );

            log.info("Image stored successfully: fileId={}, filename={}, uploadedBy={}",
                    fileId.toHexString(), sanitizedFilename, uploadedBy);

            return fileId.toHexString();

        } catch (IOException e) {
            log.error("Failed to store image: filename={}, error={}",
                    file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to store image", e);
        }
    }

    /**
     * GridFSから画像を取得します。
     *
     * @param fileId GridFSファイルID (ObjectIdの文字列表現)
     * @return GridFsResource 画像リソース
     * @throws IllegalArgumentException ファイルが見つからない場合
     */
    @Override
    public GridFsResource getImage(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            Query query = new Query(Criteria.where("_id").is(objectId));

            // ✅ ステップ1: まずGridFSFileを取得
            GridFSFile gridFSFile = gridFsTemplate.findOne(query);

            // ✅ ステップ2: null チェック
            if (gridFSFile == null) {
                log.warn("Image not found: fileId={}", fileId);
                throw new IllegalArgumentException("Image not found with id: " + fileId);
            }

            // ✅ ステップ3: GridFSFileからGridFsResourceを取得
            GridFsResource resource = gridFsTemplate.getResource(gridFSFile);

            log.debug("Image retrieved successfully: fileId={}", fileId);
            return resource;

        } catch (IllegalArgumentException e) {
            log.error("Invalid fileId format: {}", fileId);
            throw new IllegalArgumentException("Invalid file ID format", e);
        }
    }

    /**
     * GridFSから画像を削除します。
     *
     * @param fileId GridFSファイルID (ObjectIdの文字列表現)
     * @throws IllegalArgumentException ファイルIDが無効な場合
     */
    @Override
    public void deleteImage(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            gridFsTemplate.delete(query);
            log.info("Image deleted successfully: fileId={}", fileId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid fileId format for deletion: {}", fileId);
            throw new IllegalArgumentException("Invalid file ID format", e);
        }
    }

    /**
     * アップロードされたファイルのバリデーションを行います。
     *
     * @param file バリデーション対象のファイル
     * @throws IllegalArgumentException バリデーションエラー
     */
    private void validateFile(MultipartFile file) {
        // ファイルが空でないことを確認
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ファイルが選択されていません");
        }

        // ファイルサイズの確認
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("ファイルサイズが大きすぎます (最大: %dMB)", MAX_FILE_SIZE / 1024 / 1024)
            );
        }

        // Content-Typeの確認
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "サポートされていないファイル形式です (対応形式: JPEG, PNG, GIF, WebP)"
            );
        }

        // ファイル名の確認
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("ファイル名が無効です");
        }
    }

    /**
     * ファイル名をサニタイズします（セキュリティ対策）。
     * パストラバーサル攻撃を防ぐため、危険な文字を除去します。
     *
     * @param filename 元のファイル名
     * @return サニタイズされたファイル名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }

        // パス区切り文字を除去
        String sanitized = filename.replaceAll("[/\\\\]", "");

        // 制御文字を除去
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");

        // 先頭と末尾の空白を除去
        sanitized = sanitized.trim();

        // 空文字列の場合はデフォルト名を返す
        if (sanitized.isEmpty()) {
            return "unnamed";
        }

        return sanitized;
    }
}
