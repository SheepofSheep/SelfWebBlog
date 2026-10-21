# Code Review 与优化建议

## 2026-10-17 修复状态

以下 7 项已在当前工作区修复；原审查正文保留用于对照，不能把其中的“尚未修复”直接当成当前状态。个人贡献仍需本人准确说明。

| 问题 | 当前行为 | 回归证据 |
| --- | --- | --- |
| R01 管理员配置密码轮换 | 启动时在事务中同步配置密码与角色，变化时原子增加 tokenVersion；未变化不重复撤销。支持明文配置和已有 BCrypt hash | `DatabaseInitConfigTest`，内存 SQLite 验证旧密码失效、版本增长和重复启动稳定 |
| 头像误删其他用户文件（追加发现） | 用户头像写入服务端生成的所有者标记，替换时只清理归属当前用户的文件；归属未知的旧头像保留 | `UserControllerTest` 使用临时目录验证其他用户文件保留、本人旧上传可清理 |
| R02 旧评论接口可见性 | 回复要求公开根存在、根属于同一文章和目标，隐藏、删除和孤立根的回复不再返回；保留旧分页结构和两个路由别名 | `InteractionPersistenceIntegrationTest.legacyCommentListHidesRepliesWhoseRootIsNotPublic` |
| R03 相同图片再上传冲突 | 按上传者/哈希/用途查全状态，重新激活孤立资产；原图缺失时保留原 ID、原 URL 修复，复用现有唯一键。上传和删除先取得 SQLite 写锁 | `ImageAssetUploadIntegrationTest`：孤立资源复用、原图修复、并发同图上传及上传/删除竞争 |
| 发布后新文章草稿重现（追加发现） | 等待状态更新触发的 watcher 完成后清除已保存草稿；保存期间的新增编辑仍保留在本地 | `WritePage.test.js`：发布后离页不重建草稿、请求期间新修改可恢复 |
| 匿名留言复用失效验证码（追加发现） | 成功发送后获取新 challenge，刷新时清除旧图片；连续两次留言使用各自的一次性验证码 | `InteractionThread.test.js` |
| R05 GitHub 与本地用户名冲突 | 按 github_id 保持身份独立，同名时分配不冲突的内部用户名；唯一键竞争后读取已存在的同一 GitHub 身份，不按同名合并本地账号 | `UserServiceTest`：本地名与备用名冲突、并发回调获胜身份复用 |

验证：离线 `mvn -o test` **206 项通过，0 失败/错误/跳过**；前端 `pnpm check` 通过（单测、lint、格式检查、生产构建）。新增组件回归执行真实 Vue setup 与响应式逻辑，网络和浏览器边界隔离，不能称为真实浏览器 E2E。全部数据库/文件测试使用内存、临时目录或 `target/test-data`，未操作真实业务数据、未安装新依赖。

R04、R06–R12 及 T01–T05 不属于本次 7 项修复的完成声明。旧头像缺少归属证明，因此没有自动删除或迁移历史文件；头像所有者标记只用于服务端清理判断，不作为访问令牌。图片上传的写锁会串行化相关写操作，符合当前单实例 SQLite 的边界，未产生性能容量结论。

---

## 原始审查快照（2026-10-09）

日期：2026-10-09。审查当前完整工作区；本次只新增复习文档，没有修复以下业务问题，也没有修改数据库、依赖或部署。严重度衡量当前项目的影响：P1 优先处理的凭证、可见性或明确业务故障；P2 常规正确性、资源成本、契约与维护问题；P3 文档和清理。没有发现有足够证据支持的 P0 灾难性漏洞。

**源码证实**表示代码路径确定；“触发影响”可能仍是静态推演。**合理推断 / 待验证**不会冒充运行复现、攻击记录或压测结论。下列顺序综合严重度、出现条件和修复收益，不按问题数量追求成果。

## 1. 优先级总览

