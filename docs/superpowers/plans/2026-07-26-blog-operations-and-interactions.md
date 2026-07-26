# Blog Operations and Interactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the complete public archive/calendar/profile/comment upgrade and the compact, data-capable administration workbench without losing existing SQLite or uploaded data.

**Architecture:** Add one non-destructive Flyway migration and make backend status, interaction, link, asset and trend services the source of truth. On the frontend, keep route pages thin by extracting pure models, shared glass UI primitives and one panel per admin module. Every behavior is introduced through a failing unit or integration test, then verified in desktop, tablet and mobile browser flows.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, JdbcTemplate, SQLite/Flyway, Vue 3, Vue Router, Vite 8, Node test runner, Playwright, CSS View Transitions and SVG.

---

## File map

### Backend

- `db/migration/V11__operations_interactions_and_links.sql`: archived timestamps, stable pin ordering and ordered site links.
- `content/post/PostStatusPolicy.java`: one definition of public and admin-visible post states.
- `controller/PostController.java`, `service/impl/PostServiceImpl.java`: archive/restore and explicit public filtering.
- `interaction/InteractionService.java`, interaction DTO/model/mapper files: current profile projection, semantic reply target and ordered pins.
- `site/SiteLinkService.java`, `site/SiteController.java`, `admin/AdminSiteController.java`: ordered custom link CRUD and public projection.
- `upload/asset/ImageAssetService.java`, `admin/AdminAssetController.java`: complete reference scan, pagination, reference details and safe orphan deletion.
- `admin/AdminDashboardController.java`, `admin/DashboardTrendService.java`: 14/30-day series.
- `controller/UserController.java`, `controller/AdminController.java`: safe profile mutation and validated title management.

### Frontend

- `core/archiveModel.js`, `calendarModel.js`, `threadModel.js`, `emojiCatalog.js`, `appearance.js`, `dashboardModel.js`: deterministic testable behavior.
- `components/AppAvatar.vue`, `AppDialog.vue`, `ConfirmDialog.vue`, `FormDialog.vue`, `CalendarWidget.vue`, `EmojiPicker.vue`, `CommentBody.vue`: reusable presentation and interaction primitives.
- `pages/ArchivePage.vue`, `HomePage.vue`, `ProfilePage.vue`, `components/SiteHeader.vue`, `components/InteractionThread.vue`: public experience.
- `features/admin/*Panel.vue`, `pages/AdminPage.vue`: compact workbench panels.
- `api/client.js`: typed endpoint surface for archive state, pin order, media, site links and trend data.
- `styles.css`: responsive layout, unified Dialog, calendar expansion and theme transitions.
- `e2e/core-flows.spec.js`: public and authenticated regression flows.

## Task 1: Add the non-destructive data model

**Files:**
- Create: `SelfWebBlog/src/main/resources/db/migration/V11__operations_interactions_and_links.sql`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/migration/ContentMigrationTest.java`
- Modify: `SelfWebBlog/src/test/java/org/example/selfwebblog/migration/InteractionMigrationTest.java`

- [ ] **Step 1: Write the failing populated-migration assertions**

```java
assertThat(columns(connection, "post")).contains("archived_time");
assertThat(columns(connection, "interaction")).contains("pin_order", "pinned_at");
assertThat(tableCount(connection, "site_link")).isEqualTo(4);
assertThat(rowCount(connection, "post")).isEqualTo(postsBefore);
assertThat(rowCount(connection, "interaction")).isEqualTo(interactionsBefore);
```

- [ ] **Step 2: Verify RED**

Run: `mvn -Dtest=ContentMigrationTest,InteractionMigrationTest test` from `SelfWebBlog/`.

Expected: FAIL because V11 columns and `site_link` do not exist.

- [ ] **Step 3: Add the migration**

```sql
ALTER TABLE post ADD COLUMN archived_time TEXT;
ALTER TABLE interaction ADD COLUMN pin_order INTEGER;
ALTER TABLE interaction ADD COLUMN pinned_at TEXT;

