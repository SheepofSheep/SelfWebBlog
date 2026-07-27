# Personal Blog Visual Inventory

## Accepted concepts

- `docs/design/concepts/home-desktop.png` — public home, 1536×1024.
- `docs/design/concepts/article-desktop.png` — article reader and comments, 1536×1024.
- `docs/design/concepts/editor-desktop.png` — editor, live preview and presentation settings, 1536×1024.
- `docs/design/concepts/workbench-desktop.png` — interaction management workbench, 1536×1024.
- `docs/design/concepts/public-mobile.png` — public mobile continuation, 853×1820.

The generated media and anonymous typographic lines indicate real data positions only. They are not production content and must not be copied into the application.

## Copy lock

Allowed public navigation: `首页`, `文章`, `归档`, `留言`, `关于`.

Allowed utility labels are functional only: search, appearance, account, edit, preview, save, publish, reply, like, share, tag, cover, article background, filters, pagination and admin section names.

No hero slogan, eyebrow, personal-blog description, sample article title, sample biography, fake statistic or substitute `无标题` string is allowed. Article, author, user, comment, tag, calendar and metric content always comes from API data.

## Color lock

- Canvas: cool blue-white, approximately `#f7faff`; never cream or warm gray.
- Primary text: deep ink navy, approximately `#15254b`.
- Secondary text: cool slate, approximately `#71809f`.
- Quiet lines: `#d7dfed`.
- Accent: periwinkle blue `#6478f5`.
- Supporting violet: `#aaa7f6`.
- Supporting mint: `#9ddfce`.
- Glass surface: `rgba(255, 255, 255, 0.62–0.82)` with a crisp white highlight edge.
- Glass border: white highlight plus a cool `rgba(123, 145, 190, 0.16)` lower edge.
- Shadow: broad cool shadow, never black or heavy.

Dark theme retains the hue relationships with a deep blue-black canvas, translucent navy surfaces, pale blue-white text and reduced shadow opacity.

## Typography

- UI chrome: `Inter`, `PingFang SC`, `Microsoft YaHei`, system sans-serif.
- Reading content: the same disciplined sans stack with a wider line height; no decorative display font.
- Public headings: 700–780 weight, compact tracking.
- Body: 400–500, 1.7–1.85 line height in articles.
- Controls: deliberate 12–14px equivalent; never browser-default typography.

## Geometry and containers

- Page maximum width: 1280px desktop with 24–40px gutters.
- Public header: quiet 64–72px band, no enclosing giant pill.
- Feature radius: 30–32px.
- Panel radius: 22–26px.
- Control radius: 12–16px; round icon buttons only where shown.
- Featured home: wide asymmetric media/content surface plus a narrow author rail.
- Latest posts: open rows with alternating media alignment, not a card grid.
- Article: calm central reading column, slim metadata rail and table-of-contents rail.
- Editor: real three-region desktop surface—editor, preview, settings—with upload queue below.
- Workbench: sidebar plus open table/row canvas; expanded replies remain one visual level.
- Mobile: feature surface, alternating rows, calendar, then five-item bottom navigation.

## Glass and media treatment

Glass uses blur, translucency, a thin highlight border and a restrained cool shadow. Nested glass is prohibited unless the child is a popover/menu or an editor control group.

Article cover imagery remains un-tinted. Text safety uses a localized white edge fade or adjacent glass region, not a colored overlay over the image. Real post settings choose cover, custom image, validated solid/gradient, site default or no background.

## Curves and motion

Three thin flowing paths—blue, violet and occasional mint—enter from page edges and sit behind content. They move slowly through stroke-dash offset/path interpolation and pause for reduced motion. They never receive pointer events.

Page/row reveals use short opacity/translate transitions. Article content remains stable. Theme switching expands from the appearance control when View Transitions are available. Pointer trail is a short translucent ribbon on fine pointers only.

## Icon inventory

Use Lucide outline icons where they match the concepts: search, sun/moon, user, menu, file/article, archive, message, tag, calendar, clock, bookmark, heart, reply, share, more-horizontal, pin, trash, shield, users, settings, save, image, upload and chevrons.

Stroke is 1.7–2px, typically 18–20px in controls and 16px in dense rows. Icons are optically centered, inherit `currentColor`, and do not sit in decorative containers unless they are interactive buttons.

## Responsive behavior

- At tablet widths, side rails collapse into inline disclosure regions.
- At mobile widths, the desktop navigation becomes icon utilities plus a menu and fixed/safe-area bottom navigation.
- Featured media/content remains one cohesive surface and stacks internally.
- Article stream alternates image/text rhythm without horizontal overflow.
- Editor switches between editing and preview while keeping settings in a drawer.
- Workbench sidebar becomes a drawer; rows keep actions accessible without hover.
- Touch targets are at least 44px and pointer trail is disabled.

## Interaction inventory

- Search dialog drives `/articles` URL filters.
- Theme, page motion and pointer trail are independently configurable.
- Article images open a lightbox; code blocks copy; share produces the current URL QR.
- Replies are collapsed by default and expand one level.
- Pin and delete are only in a permission-aware overflow menu.
- Editor paste/drop shows local media immediately, then replaces it with the permanent upload.
- One tag input creates or reuses normalized tags on Enter.
- Admin uses query-driven sections and real rows/tables rather than public cards.