| 编号 | 等级 | 问题 | 证据性质 | 建议次序 |
| --- | --- | --- | --- | --- |
| R01 | P1 | 管理员配置密码变更后，旧数据库密码仍可登录 | 双登录路径与种子逻辑源码证实 | 第一批 |
| R02 | P1 | 旧评论 GET 可返回隐藏/删除根下的公开回复 | 新旧可见性 SQL 差异源码证实 | 第一批 |
| R03 | P1 | ORPHANED 资产再次上传相同图片违反唯一键 | 查询条件与索引冲突源码证实 | 第一批 |
| R04 | P2 | 文章图片在尺寸校验前完整解码 | 调用顺序源码证实；资源影响未压测 | 第一批，改动集中 |
| R05 | P2 | GitHub 用户名与本地账号碰撞导致登录失败 | 外部用户名直用与唯一约束源码证实 | 第一批 |
| R06 | P2 | 归档时间清空可能被 MP 非空更新策略跳过 | 代码 + 框架机制推断，待 SQL 回归 | 第二批 |
| R07 | P2 | 删除用户留下点赞记录及计数语义不清 | 删除路径源码证实 | 第二批 |
| R08 | P2 | 资源管理 GET 做全量引用扫描和数据库更新 | 源码证实，延迟影响未量测 | 第二批 |
| R09 | P2 | 分页表象下仍有全量数据读取与无界回复 | 源码证实，规模影响条件化 | 第二批 |
| R10 | P2 | 坏 JSON/上传超限落通用 500，文章输入边界弱 | Advice 和输入模型源码证实 | 第二批 |
| R11 | P2 | 401 没有统一清登录状态 | 前端源码证实；只影响状态体验 | 第二批 |
| R12 | P3 | README 与真实客户端/目录不一致，存在未用依赖 | 源码与文档差异证实 | 随业务修复更新 |

每项建议回归均属于后续实施内容，本次未新增测试或跑服务验证这些问题。

## 2. 具体问题

### R01. 管理员密码轮换存在两个权威来源

- **证据位置**：[DatabaseInitConfig.java:34](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/config/DatabaseInitConfig.java:34)，已有用户分支 :58；[AuthController.login:244](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AuthController.java:244)，普通登录调用 :271；[UserService.login:47](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/service/UserService.java:47)。
- **当前实现【源码证实】**：配置管理员密码匹配时直接登录管理员；不匹配则继续数据库 BCrypt 登录。启动对已有管理员只设 ADMIN，不同步密码或 tokenVersion。
- **为什么有问题**：运维人员以为更新 `BLOG_ADMIN_PASSWORD` 已撤销旧凭证，但数据库旧 hash 仍是有效凭证。
- **触发与影响【静态路径推演】**：已有 DB，更改配置密码后重启；旧密码走普通登录仍得到同一 ADMIN 用户。旧登录 JWT 版本也不会因为配置变更自动失效。未验证线上是否曾变更密码或遭受利用。
- **修复建议**：明确数据库或配置作为唯一权威源。若配置用于轮换，应确认身份后用 matches 判断是否变化，在受控事务中更新 hash、原子递增 tokenVersion；不要每次启动无条件重新编码导致不必要撤销。避免按任意可变用户名意外提升普通账号。
- **是否优先**：是，涉及管理员凭证生命周期，优先于换认证框架。
- **建议回归**：同一隔离 DB 下初始化旧密码→轮换→新密码成功、旧密码失败、旧 JWT 失败；配置未变时版本稳定。
- **面试追问**：密码存储与密码轮换有何区别？JWT 如何在密码变化后撤销？

### R02. 旧评论接口遗漏主线程可见性