CREATE TABLE site_link (
  id INTEGER PRIMARY KEY,
  label TEXT NOT NULL,
  url TEXT NOT NULL,
  icon_key TEXT NOT NULL DEFAULT 'link',
  enabled INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_post_status_archived_time ON post(status, archived_time DESC);
CREATE INDEX idx_interaction_pin_order ON interaction(target_type, target_id, pinned DESC, pin_order, create_time DESC);
CREATE INDEX idx_site_link_order ON site_link(enabled DESC, sort_order, id);
```

Use SQLite JSON functions to seed non-empty GitHub, Bilibili, email and website values from `blog_info.social_links`; keep the JSON column unchanged for rollback compatibility.

- [ ] **Step 4: Verify GREEN and commit**

Run the two migration tests; expect PASS and unchanged pre-migration row counts.

Commit: `feat: add operations data model`

## Task 2: Implement a real archived article lifecycle

**Files:**
- Create: `SelfWebBlog/src/main/java/org/example/selfwebblog/content/post/PostStatusPolicy.java`
- Create: `SelfWebBlog/src/test/java/org/example/selfwebblog/content/post/PostStatusPolicyTest.java`
- Modify: `entity/Post.java`
- Modify: `controller/PostController.java`
- Modify: `service/PostService.java`
- Modify: `service/impl/PostServiceImpl.java`
- Modify: `content/tag/PostTagMapper.java`
- Modify: `content/calendar/ContentCalendarService.java`
- Modify: `analytics/PostViewService.java`
- Modify: `interaction/InteractionService.java`
- Modify: related controller/service tests

- [ ] **Step 1: Write failing lifecycle and public-visibility tests**

```java
assertThat(PostStatusPolicy.canTransition("PUBLISHED", "ARCHIVED")).isTrue();
assertThat(PostStatusPolicy.canTransition("ARCHIVED", "PUBLISHED")).isTrue();
assertThat(PostStatusPolicy.isPublic("ARCHIVED")).isFalse();
```

Add controller/service cases proving archived IDs return 404 anonymously and are excluded from list, search, tag, calendar, view counting and interaction creation.

- [ ] **Step 2: Verify RED**

Run: `mvn -Dtest=PostStatusPolicyTest,PostControllerTest,ContentCalendarServiceTest,PostViewServiceTest test`.

Expected: FAIL because `ARCHIVED` and status transitions are absent.

- [ ] **Step 3: Implement minimal lifecycle**

```java
public final class PostStatusPolicy {
    public static boolean isPublic(String status) { return "PUBLISHED".equals(status); }
    public static boolean canTransition(String from, String to) {
        return ("DRAFT".equals(from) && "PUBLISHED".equals(to))
                || ("PUBLISHED".equals(from) && "ARCHIVED".equals(to))
                || ("ARCHIVED".equals(from) && "PUBLISHED".equals(to));
    }
}
```

Add `PUT /api/posts/{id}/status` with `{ "status": "ARCHIVED" }`, set/clear `archivedTime`, and replace every public `.ne(status, "DRAFT")` with explicit `status = PUBLISHED`.

- [ ] **Step 4: Verify all affected backend tests and commit**

Run the targeted suite, then `mvn test`.

Commit: `feat: add article archive lifecycle`

## Task 3: Make profiles, titles, replies and pins consistent

**Files:**
- Modify: `controller/UserController.java`
- Modify: `controller/AdminController.java`
- Modify: `interaction/model/Interaction.java`
- Modify: `interaction/mapper/InteractionMapper.java`
- Modify: `interaction/dto/InteractionItemResponse.java`
- Modify: `interaction/dto/AdminInteractionResponse.java`
- Modify: `interaction/InteractionService.java`
- Modify: `interaction/AdminInteractionController.java`
- Create/modify: user, admin and interaction tests

- [ ] **Step 1: Write failing tests for safe profile updates and current-profile projection**

```java
mockMvc.perform(put("/api/user/profile").content("{\"nickname\":null}"))
       .andExpect(status().isBadRequest());
assertThat(publicItem.nickname()).isEqualTo("最新昵称");
assertThat(publicItem.avatarUrl()).isEqualTo("/uploads/avatars/latest.webp");
assertThat(publicItem.titleName()).isEqualTo("新称号");
```

Also prove deleted users fall back to snapshots.

- [ ] **Step 2: Write failing reply and pin-order tests**

```java
assertThat(reply.replyToNickname()).isEqualTo("被回复者");
assertThat(thread.items()).extracting(InteractionItemResponse::pinOrder)
        .containsExactly(1, 2, null);
```

Test pin, unpin, up/down reorder, reply-to-reply persistence and the rule that replies cannot be pinned.

- [ ] **Step 3: Verify RED**

Run: `mvn -Dtest=InteractionServiceTest,InteractionControllerTest,AdminControllerTest,UserControllerTest test`.

Expected: FAIL for missing current-profile projection, validation, reply target nickname and ordered pins.

- [ ] **Step 4: Implement the contract**

- Validate nickname length and nulls.
- Validate title length and style against `default|blue|green|gold|rose`.
- Batch-load active users for interaction pages and overlay nickname/avatar/title on snapshots.
- Populate `replyToNickname` from the target interaction/user.
- On pin, assign `max(pin_order)+1`; on unpin compact remaining values.
- Add `PUT /api/admin/interactions/{id}/pin-order` with `{ "direction": "UP" | "DOWN" }`.
- Sort both public and moderation roots by pinned, pin order and creation time.

- [ ] **Step 5: Verify GREEN and commit**

Run targeted tests and `mvn test`.

Commit: `feat: unify profiles replies and pinned comments`

## Task 4: Add arbitrary ordered site links

**Files:**
- Create: `site/SiteLink.java`
- Create: `site/SiteLinkService.java`
- Modify: `site/SiteController.java`
- Modify: `admin/AdminSiteController.java`
- Modify: `site/SiteProfileService.java`
- Create: `site/SiteLinkServiceTest.java`

- [ ] **Step 1: Write failing validation, CRUD and order tests**

```java
assertThat(service.create(new Request("文档", "https://example.test/docs", "book", true)).label())
        .isEqualTo("文档");
assertThatThrownBy(() -> service.create(new Request("坏链接", "javascript:alert(1)", "link", true)))
        .isInstanceOf(IllegalArgumentException.class);
assertThat(service.reorder(List.of(secondId, firstId))).extracting(SiteLink::id)
        .containsExactly(secondId, firstId);
```

- [ ] **Step 2: Verify RED**

Run: `mvn -Dtest=SiteLinkServiceTest test`; expect missing service failures.

- [ ] **Step 3: Implement CRUD and projections**

Expose:

```text
GET    /api/admin/site/links
POST   /api/admin/site/links
PUT    /api/admin/site/links/{id}
PUT    /api/admin/site/links/order
DELETE /api/admin/site/links/{id}
```

Validate trimmed label length 1–30, `https://` or valid `mailto:`, icon key length 1–30, and stable integer ordering. Include enabled links in `/api/site/about`.

- [ ] **Step 4: Verify GREEN and commit**

Run site tests and controller authorization tests.

Commit: `feat: add custom site links`

## Task 5: Complete media management and trend data

**Files:**
- Modify: `upload/asset/ImageAssetMapper.java`
- Modify: `upload/asset/ImageAssetService.java`
- Modify: `admin/AdminAssetController.java`
- Create: `admin/DashboardTrendService.java`
- Modify: `admin/AdminDashboardController.java`
- Modify/create: asset and dashboard tests

- [ ] **Step 1: Write failing complete-reference tests**

Create posts and site settings that reference each asset through `content`, `cover_url`, `image_url`, `background_value`, `avatar_url` and `bg_url`; assert every asset remains ACTIVE and returns its reference count/details.

- [ ] **Step 2: Write failing trend tests**

```java
DashboardTrend trend = service.load(14, LocalDate.of(2026, 7, 26));
assertThat(trend.days()).hasSize(14);
assertThat(trend.days().getFirst().views()).isZero();
assertThat(trend.days().getLast().interactions()).isEqualTo(2);
```

- [ ] **Step 3: Verify RED**

Run: `mvn -Dtest=ImageAssetServiceTest,DashboardTrendServiceTest test`.

- [ ] **Step 4: Implement media pagination, scan and safe deletion**

`GET /api/admin/assets?page=&size=&status=&purpose=&keyword=` returns a page. `POST /scan` returns active/orphan counts. `GET /{id}/references` returns article/site references. Unknown references keep an asset ACTIVE. Delete files first into a recoverable temporary location, delete the row after all moves succeed, then clean the temporary files after commit.

- [ ] **Step 5: Implement 14/30-day trend aggregation**

Aggregate `post_view_daily`, root/reply `interaction` and PUBLISHED `post` by Shanghai calendar day, fill missing dates with zero and return `views`, `interactions`, `posts` arrays plus accessible totals.

- [ ] **Step 6: Verify GREEN and commit**

Run targeted tests and `mvn test`.

Commit: `feat: complete media and dashboard analytics`

## Task 6: Add frontend behavior models and API contracts

**Files:**
- Create: `blog-frontend/src/core/archiveModel.js`
- Modify: `blog-frontend/src/core/calendarModel.js`
- Modify: `blog-frontend/src/core/threadModel.js`
- Create: `blog-frontend/src/core/emojiCatalog.js`
- Create: `blog-frontend/src/core/dashboardModel.js`
- Modify: `blog-frontend/src/core/appearance.js`
- Create/modify: matching `*.test.js`
- Modify: `blog-frontend/src/api/client.js`
- Modify: `blog-frontend/src/api/client.test.js`

- [ ] **Step 1: Write failing pure-model tests**

```js
assert.deepEqual(groupArchive(posts).map((group) => group.year), [2026, 2025])
assert.equal(parseComment('[sticker:happy]')[0].kind, 'sticker')
assert.deepEqual(sortPinned([{ id: 'a', pinned: true, pinOrder: 2 }, { id: 'b', pinned: true, pinOrder: 1 }]).map(x => x.id), ['b', 'a'])
assert.equal(fillTrend([], 14).length, 14)
```

Add tests for invalid sticker tokens, reply target labels, calendar request versions and latest-wins appearance commits.

- [ ] **Step 2: Verify RED**

Run: `pnpm test` from `blog-frontend/`; expect missing exports/modules.

- [ ] **Step 3: Implement minimal pure functions and API methods**

Add methods for article status, ordered pin moves, admin asset pagination/scan/delete/references, custom link CRUD/order and dashboard ranges. Keep every ID as a string.

- [ ] **Step 4: Verify GREEN and commit**

Run `pnpm test`.

Commit: `feat: add operations frontend models`

## Task 7: Build shared glass UI primitives

**Files:**
- Create: `components/AppAvatar.vue`
- Create: `components/AppDialog.vue`
- Create: `components/ConfirmDialog.vue`
- Create: `components/FormDialog.vue`
- Create: `components/EmojiPicker.vue`
- Create: `components/CommentBody.vue`
- Create: `components/CalendarWidget.vue`
- Modify: `styles.css`

- [ ] **Step 1: Add an E2E contract that fails on native dialogs**

```js
page.on('dialog', () => { throw new Error('native dialog used') })
await expect(page.getByRole('dialog')).toBeVisible()
```

Add component-facing E2E checks for avatar image/fallback, emoji picker, calendar day dialog and Escape close.

- [ ] **Step 2: Verify RED**

Run the focused Playwright project; expect missing role=dialog and current native prompt interception.

- [ ] **Step 3: Implement shared components**

`AppDialog` owns teleport, backdrop, focus trap, Escape and transition. `ConfirmDialog` and `FormDialog` wrap it. `AppAvatar` handles error fallback. `CommentBody` renders escaped text plus catalog stickers. `CalendarWidget` supports compact and expanded modes with one state model.

- [ ] **Step 4: Verify focused E2E and commit**

Commit: `feat: add shared glass interaction components`

## Task 8: Repair the public archive, calendar, header, profile and theme

**Files:**
- Modify: `pages/ArchivePage.vue`
- Modify: `pages/HomePage.vue`
- Delete: `pages/CalendarPage.vue`
- Modify: `router.js`
- Modify: `components/SiteHeader.vue`
- Modify: `pages/ProfilePage.vue`
- Modify: `components/AppearanceControl.vue`
- Modify: `core/appearance.js`
- Modify: `styles.css`
- Modify: `e2e/core-flows.spec.js`

- [ ] **Step 1: Write failing archive/calendar/profile/theme E2E cases**

Assert no archive link overlaps, `/calendar` redirects home, month controls change the label, a marked day opens all articles, profile save updates header avatar, rapid theme clicks end at the last mode and reduced motion disables expansion.

- [ ] **Step 2: Verify RED**

Run focused E2E in desktop and mobile; capture the expected failures.

- [ ] **Step 3: Implement public changes**

Use `groupArchive`, wrap each year's links in `.archive-year-posts`, load all post pages, replace the mini-calendar RouterLink with `CalendarWidget`, remove the route/page, use `AppAvatar`, and update the app store after profile mutations.

- [ ] **Step 4: Implement latest-wins theme transition**

The appearance controller increments a transition token, reads both saved motion and system reduced-motion, commits only the newest token, and applies a short `.theme-changing` fallback when View Transition is unavailable.

- [ ] **Step 5: Verify GREEN and commit**

Run unit tests, focused E2E, lint and build.

Commit: `feat: refine public discovery and appearance`

## Task 9: Rebuild the interaction composer and thread

**Files:**
- Modify: `components/InteractionThread.vue`
- Use: `components/EmojiPicker.vue`, `CommentBody.vue`, `AppAvatar.vue`, `ConfirmDialog.vue`
- Modify: `core/threadModel.js`
- Modify: `e2e/core-flows.spec.js`

- [ ] **Step 1: Write failing interaction E2E cases**

Create a root comment, insert a sticker, reply to the root, reply to that reply, like a reply, collapse/expand, edit a user title, pin three roots, move one upward and assert displayed order.

- [ ] **Step 2: Verify RED**

Expected failures: no sticker picker, no reply-on-reply control, no ordered pin action and stale profile/title projection.

- [ ] **Step 3: Implement one-level visual threads with arbitrary reply targets**

Use one composer state `{ rootId, replyToId, replyToNickname, content }`. Render all descendants in one reply list; every entry offers Reply and Like. Show semantic target text and use the safe comment body renderer.

- [ ] **Step 4: Implement admin three-dot actions and stable pin updates**

Pin/delete remain collapsed under the menu. On successful pin/order mutation, update and rerun `sortPinned`; on failure restore the previous array.

- [ ] **Step 5: Verify GREEN and commit**

Run interaction unit tests and public E2E.

Commit: `feat: complete threaded blog interactions`

## Task 10: Split and complete the administration workbench

**Files:**
- Create: `blog-frontend/src/features/admin/OverviewPanel.vue`
- Create: `ContentPanel.vue`
- Create: `InteractionsPanel.vue`
- Create: `TagsPanel.vue`
- Create: `MediaPanel.vue`
- Create: `UsersPanel.vue`
- Create: `SitePanel.vue`
- Create: `SecurityPanel.vue`
- Create: `AdminToolbar.vue`, `AdminPager.vue`
- Modify: `pages/AdminPage.vue`
- Modify: `styles.css`
- Modify: `e2e/core-flows.spec.js`

- [ ] **Step 1: Write failing admin E2E flows**

Cover trend range switching, archive/restore, comment filters/batch actions/pin order, title Dialog, profile avatar visibility, asset scan/filter/preview/delete, custom link add/reorder/toggle/delete and the absence of native dialogs.

- [ ] **Step 2: Verify RED**

Run the desktop admin project; expect missing controls and native prompt failures.

- [ ] **Step 3: Extract the shell without changing behavior**

Keep route-query tab synchronization and auth guard in `AdminPage`. Move each existing section to its named panel and pass only current-user/site shell data through props.

- [ ] **Step 4: Implement content and interaction panels**

Add filter toolbar, page state, selection, batch bar, archive/restore status groups and structured comment moderation. Use shared Dialogs for every mutation.

- [ ] **Step 5: Implement media, users and site panels**

Connect admin asset endpoints, provide grid/list views, show current avatars and validated title editing, and implement ordered custom link rows with add/edit/toggle/reorder/delete.

- [ ] **Step 6: Implement Overview SVG trends**

Render one responsive SVG with three labeled paths/areas and a keyboard-accessible daily summary list. Show database and orphan media health near the chart, not as unlabelled decoration.

- [ ] **Step 7: Verify GREEN and commit**

Run admin E2E, unit tests, lint, formatting and build.

Commit: `feat: complete the blog operations workbench`

## Task 11: Responsive visual and motion review

**Files:**
- Modify: `blog-frontend/src/styles.css`
- Modify: affected Vue components only when semantic markup is needed

- [ ] **Step 1: Capture failing visual evidence**

At 1440×960, 834×1112 and 390×844 inspect archive, calendar overlay, public thread and every admin panel in light/dark modes. Record horizontal overflow, clipped menus, low contrast, awkward empty space and motion discontinuity.

- [ ] **Step 2: Apply scoped visual fixes**

Use explicit grid minmax values, `min-width: 0`, stable avatar sizes, sticky admin toolbars, responsive list cards, Dialog max heights and consistent transition variables. Do not add slogans or placeholder copy.

- [ ] **Step 3: Re-run visual evidence and commit**

Confirm no overflow, all Dialogs fit, themes are readable and reduced motion is respected.

Commit: `fix: polish responsive blog operations UI`

## Task 12: Full regression, migration rehearsal and delivery

**Files:**
- Modify: `e2e/core-flows.spec.js` only for verified missing coverage
- Create: `docs/design/renders/operations-*.png` for final evidence

- [ ] **Step 1: Run frontend gates**

```powershell
cd SelfWebBlog/blog-frontend
pnpm test
pnpm lint
pnpm format:check
pnpm build
pnpm test:e2e
```

Expected: all unit/E2E tests pass, zero lint warnings and successful production build.

- [ ] **Step 2: Run backend and migration gates**

```powershell
cd SelfWebBlog
mvn test
```

Expected: every test passes; populated V10→V11 migration preserves all row counts and relationships.

- [ ] **Step 3: Run joint-browser smoke test**

Start with `start-dev.bat`, verify backend health, front proxy and the requirement-by-requirement browser checklist, then leave the requested preview running or stop it explicitly at handoff.

- [ ] **Step 4: Audit copy and data safety**

Search for prohibited AI-like sample copy, native Dialog calls, stale `/calendar` links and public `status != DRAFT` queries. Confirm no real database, upload or user content was deleted.

- [ ] **Step 5: Request code review, resolve findings and commit**

Commit: `feat: deliver blog operations and interaction upgrade`
