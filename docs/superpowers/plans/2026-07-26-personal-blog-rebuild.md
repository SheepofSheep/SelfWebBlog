# Personal Blog Full-Stack Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the old Vue frontend with a cohesive liquid-glass personal blog, preserve every current workflow, add the approved editor/tag/interaction/theme behavior, and simplify the Spring Boot API without losing existing data.

**Architecture:** Keep the Spring Boot 3 + SQLite monolith, but move write rules into application services, expose one canonical `/api` contract, and use Flyway-only additive migrations. Rebuild the Vue 3 application from an empty `src` directory around public and admin shells, feature-owned state, shared API/UI/motion primitives, and browser-verified responsive screens.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, SQLite, Flyway, JUnit 5, Vue 3, Vue Router 4, Vite 8, CodeMirror 6, marked, DOMPurify, Lucide Vue, Node test runner, Playwright.

---

## Work breakdown and file map

This is one delivery with three independently testable tracks:

1. **Backend contract and data:** Tasks 1–5 leave the existing frontend operational while the canonical API is introduced.
2. **Frontend replacement:** Tasks 6–12 generate and approve the visual source of truth, remove the old `src`, then rebuild public, authoring, account, and admin workflows.
3. **Integration and sign-off:** Task 13 removes compatibility code, verifies migrations and all flows, and completes visual fidelity QA.

New backend units:

- `content/post/PostContentPolicy.java`: decides whether a published article contains real text or media.
- `content/post/PostUpsertRequest.java`: canonical write DTO with optional metadata and structured tags.
- `content/post/PostResponse.java`: public/admin article response that never exposes persistence details.
- `content/post/PostApplicationService.java`: transactional article save/update and tag synchronization.
- `content/post/PostQueryService.java`: published/draft/search queries and DTO mapping.
- `site/SiteHomeService.java`: explicit public-home aggregation and cache version.
- `identity/auth/*`: credential, GitHub, and session services/controllers split from the old controller.

New frontend units:

- `src/app`: bootstrap, route guards, public shell, admin shell.
- `src/shared/api`: canonical HTTP client and feature clients.
- `src/shared/ui`: accessible controls, dialogs, menus, notifications, empty/loading/error states.
- `src/shared/styles`: tokens, themes, typography, Markdown and responsive rules.
- `src/shared/motion`: appearance model, View Transition theme switch, SVG flow curves and pointer trail.
- `src/features/content`: home, search, archive, tags, calendar and article reading.
- `src/features/editor`: CodeMirror, preview, paste/drop upload tasks, draft storage, tag input and post settings.
- `src/features/interaction`: comments, guestbook, likes, replies and moderation presentation.
- `src/features/auth`: login, registration, OAuth and visitor profile.
- `src/features/admin`: overview, content, interactions, users, media, site and security.

### Task 1: Make article publication permissive but never empty

**Files:**
- Create: `SelfWebBlog/src/main/resources/db/migration/V10__post_presentation_and_query_indexes.sql`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostContentPolicy.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostUpsertRequest.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostResponse.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostResponseMapper.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostApplicationService.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/post/PostContentPolicyTest.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/post/PostApplicationServiceTest.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/entity/Post.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/controller/PostControllerTest.java`

- [ ] **Step 1: Write the failing publication-policy tests**

```java
class PostContentPolicyTest {
    @Test void acceptsTextWithoutTitleOrMetadata() {
        assertTrue(PostContentPolicy.hasPublishableContent("一段正文"));
    }

    @Test void acceptsMarkdownAndHtmlMediaWithoutText() {
        assertTrue(PostContentPolicy.hasPublishableContent("![](/uploads/a.webp)"));
        assertTrue(PostContentPolicy.hasPublishableContent("<video src=\"/uploads/a.mp4\"></video>"));
    }