- **证据位置**：[CommentController.java:71](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/controller/CommentController.java:71)，`GET /comments/post/{postId}` 与 `/api/comments/post/{postId}`；[CommentServiceImpl.java:18](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/service/impl/CommentServiceImpl.java:18)；对照 [InteractionService.getThread:124](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/interaction/InteractionService.java:124)、`softDelete:475`。
- **当前实现【源码证实】**：旧路径只检查文章公开，然后查 `post_id=? AND status='PUBLISHED'` 的所有 comment。新路径分页公开根，再查询这些根的公开回复。隐藏/删除根不自动改变全部子回复自身状态。
- **为什么有问题**：审核意图是隐藏整个线程时，兼容路径仍按回复自身状态返回正文，公开接口的规则不一致。
- **触发与影响**：公开文章下已有公开回复，管理员隐藏或软删除根；访问旧 GET 仍可见这些回复。这里不是“已隐藏的根正文原样泄露”，泄露对象是仍标记公开的子回复。
- **修复建议**：旧读取适配统一线程服务，或显式补根存在、根公开、目标一致条件；明确兼容返回结构，避免只修前端或只封一条别名。
- **是否优先**：是，后台审核和公开可见性应一致。
- **建议回归**：匿名请求两条旧别名及新接口，对公开、HIDDEN、DELETED 根分别验证；同时确认不能跨文章拼回复。
- **面试追问**：软删除树结构如何保持可见性？兼容 API 为什么容易绕过新规则？

### R03. 图片复用查询与唯一约束的状态范围不一致

- **证据位置**：[ImageAssetMapper.java:14](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/upload/asset/ImageAssetMapper.java:14)，只查 ACTIVE；[V7__image_assets.sql:18](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/resources/db/migration/V7__image_assets.sql:18)，上传者/哈希/用途唯一；[ImageAssetService.java:111](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/upload/asset/ImageAssetService.java:111)，新插入 :142。
- **当前实现【源码证实】**：仅 ACTIVE 且原图存在才复用，否则重新保存并普通 INSERT；唯一键不区分引用状态。
- **为什么有问题**：查询认为“没有可复用记录”，约束却认为“这份内容已存在”，两者条件不一致。
- **触发与影响**：同上传者的相同 ARTICLE 图片已超过宽限并被标为 ORPHANED，尚未删除，再上传会唯一冲突；ACTIVE 记录原图丢失后再传也走冲突路径。并发首次相同上传也有竞争。结果是本可复用或修复的上传失败，通常成为通用错误。
- **修复建议**：按唯一业务键查所有状态；若文件完整，可重新激活并复用；文件缺失应受控修复现有记录。并发插入失败后按同一键读取正确结果，而非放宽唯一性掩盖问题。验证原图和必要派生图的完整性。
- **是否优先**：是，正常资源生命周期可触发，修复范围较集中。
- **建议回归**：ORPHANED 同内容、ACTIVE 原图缺失、两个并发相同上传、不同上传者相同内容；检查 DB 与磁盘都不多留文件。
- **面试追问**：幂等为什么要对齐查询条件与约束？check-then-insert 如何处理竞争？

### R04. 完整图片解码发生在尺寸校验之前

- **证据位置**：[ImageAssetService.java:116](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/upload/asset/ImageAssetService.java:116)；后续 :119 调 storage；[ImageValidator.java:124](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/service/upload/ImageValidator.java:124)。
- **当前实现【源码证实】**：资产服务先 `ImageIO.read(bytes)`，storage 才运行验证器；验证器本身先读宽高、检查边界，再完整解码。
- **为什么有问题**：调用者绕过了验证器防大尺寸分配的顺序，且可能重复解码。压缩字节上限不能等同像素内存上限。
- **触发与影响【合理推断】**：合法管理员上传体积合限但像素过大的文件，校验拒绝前已经占 CPU/堆，可能导致内存压力。上传入口受 AdminOnly 保护，不能描述为任何匿名访客可直接触发；未做内存压测。
- **修复建议**：元信息与尺寸校验先行，之后只进行必要次数的完整解码；可返回已校验图像供派生服务使用，保持边界清晰，避免为了此项引入通用图片任务平台。
- **是否优先**：值得，顺序修复成本相对小，保护服务器资源。
- **建议回归**：超限宽高在 read 完整像素前拒绝，正常 JPG/PNG/WebP 不受影响，坏文件被清晰拒绝。
- **面试追问**：压缩文件与解码内存关系？上传限制为什么要检查真实内容？

