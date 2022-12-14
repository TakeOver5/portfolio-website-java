package com.tapcus.portfoliowebsitejava.service.impl;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import com.tapcus.portfoliowebsitejava.dao.ArticleDao;
import com.tapcus.portfoliowebsitejava.dao.MemberDao;
import com.tapcus.portfoliowebsitejava.model.*;
import com.tapcus.portfoliowebsitejava.service.ArticleService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static com.google.cloud.storage.BlobInfo.newBuilder;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    ArticleDao articleDao;

    @Override
    public Integer uploadArticle(Integer memberId, String title, String introduction, String content, MultipartFile cover, String git_file_path) throws IOException {

        // 上傳圖片到 firebase，返回 url
        String coverUrl = upload(cover);
        return articleDao.createArticle(memberId, title, introduction, content, coverUrl, git_file_path);

    }

    @Override
    public Integer uploadMessage(Integer articleId, Integer memberId, String content) {
        return articleDao.createMessage(articleId, memberId, content);
    }

    @Override
    public List<Article> getArticles(Integer limit, Integer offset) {
        return articleDao.getArticles(limit, offset);
    }

    @Override
    public Integer countArticle() {
        return articleDao.countArticle();
    }

    @Override
    public Integer countArticleAll() {
        return articleDao.countArticleAll();
    }

    @Override
    public ArticleDetail getArticle(Integer articleId) {

        ArticleDetail articleDetail = articleDao.getArticle(articleId);
        List<MessageDetail> messageDetailList = articleDao.getMessage(articleId);
        articleDetail.setMessageDetail(messageDetailList);
        return articleDetail;
    }

    @Override
    public List<ArticleSimple> getArticlesSimple(Integer limit, Integer offset) {
        return articleDao.getArticlesSimple(limit, offset);
    }

    @Override
    public void setViewable(Integer articleId, Integer view) {
        articleDao.setViewable(articleId, view);
    }

    @Override
    public void deleteArticle(Integer articleId) throws IOException {
        Object object = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = (Member) object;

        String coverPath = articleDao.getArticleCoverUrlByArticleIdMemberId(member.getMemberId(), articleId);

        coverPath = coverPath.substring(75, 115);

        if(member != null) articleDao.deleteArticle(member.getMemberId(), articleId);
        else return;

        if(coverPath != null) {
            deleteFile(coverPath);
        } else return;
    }

    @Override
    public void uploadEditArticle(Integer articleId, String title, String introduction, String content, String oldCover, MultipartFile newCover, String git_file_path) throws IOException {
        // 如果有新圖片要上傳到 firebase，返回 url，並刪除原有的 firebase url
        String coverUrl;
        if(newCover != null) {
            coverUrl = upload(newCover);
            oldCover = oldCover.substring(75, 115);
            deleteFile(oldCover);
        } else {
            coverUrl = null;
        }
        articleDao.uploadEditArticle(articleId, title, introduction, content, coverUrl, git_file_path);
    }

    // 上傳整合
    public String upload(MultipartFile multipartFile) throws IOException {
        String fileName = null;
        File file = null;

        try {
            fileName = multipartFile.getOriginalFilename();                        // to get original file name
            fileName = UUID.randomUUID().toString().concat(this.getExtension(fileName));  // to generated random string values for file name.

            file = this.convertToFile(multipartFile, fileName);                      // to convert multipartFile to File
            String TEMP_URL = this.uploadFile(file, fileName);                                   // to get uploaded file link
            file.delete();                                                                // to delete the copy of uploaded file stored in the project folder
            return TEMP_URL;                     // Your customized response
        } catch (Exception e) {
            e.printStackTrace();
            this.deleteFile(fileName);
            file.delete();
            throw new IOException("檔案上傳發生錯誤");
        }
    }

    // 上傳主體，最後返回下載路徑
    private String uploadFile(File file, String fileName) throws IOException {
        // 你的 bucket name
        BlobId blobId = BlobId.of("fileupload-33ca0.appspot.com", fileName);
        BlobInfo blobInfo = newBuilder(blobId).setContentType("media").build();

        File temp = new File("src\\main\\resources\\firebase.json");
        Credentials credentials = GoogleCredentials.fromStream(new FileInputStream(temp));
        System.out.println(credentials);
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));

        return String.format("https://firebasestorage.googleapis.com/v0/b/fileupload-33ca0.appspot.com/o/%s?alt=media", URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    // MultipartFile 轉 File
    private File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File tempFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
            fos.close();
        }
        return tempFile;
    }

    // 獲取副檔名
    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    // 刪除檔案
    private void deleteFile(String fileName) throws IOException {

        try {

            ClassPathResource serviceAccount = new ClassPathResource("firebase.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                    .setStorageBucket("fileupload-33ca0.appspot.com")
                    .build();

            FirebaseApp.initializeApp(options);

        } catch (Exception ex) {

            ex.printStackTrace();

        }

        Bucket bucket = StorageClient.getInstance().bucket();

        if (StringUtils.isEmpty(fileName)) {
            throw new IOException("invalid file name");
        }

        Blob blob = bucket.get(fileName);

        if (blob == null) {
            throw new IOException("file not found");
        }

        blob.delete();
    }
}