    @Test void rejectsWhitespaceAndMarkdownPunctuationOnly() {
        assertFalse(PostContentPolicy.hasPublishableContent("  \n# ** ` ` ---"));
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails because the policy does not exist**

Run: `mvn -Dtest=PostContentPolicyTest test`

Expected: compilation failure naming `PostContentPolicy`.

- [ ] **Step 3: Add the additive migration and persistence fields**

```sql
ALTER TABLE post ADD COLUMN background_mode TEXT NOT NULL DEFAULT 'COVER';
ALTER TABLE post ADD COLUMN background_value TEXT DEFAULT '';
ALTER TABLE post ADD COLUMN background_focus_x INTEGER NOT NULL DEFAULT 50;
ALTER TABLE post ADD COLUMN background_focus_y INTEGER NOT NULL DEFAULT 50;

CREATE INDEX IF NOT EXISTS idx_post_status_created
    ON post(status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_post_status_category_created
    ON post(status, category, create_time DESC);
```

Remove `@NotBlank` from `Post.title` and `Post.content`, add the four presentation fields, and normalize null titles/content to empty strings in the application service so the existing SQLite `title NOT NULL` constraint remains safe.

- [ ] **Step 4: Implement the content policy**

```java
public final class PostContentPolicy {
    private static final Pattern MARKDOWN_MEDIA = Pattern.compile(
            "!\\[[^]]*]\\(([^\\s)]+)(?:\\s+\\\"[^\\\"]*\\\")?\\)");
    private static final Pattern HTML_MEDIA = Pattern.compile(
            "<(?:img|video|audio)\\b[^>]*(?:src|poster)=\\\"([^\\\"]+)\\\"[^>]*>",
            Pattern.CASE_INSENSITIVE);

    private PostContentPolicy() {}

    public static boolean hasPublishableContent(String raw) {
        String content = raw == null ? "" : raw;
        if (containsPermanentMedia(MARKDOWN_MEDIA.matcher(content))
                || containsPermanentMedia(HTML_MEDIA.matcher(content))) return true;
        String visible = HTML_MEDIA.matcher(MARKDOWN_MEDIA.matcher(content).replaceAll("")).replaceAll("")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("[#>*_`~\\-\\[\\]()!\\s]", "");
        return !visible.isBlank();
    }

    private static boolean containsPermanentMedia(Matcher matcher) {
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url.startsWith("/uploads/") || url.startsWith("/api/media/")
                    || url.startsWith("https://") || url.startsWith("http://")) return true;
        }
        return false;
    }

    public static void requirePublishable(String status, String content) {
        if ("PUBLISHED".equals(status) && !hasPublishableContent(content)) {
            throw new IllegalArgumentException("文章需要包含正文或媒体");
        }
    }
}
```

- [ ] **Step 5: Define a write DTO that makes metadata optional**

```java
public record PostUpsertRequest(
        Long id,
        String title,
        String content,
        String summary,
        String coverUrl,
        String category,
        List<String> tags,
        String status,
        String backgroundMode,
        String backgroundValue,
        Integer backgroundFocusX,
        Integer backgroundFocusY) {
}
```

`PostResponse` must return `id` as a string, tags as `List<String>`, counts as non-null integers, and all optional text as empty strings rather than null.

- [ ] **Step 6: Write application-service tests for an image-only publish, a blank publish, and an empty draft**

Mock `PostService` and `TagService`. Assert that image-only content is saved as `PUBLISHED`, completely blank published content throws `IllegalArgumentException`, and a blank `DRAFT` is accepted.

- [ ] **Step 7: Move save/update/merge/tag synchronization into `PostApplicationService`**

The transactional method must have this contract:

```java
@Transactional
public PostResponse save(PostUpsertRequest request) {
    String status = normalizeStatus(request.status());
    String content = value(request.content());
    PostContentPolicy.requirePublishable(status, content);
    Post target = request.id() == null ? new Post() : requirePost(request.id());
    target.setTitle(value(request.title()));
    target.setContent(content);
    target.setSummary(value(request.summary()));
    target.setCoverUrl(value(request.coverUrl()));
    target.setCategory(value(request.category()));
    target.setStatus(status);
    applyBackground(target, request);
    persist(target);
    tagService.replacePostTags(target.getId(), request.tags());
    return responseMapper.toResponse(target, tagService.namesForPost(target.getId()));
}
```

Clamp focus values to `0..100`, allow only `COVER`, `CUSTOM_IMAGE`, `GRADIENT`, `SOLID`, `SITE_DEFAULT`, and `NONE`, and clear `backgroundValue` when the selected mode does not use it.
Treat `backgroundValue` as validated JSON with only `imageUrl`, `colorFrom`, `colorTo`, and `angle`; accept safe upload/HTTPS image URLs, hex colors, and angles `0..360`. Never persist raw CSS.

- [ ] **Step 8: Make `PostController` delegate and return the saved article**

Replace entity binding and controller-side merging with:

```java
@PostMapping
@AdminOnly(action = "POST_SAVE")
public Result<PostResponse> save(@RequestBody PostUpsertRequest request) {
    return Result.success(postApplicationService.save(request));
}
```

Keep existing get/list routes operational until Task 5 moves all reads to response DTOs.

- [ ] **Step 9: Run backend tests**

Run: `mvn -Dtest=PostContentPolicyTest,PostApplicationServiceTest,PostControllerTest test`

Expected: all focused tests pass.

- [ ] **Step 10: Commit the article contract**

```bash
git add SelfWebBlog/src/main/resources/db/migration/V10__post_presentation_and_query_indexes.sql \
  SelfWebBlog/src/main/java/org/example/selfwebblog/content/post \
  SelfWebBlog/src/main/java/org/example/selfwebblog/entity/Post.java \
  SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java \
  SelfWebBlog/src/test/java/org/example/selfwebblog/content/post \
  SelfWebBlog/src/test/java/org/example/selfwebblog/controller/PostControllerTest.java
git commit -m "feat: support flexible article publishing"
```

### Task 2: Make one reusable tag model drive writing, search, and admin

**Files:**
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostQueryService.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostQueryMapper.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/post/PostQueryServiceTest.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/tag/TagService.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/tag/PostTagMapper.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/TagController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/tag/TagServiceTest.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/tag/TagServiceSyncTest.java`

- [ ] **Step 1: Add failing tag normalization and reuse tests**

```java
@Test void listInput_isNormalizedAndDeduplicated() {
    assertEquals(List.of("Vue", "生活 随笔"),
            TagService.normalizeNames(List.of(" Vue ", "vue", "生活   随笔")));
}

@Test void arbitraryChineseTag_getsStableSlug() {
    assertTrue(TagService.slugFor("生活随笔").startsWith("tag-"));
}
```

- [ ] **Step 2: Run the focused test and verify the missing list API**

Run: `mvn -Dtest=TagServiceTest,TagServiceSyncTest test`

Expected: failure naming `normalizeNames` or the list-based `replacePostTags` overload.

- [ ] **Step 3: Implement list-based normalization and synchronization**

```java
public static List<String> normalizeNames(List<String> rawTags) {
    Map<String, String> unique = new LinkedHashMap<>();
    for (String raw : rawTags == null ? List.<String>of() : rawTags) {
        if (raw == null || raw.isBlank()) continue;
        String name = normalizeName(raw);
        unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
    }
    return List.copyOf(unique.values());
}

@Transactional
public List<Tag> replacePostTags(Long postId, List<String> rawTags) {
    postTagMapper.deleteRelationsForPost(postId);
    List<Tag> tags = normalizeNames(rawTags).stream().map(this::findOrCreate).toList();
    tags.forEach(tag -> postTagMapper.insertRelation(postId, tag.getId()));
    postTagMapper.syncPostTagText(postId);
    return tags;
}
```

Add `namesForPost(Long)` to `PostTagMapper`, ordered case-insensitively. Keep the legacy comma-string parser only for the migration runner; new API code must pass lists.

- [ ] **Step 4: Add an exact tag-aware post query**

`PostQueryMapper.searchPublished` must search title, summary, and content, join `post_tag/tag` when a tag is supplied, and select distinct posts ordered by date unless `sort=title`.