### R05. GitHub 新用户与本地用户名冲突

- **证据位置**：[UserService.java:91](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/service/UserService.java:91)，直接 `setUsername(username):98`、`save:105`；V1 `user.username UNIQUE`；`AuthController.githubCallback:313`。
- **当前实现【源码证实】**：按 github_id 查找，无此用户则以 GitHub login 作为本地 username 新建。
- **为什么有问题**：稳定提供方 ID 与本地展示/登录名是不同身份维度，两个合法账号可以同名。
- **触发与影响**：本地已注册用户名恰等于另一个首次 GitHub 用户的 login，新建触发唯一冲突，OAuth 回调失败。当前没有因此直接接管账号；不可把它夸大成已发生账号绑定漏洞。
- **修复建议**：外部身份以 provider/id 为准，生成不会碰撞的内部用户名或区分用户名命名空间。账号合并必须用户证明双方控制权，禁止按同名自动绑定。对同一 github_id 并发首次登录也应处理唯一冲突重读。
- **是否优先**：值得，影响已有核心登录功能，且无需完整重写身份系统。
- **建议回归**：本地同名账号、同名第三方首次登录、重复回调、稳定 ID 对应的 GitHub login 改名。
- **面试追问**：OAuth 认证后的本地账号映射为何重要？邮箱/用户名能作为自动绑定证据吗？

### R06. 归档时间设 null 未必持久化

