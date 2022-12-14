package com.tapcus.portfoliowebsitejava.dao.impl;

import com.tapcus.portfoliowebsitejava.dao.ArticleDao;
import com.tapcus.portfoliowebsitejava.model.*;
import com.tapcus.portfoliowebsitejava.rowmapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class ArticleDaoImpl implements ArticleDao {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Integer createArticle(Integer memberId, String title, String introduction, String content, String coverUrl, String git_file_path) {
        String sql = "INSERT INTO article(title, introduction, content, cover_path, git_file_path, member_id) " +
                "VALUES (:title, :introduction, :content, :cover_path, :git_file_path, :member_id)";

        Map<String, Object> map = new HashMap<>();

        map.put("title", title);
        map.put("introduction", introduction);
        map.put("content", content);
        map.put("cover_path", coverUrl);
        map.put("git_file_path", git_file_path);
        map.put("member_id", memberId);

        // 取得自增鍵
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(map), keyHolder);

        int articleId = keyHolder.getKey().intValue();

        return articleId;
    }

    @Override
    public Integer createMessage(Integer articleId, Integer memberId, String content) {
        String sql = "INSERT INTO message(article_id, member_id, content) " +
                "VALUES (:article_id, :member_id, :content)";

        Map<String, Object> map = new HashMap<>();

        map.put("article_id", articleId);
        map.put("member_id", memberId);
        map.put("content", content);

        // 取得自增鍵
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(map), keyHolder);

        int messageId = keyHolder.getKey().intValue();

        return messageId;
    }

    @Override
    public List<Article> getArticles(Integer limit, Integer offset) {
        String sql = "SELECT * " +
                "FROM article WHERE 1=1 AND viewable=true " +
                "ORDER BY last_modified_date DESC " +
                "LIMIT :limit OFFSET :offset";
        Map<String, Object> map = new HashMap<>();
        map.put("limit", limit);
        map.put("offset", offset);
        List<Article> articleList = namedParameterJdbcTemplate.query(sql, map, new ArticleRowMapper());
        return articleList;
    }

    @Override
    public Integer countArticle() {
        String sql = "SELECT COUNT(article_id) FROM article WHERE 1=1 AND viewable=true ";
        Map<String, Object> map = new HashMap<>();
        Integer count = namedParameterJdbcTemplate.queryForObject(sql, map, Integer.class);
        return count;
    }

    public Integer countArticleAll() {
        String sql = "SELECT COUNT(article_id) FROM article WHERE 1=1 ";
        Map<String, Object> map = new HashMap<>();
        Integer count = namedParameterJdbcTemplate.queryForObject(sql, map, Integer.class);
        return count;
    }

    @Override
    public ArticleDetail getArticle(Integer articleId) {

        String sql = "SELECT a.article_id, a.title, a.introduction, a.content, a.cover_path, a.created_date , a.last_modified_date, a.git_file_path, m.member_id ,m.avatar, m.name " +
                "FROM article as a " +
                "LEFT JOIN member as m ON a.member_id = m.member_id " +
                "WHERE a.article_id = :articleId";

        Map<String, Object> map = new HashMap<>();
        map.put("articleId", articleId);

        List<ArticleDetail> articleDetailsList = namedParameterJdbcTemplate.query(sql, map, new ArticleDetailRowMapper());


        if(articleDetailsList.size() > 0)
            return articleDetailsList.get(0);
        else
            return null;
    }

    @Override
    public List<Article> getArticleByMemberId(Integer memberId) {
        String sql = "SELECT * " +
                "FROM article " +
                "WHERE member_id = :memberId";

        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);

        List<Article> articleList = namedParameterJdbcTemplate.query(sql, map, new ArticleRowMapper());

        return articleList;
    }

    @Override
    public List<MessageDetail> getMessage(Integer articleId) {

        String sql = "SELECT mg.message_id, mg.content, mg.created_date, m.avatar, m.name " +
                "FROM message as mg " +
                "LEFT JOIN member as m ON mg.member_id = m.member_id " +
                "WHERE mg.article_id = :articleId";

        Map<String, Object> map = new HashMap<>();
        map.put("articleId", articleId);

        List<MessageDetail> messageDetailList = namedParameterJdbcTemplate.query(sql, map, new MessageDetailRowMapper());

        return messageDetailList;
    }

    @Override
    public List<ArticleSimple> getArticlesSimple(Integer limit, Integer offset) {
        String sql = "SELECT a.article_id, a.title, a.last_modified_date, a.viewable, m.member_id ,m.name, m.avatar, m.email " +
                "FROM article as a " +
                "LEFT JOIN member as m ON a.member_id = m.member_id " +
                "ORDER BY last_modified_date DESC " +
                "LIMIT :limit OFFSET :offset";
        Map<String, Object> map = new HashMap<>();
        map.put("limit", limit);
        map.put("offset", offset);
        List<ArticleSimple> articleSimples = namedParameterJdbcTemplate.query(sql, map, new ArticleSimpleRowMapper());
        return articleSimples;
    }

    @Override
    public void setViewable(Integer articleId, Integer view) {
        String sql = "UPDATE article SET " +
                "viewable = :view " +
                "where article_id = :articleId";
        Map<String, Object> map = new HashMap<>();
        map.put("view", view);
        map.put("articleId", articleId);
        namedParameterJdbcTemplate.update(sql, map);
    }

    @Override
    public void deleteArticle(Integer memberId, Integer articleId) {
        String sql = "DELETE FROM article WHERE member_id = :memberId and article_id = :articleId";
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("articleId", articleId);
        namedParameterJdbcTemplate.update(sql, map);
    }

    @Override
    public String getArticleCoverUrlByArticleIdMemberId(Integer memberId, Integer articleId) {
        String sql = "SELECT cover_path " +
                "FROM article " +
                "where member_id = :memberId and article_id = :articleId ";
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("articleId", articleId);

        try {
            String coverPath = namedParameterJdbcTemplate.queryForObject(sql, map, String.class);
            return coverPath;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void uploadEditArticle(Integer articleId, String title, String introduction, String content, String coverUrl, String git_file_path) {

        String sql = "UPDATE article SET " +
                "title = :title, " +
                "introduction = :introduction, " +
                "content = :content," +
                "git_file_path = :git_file_path ";
        Map<String, Object> map = new HashMap<>();
        if(coverUrl != null) {
            sql += "cover_path = :cover_path, ";
            map.put("cover_path", coverUrl);
        }
        sql += "where article_id = :articleId ";
        map.put("title", title);
        map.put("introduction", introduction);
        map.put("content", content);
        map.put("articleId", articleId);
        map.put("git_file_path", git_file_path);
        namedParameterJdbcTemplate.update(sql, map);
    }
}