```java
List<Post> searchPublished(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("tag") String tag,
        @Param("sort") String sort,
        @Param("limit") int limit,
        @Param("offset") int offset);
```

- [ ] **Step 5: Return the same canonical tags from all endpoints**

`GET /api/tags` returns tag summaries, `POST /api/tags` creates-or-reuses a tag, article responses return structured tags, and article search accepts exact tag name or slug. Rename/merge/delete continue to operate on the same tag table.

- [ ] **Step 6: Run tag, post, calendar, and migration tests**

Run: `mvn -Dtest=TagServiceTest,TagServiceSyncTest,PostQueryServiceTest,ContentCalendarServiceTest,ContentMigrationTest test`

Expected: all tests pass and Flyway validates migrations V1–V10.

- [ ] **Step 7: Commit canonical tags**

```bash
git add SelfWebBlog/src/main/java/org/example/selfwebblog/content \
  SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java \
  SelfWebBlog/src/main/java/org/example/selfwebblog/controller/TagController.java \
  SelfWebBlog/src/test/java/org/example/selfwebblog/content
git commit -m "refactor: unify article tags and search"
```

### Task 3: Replace the overloaded profile endpoint with explicit site APIs

**Files:**
- Create: `SelfWebBlog/src/main/resources/db/migration/V11__site_appearance_preferences.sql`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/site/SiteHomeService.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/site/SiteHomeServiceTest.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/site/SiteProfileService.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/site/SiteController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/admin/AdminSiteController.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/site/SiteSummaryServiceTest.java`

- [ ] **Step 1: Write the failing home aggregation test**

Assert that `SiteHomeService.home(1, 12)` returns site profile, featured article, article records, total count, tag summaries, categories, and site summary without consulting the current visitor session.

- [ ] **Step 2: Add site-level appearance defaults**

```sql
ALTER TABLE blog_info ADD COLUMN default_post_background_mode TEXT NOT NULL DEFAULT 'COVER';
ALTER TABLE blog_info ADD COLUMN default_post_background_value TEXT DEFAULT '';
ALTER TABLE blog_info ADD COLUMN default_motion_mode TEXT NOT NULL DEFAULT 'FULL';
ALTER TABLE blog_info ADD COLUMN pointer_trail_enabled INTEGER NOT NULL DEFAULT 1;
```

Extend `SiteProfile` and `SiteSettingsRequest` with those four values. Validate background modes against the article mode set, motion against `FULL`/`REDUCED`, and convert the SQLite integer to boolean.

- [ ] **Step 3: Implement the explicit home response**

```java
public record SiteHome(
        SiteProfileService.SiteProfile site,
        PostResponse featured,
        List<PostResponse> posts,
        long total,
        List<TagSummary> tags,
        List<String> categories,
        SiteSummaryService.SiteSummary summary,
        String version) {}
```

The version is a stable hash of the latest public post update, site status update, and requested page. It becomes the response ETag.

- [ ] **Step 4: Expose `GET /api/site/home` with conditional caching**

```java
@GetMapping("/home")
public ResponseEntity<Result<SiteHome>> home(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestHeader(value = "If-None-Match", required = false) String etag) {
    SiteHome value = homeService.home(page, size);
    String quoted = "\"" + value.version() + "\"";
    if (quoted.equals(etag)) return ResponseEntity.status(304).eTag(quoted).build();
    return ResponseEntity.ok().eTag(quoted)
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
            .body(Result.success(value));
}
```

- [ ] **Step 5: Run site and migration tests**

Run: `mvn -Dtest=SiteHomeServiceTest,SiteSummaryServiceTest,ContentMigrationTest,SelfWebBlogApplicationTests test`

Expected: all pass and Flyway validates V1–V11.

- [ ] **Step 6: Commit explicit site APIs**

```bash
git add SelfWebBlog/src/main/resources/db/migration/V11__site_appearance_preferences.sql \
  SelfWebBlog/src/main/java/org/example/selfwebblog/site \
  SelfWebBlog/src/main/java/org/example/selfwebblog/admin/AdminSiteController.java \
  SelfWebBlog/src/test/java/org/example/selfwebblog/site
git commit -m "feat: add explicit home and appearance APIs"
```

### Task 4: Complete threaded interaction and moderation payloads

**Files:**
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/dto/AdminInteractionResponse.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/dto/InteractionItemResponse.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/InteractionService.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/AdminInteractionController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/dto/InteractionReplyRequest.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/interaction/InteractionServiceTest.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/interaction/InteractionControllerTest.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/interaction/InteractionDtoPrivacyTest.java`

- [ ] **Step 1: Write failing response tests for titles and expandable replies**

Create a root with two replies and assert the public item includes `titleName`, `titleStyle`, `likeCount`, `liked`, `pinned`, and `replies`. Assert the admin row additionally includes `avatarUrl`, title fields, `replyCount`, and never contains ciphertext, raw IP, or IP hash.

- [ ] **Step 2: Extend the admin response record**

```java
public record AdminInteractionResponse(
        String id,
        String targetType,
        String targetId,
        String rootId,
        String replyToId,
        String content,
        String userId,
        String nickname,
        String avatarUrl,
        String role,
        String titleName,
        String titleStyle,
        String status,
        String ipRegion,
        int likeCount,
        int replyCount,
        boolean pinned,
        LocalDateTime createTime) {}
```

- [ ] **Step 3: Add an admin thread endpoint**

```java
@GetMapping("/{id}/thread")
public Result<InteractionThreadResponse> thread(@PathVariable Long id) {
    return Result.success(interactionService.getModerationThread(id));
}
```

`getModerationThread` resolves the selected root, loads all direct replies including moderation states, and maps them with the same title/like fields. The existing reply endpoint and engagement like endpoint remain the mutation paths.

Remove the unused required `replyToId` field from `InteractionReplyRequest`; the path parameter is the only reply target and the body contains only validated `content`. Add a controller test proving `{ "content": "回复" }` succeeds.

- [ ] **Step 4: Keep permissions server-side**