- **证据位置**：[PostController.java:251](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/controller/PostController.java:251)、`applyStatus:264`；[Post.java:51](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/entity/Post.java:51)；对照 `Interaction.java:31` 的 ALWAYS。
- **当前实现【源码证实】**：归档转发布在 Java 对象上清 archivedTime，再 updateById；该字段没有显式 null 更新策略，配置未改全局策略。
- **为什么有问题【框架机制推断】**：MP 默认非空更新可能略过 null，导致数据库旧归档时间仍在；再次归档时又可能沿用旧值。[MP 字段策略说明](https://baomidou.com/en/reference/annotation/)。
- **触发与影响**：归档→发布→再归档，展示或后台筛选使用历史时间。需在隔离 SQLite 与当前 MP 版本下验证生成 SQL，本次没有运行该回归。
- **修复建议**：该业务确需清空时用定向 update set null 或针对字段配置 ALWAYS；明确全量/局部更新 DTO，避免全局 ALWAYS 意外清空其他字段。
- **是否优先**：第二批，改动小但先验证实际 SQL。不要误报互动取消置顶，该模型已显式允许 null 更新。
- **建议回归 / 面试追问**：完整三次状态转换后重新读库；ORM 更新策略为何与 Java 赋值不同？

### R07. 删除用户未定义点赞清理与冗余计数策略

- **证据位置**：[AdminController.java:124](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AdminController.java:124)，:139 仅 removeById；V5 interaction_like 没有用户级联外键。
- **当前实现【源码证实】**：不能删自己或 ADMIN；普通用户删除后 JWT 因查不到用户失效，但点赞记录和已有 count 不变。评论保留快照是另一项独立设计。
- **问题 / 触发影响**：被删用户曾点赞时产生悬空点赞，无法由该用户取消。若业务要保留历史点赞，必须明确其为历史记录；若要算有效用户点赞，则计数已不符合口径。不能未经定义就直接说所有 count 必须减少。
- **修复建议**：明确“注销保留历史”或“删除有效行为”的策略；需要清理时将记录删除和各目标计数更新放同事务，并有对账。评论快照可保留，不应盲目级联删除所有内容。
- **是否优先**：第二批，先定语义再修。
- **建议回归 / 面试追问**：含文章/评论点赞用户删除后记录、计数与公开行为一致；软删除、硬删除和历史审计如何权衡？

### R08. 资源列表 GET 每次全量扫描并更新状态

- **证据位置**：[AdminAssetController.java:29](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/admin/AdminAssetController.java:29)；[ImageAssetService.java:161](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/upload/asset/ImageAssetService.java:161)、`reconcileReferenceStatuses:199`、引用源 :381。
- **当前实现【源码证实】**：GET 列表进入事务、恢复暂存和扫描引用；读取全部资产和文章/站点文本，逐资产更新数据库，然后才分页。引用详情 GET 也会纠正状态。
- **问题 / 触发影响【规模条件】**：读列表具有写副作用，扫描成本随资产与源文本增长；即使状态未变也更新，会占 SQLite 写者与连接。重复打开管理页可重复付出成本，未实测延迟。
- **修复建议**：普通 GET 只读分页；引用重扫作为显式管理 POST，或在文章/站点变更时维护需要重扫标记。保持删除前重新核验与宽限，不能为提速去掉防误删规则。
- **是否优先**：第二批，收益随数据量增加；小数据也值得改善 HTTP 语义。
- **建议回归 / 面试追问**：列表无无关写 SQL，显式扫描和删除仍正确；GET 为什么应安全、幂等与只读是什么关系？

### R09. 全量读取、正文载荷及无界子回复

- **证据位置**：[AdminController.java:42](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AdminController.java:42)；[PostServiceImpl.java:16](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/service/impl/PostServiceImpl.java:16)；[archiveModel.js:37](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/blog-frontend/src/core/archiveModel.js:37)；`PostTagMapper:48`；`InteractionService:146`。
- **当前实现【源码证实】**：用户页分页后仍 list 全用户做 IP 分组；文章列表查询完整实体含正文，归档与文章集合逐页全取；标签页查全部文章；根评论分页后一次取全部子回复。
- **问题 / 触发影响**：分页上限不约束全量辅助读取或子集合，数据增长后增大 SQL、网络、堆和浏览器成本。当前项目规模与性能未量测；这不是 N+1，资料批量查询已避免典型 N+1。
- **修复建议**：用户重复标识计数做数据库聚合/按本页查询；列表 DTO 只返回需要字段；归档、标签、子回复服务端分页，保持稳定排序。搜索 date/LIKE 的索引问题先 EXPLAIN，不盲目新增索引。
- **是否优先**：第二批，先低成本 DTO/查询调整，再依数据量考虑游标与 FTS。
- **建议回归 / 面试追问**：大数量隔离数据下边界与返回量可控；根分页与子分页有什么区别？为什么没有 N+1 仍可能慢？

### R10. 客户端错误映射与文章 DTO 不完整

- **证据位置**：[GlobalExceptionHandler.java:20](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/exception/GlobalExceptionHandler.java:20)，通用异常 :49；`PostController.savePost:79`；`Post.java:21`。
- **当前实现【源码证实】**：已有校验、类型、缺参处理，但未单独处理 HttpMessageNotReadableException 和 MaxUploadSizeExceededException；文章直接接收实体，字段无 NotBlank/Size，新增实体还允许携入部分内部计数/时间字段。
- **问题 / 触发影响**：坏 JSON、部分上传超限会被通用 Exception 映射 500（超限也可能先被代理/容器拒绝，部署待验证）；空标题、过长正文或客户端内部字段缺少稳定契约。此接口仅管理员，不能描述为匿名权限提升。
- **修复建议**：专门输入 DTO，按业务确定标题/正文/摘要长度与状态规则，服务端设置内部字段；明确 400/413/冲突响应，错误响应不暴露数据库 message。不要为输入校验引入大量无需求规则。
- **是否优先**：第二批，利于接口与测试稳定。
- **建议回归 / 面试追问**：坏 JSON、错误类型、字节超限、实体内部字段注入都返回预期结果；`@Valid` 为什么不等于自动完整校验？

### R11. 401 后前端共享用户可能保持旧状态

- **证据位置**：[client.js:19](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/blog-frontend/src/api/client.js:19)；`stores/app.js:4`；`AdminPage.vue:62`。
- **当前实现【源码证实】**：client 抛带 status/payload 的 Error，没有统一失效处理；页面权限主要依初始化 ready 和共享 user，Cookie 过期后不必然重查。
- **触发与影响**：长时间打开页面、他处退出撤销或用户被删除后，界面还显示登录/管理入口，后端操作被拒。后端有真实权限检查，因此这里是体验/状态一致性问题，不是授权绕过。
- **修复建议**：通过少量统一事件或认证状态层处理 401，清用户、展示重登并保留安全的返回目标；区分 403，避免无限重试和隐式自动刷新。不要在网络层直接制造循环 Router 依赖。
- **是否优先 / 回归 / 追问**：第二批；验证过期、撤销、匿名与 403 四种情况；前端权限与后端权限职责为何不同？

### R12. README 与当前实现不一致

- **证据位置**：[前端 README](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/blog-frontend/README.md:48)；`client.js`；`WritePage.vue`；package.json、lockfile。
- **当前实现**：README 写 Axios 拦截器和部分未采用的目录/流程；代码是 fetch、textarea、本地集合筛选。Node“20+”未表达 Vite 8 的最低补丁版本；声明的 Axios/CodeMirror/NProgress 未在当前 src 导入。
- **触发与影响**：新人或本人按 README 复习，容易说出不存在的实现；未使用包增加依赖维护面，但不能直接说有可利用 CVE。
- **建议 / 优先**：随真实代码更新 README，核实无其他使用后再清理依赖与 lock；本次不更改。P3。
- **面试追问**：如何区分依赖声明、实际调用与框架底层掌握？

## 3. 尚需运行环境验证的风险

这些不是已复现缺陷，不和前面结论混为一谈。

### T01. SQLite 外键是否在每个连接启用

V6 声明 post_tag 外键，但生产配置/代码未发现 `PRAGMA foreign_keys=ON`。SQLite 需要按连接启用；因此记录为**待验证**，不能断言当前 DB 一定关闭。应在隔离连接及真实部署的只读检查中确认，并验证连接池所有连接行为。项目主动清理文章关联降低了部分影响，但不能代替未来所有写入的约束。[SQLite 外键原理](https://www.sqlite.org/foreignkeys.html)。面试关联：FK、连接级配置、级联与应用清理。

### T02. 时间口径混合

V1/V7 等使用 CURRENT_TIMESTAMP，Java 多处 LocalDateTime/LocalDate.now 使用服务器默认时区，DashboardTrendService:15/24 明确上海时区。**合理推断**：若部署默认 UTC，跨日统计、显示、归档和去重日期可能不一致。先查真实时区、历史数据口径，再选 UTC Instant 存储或明确统一业务时区，不能直接对历史数据盲目整体加八小时。面试关联：时间类型、UTC、时区、统计边界。

### T03. 站点资料读取中的嵌套 JDBC 连接获取

[SiteProfileService.get:31](E:/AAZ-StudyProfileslib/SelfWebBlog/SelfWebBlog/src/main/java/org/example/selfwebblog/site/SiteProfileService.java:31) 在 queryForObject 的 RowMapper 内调用 `linkService.listEnabled:47`，后者又做 JDBC 查询，get 没有外层事务。**机制推断**：无事务绑定连接时，外层 Connection 尚未释放，内层可能另取连接；池上限 2，两并发读取若各先占一连接，可能相互等待内层连接至超时。尚未调度复现，不应称已发生死锁。最简单改法是先查完资料释放连接，再查链接拼 DTO；或明确使用同一只读事务，评估一致性需求。面试关联：连接池饥饿、事务线程绑定、资源生命周期。

### T04. 浏览去重事务的读后写竞争

PostViewService.record:22 先读文章再插入，而点赞先取得写锁。WAL 下并发写入后读快照升级可能失败；unique 去重只保证重复记录，不保证所有瞬时锁竞争都成功。**待验证**：使用隔离 SQLite 并发回归检查 BUSY/BUSY_SNAPSHOT，按结果决定短事务、写入前锁或有限重试；不能为静态可能性引入复杂队列。[SQLite 隔离机制](https://www.sqlite.org/isolation.html)。

### T05. 生产配置与安全头实际落点

默认 dev、prod 校验条件化、Secure Cookie 配置化、forward headers none、HSTS 仅在 request.isSecure 时发出。**待验证**：生产是否激活 prod，HTTPS 在哪终止，代理是否覆盖用户转发头，前端 HTML 是否有 CSP/HSTS，history 路由是否正确回退、上传与备份是否被错误暴露。没有 nginx/systemd/容器配置证据，不能宣称这些均已部署，也不能因缺文件断言线上无 TLS。

## 4. 已有有效设计，避免重复误报

| 已解决的边界【源码证实】 | 证据与保留价值 |
| --- | --- |
| Token 不暴露给前端 JS；退出检查用户版本 | JwtFilter、SessionCookieService、logout；仍需 R01 轮换完善 |
| OAuth state 与浏览器绑定、票据一次消费 | OAuthStateService/OAuthLoginTicketService 原子 remove；不等于多实例共享 |
| 点赞重复不重复计数 | V5 唯一键、影响行数、事务内 SQL 增减 |
| 用户资料批量投影 | InteractionService.projectProfiles，避免逐回复查用户 |
| 置顶约束与清 nullable 元数据 | V12/V14、Interaction ALWAYS；不应误报取消置顶不能清空 |
| 上传路径边界和实际格式校验 | ImageValidator、随机名、normalize/realPath；仍有 R04 调用顺序缺陷 |
| 草稿/归档资源也纳入引用判断 | ImageAssetService 引用源，宽限及删除前重查，避免只查公开文章误删 |
| 文件删除失败补偿和重启恢复 | 暂存、清单、afterCompletion 和 PostConstruct；不等于任意故障完全原子 |
| 页面切换与异步结果清理 | App route.path key、requestId/generation、上传取消/Blob 回收 |
| 响应 HTTP 状态和请求关联 | ResultHttpStatusAdvice、trace/MDC finally 清理 |

安全审查未发现证据支持把当前参数绑定 SQL 报为 SQL 注入；未将有 AdminOnly 的头像/图片接口报为匿名上传；未把仅存在于测试构造器的服务缺省分支当成生产绕过。

## 5. 测试与可维护性结论

49 个 Java 测试源文件中可见参数、隐私、状态、数据库迁移/约束、文件删除恢复和并发正确性测试；前端模型也有异步与输入测试。它们比只有空 contextLoads 更有价值。但没有覆盖率工具结果，不能给覆盖率百分比；并发单元/集成测试不等于吞吐压测。

本次已验证：既有纯前端 **77 项单测通过**；会话此前当前认证改动下 **ApiNamespaceTest 3 项通过**。未执行其余 Maven、Playwright、真实 OAuth、构建或线上检查。核心 Playwright API mock，CI 未跑 Playwright，因此后端与浏览器联通仍需要独立验证。

后续优先补 R01–R05 的真实回归，尤其数据库唯一键、旧路由和事务提交后的结果，不能只用 mock mapper 返回成功来验证这些约束。再补时间、连接池和 FK 的隔离验证。InteractionService 约 892 行、ImageAssetService 约 699 行，有混合职责；先统一业务规则，再按查询、审核排序、隐私投影、引用扫描/删除恢复拆出明确边界。拆类是减少职责混杂，不是为凑设计模式。

## 6. 合理投入与过度设计

优先投入正确性、输入/错误契约、查询数据量、部署事实核对与恢复演练。后续按量测考虑 MySQL/PostgreSQL、共享临时状态、对象存储、FTS 或简单缓存。当前没有必要为了实习简历直接引入微服务、服务网格、Kafka、分布式事务或复杂权限中心。

可对面试官这样表达：“项目已有不少一致性和恢复处理，我审查后发现旧接口可见性和双凭证来源仍有边界缺口。我的改进排序是先修可验证的正确性，再控制全量查询，最后依据实际数据考虑架构变化；这些优化尚未实施。”
