package org.example.selfwebblog.content.tag;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Tag;
import org.example.selfwebblog.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TagService {
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;

    public TagService(TagMapper tagMapper, PostTagMapper postTagMapper) {
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
    }

    public List<TagSummary> listSummaries() {
        return postTagMapper.listSummaries();
    }

    public List<Post> listPublishedPosts(String slug) {
        if (slug == null || slug.isBlank()) throw new IllegalArgumentException("标签不存在");
        return postTagMapper.listPublishedPosts(slug.trim());
    }

    @Transactional
    public Tag create(String rawName) {
        return findOrCreate(normalizeName(rawName));
    }

    @Transactional
    public void replacePostTags(Long postId, String rawTags) {
        if (postId == null) return;
        postTagMapper.deleteRelationsForPost(postId);
        for (String name : parseNames(rawTags)) {
            Tag tag = findOrCreate(name);
            postTagMapper.insertRelation(postId, tag.getId());
        }
    }

    @Transactional
    public Tag rename(Long id, String rawName) {
        String name = normalizeName(rawName);
        Tag tag = requireTag(id);
        Tag conflict = postTagMapper.findByName(name);
        if (conflict != null && !conflict.getId().equals(id)) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        tag.setName(name);
        tag.setSlug(uniqueSlug(name, id));
        tagMapper.updateById(tag);
        return tag;
    }

    @Transactional
    public void merge(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
            throw new IllegalArgumentException("请选择两个不同标签");
        }
        requireTag(sourceId);
        requireTag(targetId);
        postTagMapper.mergeRelations(sourceId, targetId);
        postTagMapper.deleteRelationsForTag(sourceId);
        tagMapper.deleteById(sourceId);
    }

    @Transactional
    public void deleteUnused(Long id) {
        requireTag(id);
        if (postTagMapper.usageCount(id) > 0) throw new IllegalArgumentException("标签仍被文章使用");
        tagMapper.deleteById(id);
    }

    public int relationCountForPost(Long postId) {
        return postTagMapper.relationCountForPost(postId);
    }

    public static List<String> parseNames(String rawTags) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (rawTags == null) return List.of();
        for (String raw : rawTags.split("[,，\\n]+")) {
            if (raw == null || raw.isBlank()) continue;
            String name = normalizeName(raw);
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        return new ArrayList<>(unique.values());
    }

    public static String normalizeName(String value) {
        String normalized = value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        normalized = normalized.replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > 30) {
            throw new IllegalArgumentException("标签名称需要 1 到 30 个字符");
        }
        return normalized;
    }

    public static String slugFor(String name) {
        String normalized = Normalizer.normalize(normalizeName(name), Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (!normalized.isBlank()) return normalized;
        return "tag-" + digest(name).substring(0, 10);
    }

    private Tag findOrCreate(String name) {
        Tag existing = postTagMapper.findByName(name);
        if (existing != null) return existing;
        String slug = uniqueSlug(name, null);
        postTagMapper.insertTag(name, slug, LocalDateTime.now());
        return postTagMapper.findByName(name);
    }

    private String uniqueSlug(String name, Long currentId) {
        String base = slugFor(name);
        String slug = base;
        int suffix = 2;
        while (true) {
            Tag conflict = postTagMapper.findBySlug(slug);
            if (conflict == null || conflict.getId().equals(currentId)) return slug;
            slug = base + "-" + suffix++;
        }
    }

    private Tag requireTag(Long id) {
        Tag tag = id == null ? null : tagMapper.selectById(id);
        if (tag == null) throw new IllegalArgumentException("标签不存在");
        return tag;
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            for (byte item : bytes) output.append(String.format("%02x", item));
            return output.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成标签标识", exception);
        }
    }
}