Verify replies require authentication, public post comments require authentication, guestbook roots may be anonymous with CAPTCHA, only admins may pin/moderate/reveal IP, and deletion remains a soft delete that clears pin state.

- [ ] **Step 5: Run interaction and privacy tests**

Run: `mvn -Dtest=InteractionServiceTest,InteractionControllerTest,InteractionDtoPrivacyTest,LikeServiceTest test`

Expected: all tests pass and serialized public/admin DTOs contain no private IP material.

- [ ] **Step 6: Commit threaded moderation data**

```bash
git add SelfWebBlog/src/main/java/org/example/selfwebblog/interaction \
  SelfWebBlog/src/test/java/org/example/selfwebblog/interaction
git commit -m "feat: enrich threaded interaction moderation"
```

### Task 5: Split authentication responsibilities and remove duplicate API namespaces

**Files:**
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/identity/auth/AuthenticationService.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/identity/auth/GithubOAuthService.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/identity/auth/CredentialAuthController.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/identity/auth/GithubOAuthController.java`
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/identity/auth/SessionController.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/identity/auth/AuthenticationServiceTest.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/identity/auth/AuthRoutingTest.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AuthController.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/CommentController.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/entity/Comment.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/service/CommentService.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/service/impl/CommentServiceImpl.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/mapper/CommentMapper.java`
- Delete: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/ProfileController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/TagController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/UserController.java`
- Modify: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/UploadController.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/controller/ApiNamespaceTest.java`
- Modify: `SelfWebBlog/src/main/resources/application.yml`

- [ ] **Step 1: Pin the approved route contract in a failing MVC test**

```java
mockMvc.perform(post("/api/auth/login")).andExpect(status().isBadRequest());
mockMvc.perform(get("/api/auth/session")).andExpect(status().isOk());
mockMvc.perform(get("/api/site/home")).andExpect(status().isOk());
mockMvc.perform(get("/profile")).andExpect(status().isNotFound());
mockMvc.perform(get("/comments/post/1")).andExpect(status().isNotFound());
```

The browser redirect entry remains `GET /api/auth/github`; the provider callback remains `GET /auth/github/callback`; exchange remains `POST /api/auth/exchange`.

- [ ] **Step 2: Extract credential and session behavior without changing security rules**

`AuthenticationService` owns registration, password verification, admin-user reconciliation, session issue/revoke, rate-limit events, and conversion to one `UserSession` record. Preserve CSRF cookies, HttpOnly session cookies, token-version invalidation, security auditing, registration IP protection, and the already-approved removal of the special `/auth/admin` login endpoint.

- [ ] **Step 3: Extract GitHub OAuth behavior**

`GithubOAuthService` owns proxy detection, state cookies, provider token/user calls, admin GitHub ID matching, one-time login tickets, and redirect construction. `GithubOAuthController` only translates browser/API requests to the service.

- [ ] **Step 4: Make all ordinary controllers canonical `/api` only**

Change array mappings such as `@RequestMapping({"/posts", "/api/posts"})` to `@RequestMapping("/api/posts")`. Delete the legacy comment controller, entity, service and mapper because `/api/interactions` is the canonical implementation over the existing `comment` table. Do not delete or recreate that table. Delete the overloaded profile controller because `/api/site/*`, `/api/auth/session`, `/api/user/profile`, and `/api/admin/site` cover its responsibilities.

- [ ] **Step 5: Update the default OAuth callback**

```yaml
github:
  oauth:
    redirect-uri: ${GITHUB_REDIRECT_URI:http://localhost:8080/auth/github/callback}
```

Retain this exact non-API callback path in CORS/security allowlists.

- [ ] **Step 6: Run the complete backend suite**

Run: `mvn test`

Expected: all previous 69 tests plus the new post, site, interaction and auth tests pass; no test creates a second database or rewrites existing migrations.

- [ ] **Step 7: Commit the canonical backend**

```bash
git add SelfWebBlog/src/main/java SelfWebBlog/src/test/java SelfWebBlog/src/main/resources/application.yml
git commit -m "refactor: establish canonical blog API boundaries"
```

### Task 6: Generate the production visual source of truth and reset the frontend

**Files:**
- Create: `docs/design/personal-blog-visual-inventory.md`
- Create: `docs/design/concepts/home-desktop.png`
- Create: `docs/design/concepts/article-desktop.png`
- Create: `docs/design/concepts/editor-desktop.png`
- Create: `docs/design/concepts/workbench-desktop.png`
- Create: `docs/design/concepts/public-mobile.png`
- Delete: `SelfWebBlog/blog-frontend/src/**`
- Create: `SelfWebBlog/blog-frontend/src/main.js`
- Create: `SelfWebBlog/blog-frontend/src/App.vue`
- Create: `SelfWebBlog/blog-frontend/src/app/router.js`
- Create: `SelfWebBlog/blog-frontend/src/app/layouts/PublicLayout.vue`
- Create: `SelfWebBlog/blog-frontend/src/app/layouts/AuthLayout.vue`
- Create: `SelfWebBlog/blog-frontend/src/app/layouts/EditorLayout.vue`
- Create: `SelfWebBlog/blog-frontend/src/app/layouts/WorkbenchLayout.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/NotFoundPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/styles/tokens.css`
- Create: `SelfWebBlog/blog-frontend/src/shared/styles/base.css`
- Create: `SelfWebBlog/blog-frontend/src/shared/styles/glass.css`
- Create: `SelfWebBlog/blog-frontend/src/shared/styles/motion.css`
- Modify: `SelfWebBlog/blog-frontend/index.html`

- [ ] **Step 1: Invoke the frontend design and image generation workflow**

Generate five coordinated, readable production concepts using this exact brief:

```text
Redesign a Vue personal blog as “流动书页”: rounded liquid-glass surfaces,
open editorial layout, strong readable typography, restrained blue/violet/mint
palette, real code-native Chinese navigation labels only, no marketing slogan,
no sample article prose, no fake statistics, no eyebrow badges. The homepage
is content-first and asymmetric. Featured article surfaces can use the actual
cover, a validated custom image/solid/gradient, site default, or no background.
Use elegant SVG-like flowing curves and subtle motion cues. Design coordinated
screens for homepage, article reader with comments, split editor with live paste
image preview and one tag input, admin workbench, and mobile public layout.
Public navigation: 首页、文章、归档、留言、关于. Utilities: search, appearance,
account. Preserve visible content hierarchy and make every control practical to
implement in Vue/CSS. Do not render the product as generic bento card grids.
```

Save each accepted concept as the path listed above. If text, editor controls, comment anatomy, or workbench rows are unreadable, regenerate that screen at a larger standalone size before coding.

- [ ] **Step 2: Record the accepted design inventory**

`personal-blog-visual-inventory.md` must list exact visible labels, allowed first-viewport copy, palette, background temperature, typography, radii, borders, shadows, blur, container rules, every icon, the curve treatment, responsive behavior, and interaction states. Record that all article/user/site content is data-bound and that no mock concept title is allowed in production.

- [ ] **Step 3: Remove the old frontend source tree**

Delete every tracked and untracked file under `SelfWebBlog/blog-frontend/src`. Do not delete `public/images/emojis`, login/register illustrations, the lockfile, Vite/Playwright/ESLint/Prettier config, robots, or sitemap. Verify the deletion target resolves exactly to `E:\AAZ-StudyProfileslib\SelfWebBlog\SelfWebBlog\blog-frontend\src` before removing anything.

- [ ] **Step 4: Write the clean application bootstrap**

```js
// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './app/router'
import './shared/styles/tokens.css'
import './shared/styles/base.css'
import './shared/styles/glass.css'
import './shared/styles/motion.css'

createApp(App).use(router).mount('#app')
```

`App.vue` must only render the active route and global live regions. Layout ownership belongs to route components, not a monolithic application component.

- [ ] **Step 5: Prevent first-paint theme flash in `index.html`**

Insert this script before styles or module loading and remove hard-coded author names, remote favicons, promotional descriptions, and sample copy:

```html
<script>
  (() => {
    try {
      const saved = JSON.parse(localStorage.getItem('blog:appearance') || '{}')
      const systemDark = matchMedia('(prefers-color-scheme: dark)').matches
      const theme = saved.theme === 'light' || saved.theme === 'dark'
        ? saved.theme
        : (systemDark ? 'dark' : 'light')
      document.documentElement.dataset.theme = theme
      document.documentElement.dataset.motion = saved.pageMotion === false ? 'reduced' : 'full'
    } catch {
      document.documentElement.dataset.theme = 'light'
      document.documentElement.dataset.motion = 'full'
    }
  })()
</script>
```

- [ ] **Step 6: Add the accepted tokens and four layout shells**

Define tokens rather than one-off values. At minimum include background/surface/text/border/accent/semantic colors, radii, elevation, spacing, typography, blur, and motion values. `PublicLayout`, `AuthLayout`, `EditorLayout`, and `WorkbenchLayout` each own their header/navigation/container behavior and render `<RouterView />`.

- [ ] **Step 7: Make the empty replacement compile**

Define lazy routes for every required page with temporary route-level components that contain only a functional heading. Do not add article slogans or mock data. Run:

```powershell
pnpm test
pnpm lint
pnpm build
```

Expected: unit suite, lint, and production build pass with the clean source tree.

- [ ] **Step 8: Commit the approved concept and clean scaffold**

```bash
git add docs/design SelfWebBlog/blog-frontend/index.html SelfWebBlog/blog-frontend/src
git commit -m "feat: establish redesigned blog foundation"
```

### Task 7: Build the canonical client, session store, and route lifecycle

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/shared/api/client.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/error.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/auth.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/home.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/posts.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/tags.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/interactions.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/engagement.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/uploads.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/admin.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/site.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/api/client.test.js`
- Create: `SelfWebBlog/blog-frontend/src/app/session.js`
- Create: `SelfWebBlog/blog-frontend/src/app/legacyRoutes.js`
- Create: `SelfWebBlog/blog-frontend/src/app/legacyRoutes.test.js`
- Modify: `SelfWebBlog/blog-frontend/src/app/router.js`

- [ ] **Step 1: Write failing HTTP behavior tests**

Test that reads with the same key share one in-flight promise, a newer keyed read aborts the older request, writes obtain `/api/auth/csrf` before mutation, `401` clears the session, `403` remains an authorization error, and `Result.traceId/retryAfterSeconds` survive normalization.

- [ ] **Step 2: Implement one Axios client**

```js
export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  timeout: 15_000
})
```

`request()` must unwrap `{ code, msg, data, traceId, retryAfterSeconds }`, preserve `AbortSignal`, run a single CSRF bootstrap before non-GET requests, and emit `auth:expired` only for `401` or explicit expired-session payloads.

- [ ] **Step 3: Implement session restoration and route guards**

`session.js` exposes readonly `user`, `status`, `restore()`, `save()`, and `logout()`. The router restores once, redirects unauthenticated protected routes to `/login?redirect=...`, rejects non-admin workbench/editor access, processes OAuth `ticket/error`, restores scroll positions, handles anchors, and focuses the first page heading after navigation.

- [ ] **Step 4: Preserve old inbound links without preserving old UI**

```js
export function legacyTarget(url) {
  const parsed = new URL(url)
  if (/^#\/post\?id=\d+$/.test(parsed.hash)) return parsed.hash.replace('#', '')
  if (parsed.pathname === '/post' && /^\d+$/.test(parsed.searchParams.get('id') || '')) {
    return `/post/${parsed.searchParams.get('id')}`
  }
  if (parsed.pathname === '/profile') return '/admin'
  return null
}
```

- [ ] **Step 5: Run client/model tests and build**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: all new client and legacy-route tests pass and no client imports `/comments` or `/profile`.

- [ ] **Step 6: Commit client infrastructure**

```bash
git add SelfWebBlog/blog-frontend/src/shared/api SelfWebBlog/blog-frontend/src/app
git commit -m "feat: add canonical frontend data layer"
```

### Task 8: Build shared liquid-glass UI, themes, curves, and pointer trail

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/AppButton.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/IconButton.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/AppField.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/AppDialog.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/OverflowMenu.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/ToastHost.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/EmptyState.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/InlineError.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/appearanceModel.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/appearanceModel.test.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/themeTransition.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/CurveLayer.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/PointerTrail.vue`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/pointerPolicy.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/motion/pointerPolicy.test.js`
- Create: `SelfWebBlog/blog-frontend/src/shared/ui/AppearanceControl.vue`

- [ ] **Step 1: Write failing appearance and pointer-policy tests**

```js
assert.equal(resolveTheme({ saved: 'dark', systemDark: false }), 'dark')
assert.equal(resolveTheme({ saved: 'system', systemDark: true }), 'dark')
assert.equal(canRunPointerTrail({ fine: true, visible: true, focused: true,
  reduced: false, enabled: true }), true)
assert.equal(canRunPointerTrail({ fine: false, visible: true, focused: true,
  reduced: false, enabled: true }), false)
```

- [ ] **Step 2: Implement the appearance store**

Persist exactly `{ theme: 'system'|'light'|'dark', pageMotion: boolean, pointerTrail: boolean }`. Listen to system theme changes only while theme is `system`, and update `data-theme`/`data-motion` atomically.

- [ ] **Step 3: Implement interruptible theme transition**

Use `document.startViewTransition` when available and motion is allowed. Calculate a circle from the clicked control to the farthest viewport corner, animate the root clip-path for 360 ms, and use a monotonically increasing transition token so rapid clicks only commit the last requested theme. Fall back to tokenized CSS color transitions; reduced motion switches immediately.

- [ ] **Step 4: Implement the pointer trail lifecycle**

Render a small canvas-based, palette-aware ribbon/particle trail. Start only when `canRunPointerTrail` is true. On blur, visibility change, coarse pointer, reduced motion, route unmount, or user disable: cancel `requestAnimationFrame`, clear particles, detach listeners, and clear the canvas.

- [ ] **Step 5: Build accessible shared controls**

Dialogs trap focus and restore it; menus use button/menu roles, Arrow/Escape behavior, outside-click close and permission-controlled items; icon buttons require an accessible label; toasts use a live region; all controls use deliberate UI typography from tokens.

- [ ] **Step 6: Verify theme and motion foundations**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: rapid-theme and pointer-policy tests pass, build contains no browser-default unstyled control path, and reduced motion disables decorative animation.

- [ ] **Step 7: Commit the shared design system**

```bash
git add SelfWebBlog/blog-frontend/src/shared SelfWebBlog/blog-frontend/src/app/layouts
git commit -m "feat: build liquid glass design and motion system"
```

### Task 9: Rebuild home, article discovery, archive, tags, calendar, and about

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/features/content/model/postPresentation.js`
- Create: `SelfWebBlog/blog-frontend/src/features/content/model/postPresentation.test.js`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/SiteHeader.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/FeaturedPost.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/PostStream.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/SearchDialog.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/ArchiveTimeline.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/TagDirectory.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/ContentCalendar.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/HomePage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/ArticlesPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/ArchivePage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/TagsPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/TagDetailPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/CalendarPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/AboutPage.vue`

- [ ] **Step 1: Write titleless and background-presentation tests**

```js
assert.deepEqual(presentPost({ title: '', content: '第一段正文\n\n后续' }).headline, '')
assert.equal(presentPost({ title: '', content: '第一段正文\n\n后续' }).excerpt, '第一段正文')
assert.equal(presentPost({ title: '', content: '![](/uploads/a.webp)' }).leadImage,
  '/uploads/a.webp')
assert.equal(backgroundFor({ backgroundMode: 'NONE' }).kind, 'none')
```

No presentation helper may return the visible string “无标题”. Background values are parsed as validated data, never inserted as raw CSS.

- [ ] **Step 2: Build the public header and home against `/api/site/home`**

Implement only the approved navigation labels. Use the accepted asymmetric featured surface, data-bound cover/custom background, flowing curve layer, real post stream, search entry, author entry, and necessary site data. Do not render slogans, mock article titles, fake totals, decorative badges, or a grid of nested cards.

- [ ] **Step 3: Build articles, archive, tags, and calendar from one query model**

`/articles` owns keyword/category/tag/sort query parameters. `/archive` is a chronological timeline. Tags and calendar use canonical server data and route back to article filters. Empty and error states use functional copy and retries only.

- [ ] **Step 4: Build the About page from site data**

Render configured avatar, biography, about Markdown, dates, status and allowed social links without generating replacement copy when a field is empty.

- [ ] **Step 5: Verify discovery routes**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: titleless/pure-image tests pass, all seven routes compile, and a source scan finds no approved-forbidden sample titles or marketing phrases.

- [ ] **Step 6: Commit public discovery**

```bash
git add SelfWebBlog/blog-frontend/src/features/content SelfWebBlog/blog-frontend/src/pages \
  SelfWebBlog/blog-frontend/src/app/router.js
git commit -m "feat: rebuild public blog discovery"
```

### Task 10: Rebuild article reading, interactions, accounts, and visitor profile

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/shared/styles/markdown.css`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/MarkdownArticle.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/ArticleToc.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/ImageLightbox.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/content/components/ShareCardDialog.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/model/thread.js`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/model/thread.test.js`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/model/permissions.js`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/components/InteractionItem.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/components/InteractionThread.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/components/ReplyGroup.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/interaction/components/InteractionComposer.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/auth/components/LoginForm.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/auth/components/RegisterForm.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/PostPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/GuestbookPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/LoginPage.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/VisitorProfilePage.vue`

- [ ] **Step 1: Write failing thread behavior tests**

Test one-level normalization, default reply collapse, reply-count expansion, optimistic like snapshot/rollback, reply insertion under the correct root, and menu permission results for guest/user/admin/root/reply combinations.

- [ ] **Step 2: Rebuild safe article rendering**

Preserve DOMPurify sanitization, responsive optimized image URLs, h2/h3 table of contents, reading progress, return-to-top, code copy, table horizontal scrolling, image lightbox, view recording, post like, current-URL share QR, comments, and admin edit entry. Derive all visible content from the article; do not insert a visible fallback title.

- [ ] **Step 3: Build the approved interaction anatomy**

Each item shows avatar, nickname, title, time and public region. Only like/reply remain at first level. Replies are collapsed by default and expand into one visual level plus composer. Pin/delete appear in a three-dot menu only when permitted. Use optimistic likes with rollback and disable interaction on non-published items.

- [ ] **Step 4: Preserve guestbook trust rules**

Anonymous users can submit only root guestbook entries with nickname, honeypot and CAPTCHA. Any reply routes an anonymous visitor to login with the current redirect. Pending entries show only the server-provided moderation state to their creator session.

- [ ] **Step 5: Rebuild account flows**

Keep password login, registration CAPTCHA, optional email/avatar URL, GitHub OAuth ticket/error exchange, HttpOnly session restore/logout, post-login redirect, visitor nickname update and avatar upload. Display role/title where supplied; never expose token or IP fields in client storage.

- [ ] **Step 6: Run focused and build checks**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: thread, permission, Markdown and auth model tests pass; article and guestbook routes have no console error with mocked API responses.

- [ ] **Step 7: Commit reading and account flows**

```bash
git add SelfWebBlog/blog-frontend/src/features/content \
  SelfWebBlog/blog-frontend/src/features/interaction \
  SelfWebBlog/blog-frontend/src/features/auth \
  SelfWebBlog/blog-frontend/src/pages SelfWebBlog/blog-frontend/src/shared/styles/markdown.css
git commit -m "feat: rebuild reading interaction and account flows"
```

### Task 11: Rebuild the editor with immediate pasted-image preview and one tag input

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/publishContent.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/publishContent.test.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/tagInput.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/tagInput.test.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/uploadQueue.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/uploadQueue.test.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/model/draftRepository.js`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/components/MarkdownEditor.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/components/MarkdownPreview.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/components/UploadQueue.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/components/TagInput.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/editor/components/PostPresentationSettings.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/WritePage.vue`

- [ ] **Step 1: Write the publication and tag tests**

```js
assert.equal(hasPublishableContent('正文'), true)
assert.equal(hasPublishableContent('![](/uploads/a.webp)'), true)
assert.equal(hasPublishableContent('<picture><img src="/uploads/a.webp"></picture>'), true)
assert.equal(hasPublishableContent(' # **  '), false)
assert.equal(hasPublishableContent('![上传中](blob:task-1)'), false)
assert.deepEqual(addTag(['Vue'], '  vue  '), ['Vue'])
assert.deepEqual(addTag([], '生活　随笔'), ['生活 随笔'])
```

Also test that IME composition suppresses Enter submission and that arbitrary visible text becomes a tag after composition ends.

- [ ] **Step 2: Write the upload-task tests**

Inject URL and upload adapters. Assert `URL.createObjectURL(file)` appears in preview before the upload promise resolves, task IDs replace only their own placeholder when uploads complete out of order, failures retain preview and allow retry/remove, and dispose aborts uploads and revokes every object URL.

- [ ] **Step 3: Implement immediate paste/drop preview**

On paste/drop, insert a stable task token that the preview renderer resolves to the task object URL. Start upload concurrently. On success replace only that task token with responsive permanent Markdown; on failure keep the task token and render retry/remove actions. Never treat `blob:`, `uploading:`, or `upload-failed:` tokens as publishable media.

- [ ] **Step 4: Persist drafts and pending image blobs**

Use IndexedDB for task metadata and `Blob` values, with a localStorage migration for old text-only draft keys. Restore new/existing post drafts separately, recreate object URLs after reload, and delete persisted blobs after permanent upload replacement or task removal.

- [ ] **Step 5: Build the single tag control**

Use an accessible combobox/chip input. Enter creates or reuses the normalized value; Backspace removes the final chip only when the input is empty; suggestions come from `/api/tags`; composition events prevent accidental submission. Internal state is always `string[]`, never a comma string.

- [ ] **Step 6: Build desktop/mobile editor layouts**

Desktop shows editor and live preview together. Mobile switches between the two. Keep Markdown commands, resource insertion, cover/background mode, image/solid/gradient values, focal point, draft save, edit mode and upload status. Publishing blocks only when there is no effective text/permanent media or when an upload is unresolved.

- [ ] **Step 7: Run editor tests and build**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: text-only and permanent-image-only publish, empty rejection, immediate object URL preview, retry, cleanup, tag normalization/IME, and draft restoration all pass.

- [ ] **Step 8: Commit the editor**

```bash
git add SelfWebBlog/blog-frontend/src/features/editor SelfWebBlog/blog-frontend/src/pages/WritePage.vue
git commit -m "feat: rebuild flexible article editor"
```

### Task 12: Rebuild the admin workbench and complete management flows

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/WorkbenchNav.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/OverviewPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/ContentPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/InteractionPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/LibraryPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/UsersPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/SitePanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/features/admin/components/SecurityPanel.vue`
- Create: `SelfWebBlog/blog-frontend/src/pages/AdminPage.vue`

- [ ] **Step 1: Build a section-driven workbench shell**

Use `/admin?section=overview|content|interactions|library|users|site|security`. Keep the public visual language but use rows, tables, filters and drawers rather than public article cards. Preserve keyboard focus and responsive navigation.

- [ ] **Step 2: Implement overview and content management**

Show real overview data only. Content provides published/draft filtering, query, edit navigation, batch selection/delete and explicit confirmations. Never use fake metrics to fill empty regions.

- [ ] **Step 3: Implement interaction management**

Show avatar, nickname, current title style, region, target, status and reply count. Expand replies in place. Like/reply use canonical interaction endpoints. Put pin/unpin and delete in the three-dot menu. Keep status filtering, batch moderation and audited IP reveal.

- [ ] **Step 4: Implement library, users, site and security panels**

Library combines tag create/rename/merge/delete with media list/scan/optimized variants/safe delete. Users support title name/style and deletion. Site supports author/about/social data plus article-background, theme-motion and pointer-trail defaults. Security shows server audit events only.

- [ ] **Step 5: Verify admin source and build**

Run: `pnpm test && pnpm lint && pnpm build`

Expected: all seven sections compile, no raw IP/hash field appears in ordinary user data, and every destructive action uses confirmation and a server permission check.

- [ ] **Step 6: Commit the workbench**

```bash
git add SelfWebBlog/blog-frontend/src/features/admin SelfWebBlog/blog-frontend/src/pages/AdminPage.vue \
  SelfWebBlog/blog-frontend/src/app/layouts/WorkbenchLayout.vue
git commit -m "feat: rebuild blog administration workbench"
```

### Task 13: Run isolated full-stack verification, visual fidelity QA, and cleanup

**Files:**
- Rewrite: `SelfWebBlog/blog-frontend/e2e/core-flows.spec.js`
- Modify: `SelfWebBlog/blog-frontend/playwright.config.js`
- Modify: `SelfWebBlog/blog-frontend/package.json`
- Modify: `scripts/smoke-test.ps1`
- Modify: `.gitignore`
- Delete: `.superpowers/brainstorm/**`
- Delete: frontend and browser QA artifacts not required by the product

- [ ] **Step 1: Make the standard frontend check complete**

```json
{
  "scripts": {
    "check": "pnpm test && pnpm lint && pnpm format:check && pnpm build"
  },
  "engines": { "node": ">=20.19" },
  "packageManager": "pnpm@10.33.0"
}
```

- [ ] **Step 2: Rewrite deterministic Playwright flows**

Cover public discovery and URL filters; titleless text and image-only cards without “无标题”; safe article reader/TOC/lightbox/share QR; account/OAuth/session redirect; immediate paste preview then permanent URL; text-only/image-only publish and empty rejection; tag create/reuse/sync; comment title/like rollback/reply expansion/menu permissions; guest CAPTCHA; all seven admin sections; theme first paint/rapid toggle/reduced motion; legacy links; keyboard/dialog/menu behavior; and no horizontal overflow at desktop, tablet and mobile widths.

- [ ] **Step 3: Add an isolated full-stack test profile**

Start the backend with an explicit temporary data directory, never the user's real database:

```powershell
$env:BLOG_DATA_DIR = Join-Path $env:TEMP 'selfwebblog-e2e'
$env:BLOG_ADMIN_USERNAME = 'e2e-admin'
$env:BLOG_ADMIN_PASSWORD = 'e2e-password'
mvn spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Then run Playwright with required admin credentials; missing credentials must fail the full-stack job rather than silently skip admin tests.

- [ ] **Step 4: Run all automated gates**

```powershell
cd SelfWebBlog/blog-frontend
pnpm install --frozen-lockfile
pnpm check
pnpm test:e2e

cd ..
mvn test
mvn '-Dtest=ContentMigrationTest,InteractionMigrationTest' test
mvn package
```

Expected: all unit/integration/E2E tests pass, production bundles build, no console/page errors or HTTP 5xx occur, and migrations preserve old row counts while adding defaults and indexes.

- [ ] **Step 5: Verify the visible product in Browser/IAB**

Exercise home → articles/search → archive/tag/calendar → article/like/comment/share; register/login/profile/logout; write paste image → preview → tags → publish; and every admin section. Check current browser, 1440×1000, 768×1024 and 390×844 viewports, light/dark themes, keyboard navigation and reduced motion.

- [ ] **Step 6: Compare accepted concepts and final screenshots with `view_image`**

Inspect the accepted and rendered images directly. Write a fidelity ledger covering at least copy, layout, typography, palette, glass treatment, curves, cover behavior, spacing/container model, icons, motion and responsive collapse. Fix every agency-review-level mismatch before proceeding. Confirm the above-the-fold copy diff contains no invented title, slogan, badge, fake metric or explanatory line.

- [ ] **Step 7: Run a migration rehearsal on a copy of production data**

Copy the real database to a separate explicit rehearsal directory, run production-profile migration, confirm the migration backup exists and opens, compare article/user/comment/tag/image counts, query new background defaults and indexes, then discard only the rehearsal directory. Never point this test at the live data directory.

- [ ] **Step 8: Stop the brainstorming server and clean temporary artifacts**

Stop the exact session recorded under `.superpowers/brainstorm`, verify its resolved path remains inside the repository, remove that session directory, and add `.superpowers/` to `.gitignore`. Preserve accepted concept images and final evidence listed in the design inventory.

- [ ] **Step 9: Commit integration and verification updates**

```bash
git add SelfWebBlog/blog-frontend SelfWebBlog/src scripts/smoke-test.ps1 .gitignore docs/design
git commit -m "test: verify rebuilt personal blog experience"
```

## Final acceptance checklist

- Backend tests include text-only/image-only/full-empty publication, canonical tags, site home, interaction privacy and canonical auth routes.
- The old frontend source implementation is gone; the new directory structure owns every route and feature.
- No visible hard-coded sample article, fake metric, generic personal-blog slogan or generated placeholder copy remains.
- Article background follows cover by default and supports validated custom image, solid, gradient, site default and none modes.
- Paste/drop images preview immediately, survive recoverable draft state, replace precisely after upload, and clean up blobs/controllers.
- The one tag input creates/reuses arbitrary normalized tags and all tag consumers stay synchronized.
- Comments show titles, likes roll back on failure, replies expand one level, and pin/delete live in permission-aware overflow menus.
- Light/dark switching is flash-free, interruptible and smooth; reduced motion and coarse pointers disable decorative motion.
- Public, account, editor and all seven admin workflows pass browser tests on desktop, tablet and mobile.
- Existing data migrates without loss and the deployment health check passes.
