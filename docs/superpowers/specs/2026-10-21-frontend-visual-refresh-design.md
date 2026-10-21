# Frontend Visual Refresh — Design

Date: 2026-10-21
Status: Implemented. Direction A — refine the existing neutral glass language.

## Problem

The live site placed a busy, high-saturation site wallpaper directly behind
translucent UI. With `--glass` at roughly 48% opacity, the rail, greeting, cards and
article body all competed with the wallpaper. In light mode the article hero also kept
dark text over a dark cover gradient. Typography and motion were serviceable, but the
surface hierarchy was too flat and content cards were visually identical to chrome.

## Direction

Keep the current structure — floating left rail, glass navigation/header, neutral
canvas, blue accent — and make the content read first. Content surfaces became quiet
paper; glass is reserved for chrome (rail, sticky header, overlays, sidebar widgets).
The wallpaper stays as an ambient colour field, pre-blurred once in a small canvas so
the browser does not pay for a full-viewport CSS filter on every frame.

## Surfaces and light

- Canvas: light `#edf0f5`, dark `#101216`. Three tiers: canvas, glass chrome, paper.
- Paper cards are 96% opaque in both themes. Content no longer sits on backdrop blur;
  the rail, header and small sidebar widgets remain glass.
- The wallpaper is rendered once into a 720px canvas at 38% blur strength (an 8px
  radius) and 90% saturation, then used as a JPEG background under a theme-aware veil
  (light 76%, dark 74%) plus the existing directional scrims.
- The appearance popover exposes a 0–100% background blur slider, persisted in
  localStorage (`background-blur`, default 38). Changes regenerate the small canvas
  after a short debounce; 0% uses the original full-resolution wallpaper without
  processing.
- Borders become cool ink at 14% in light mode; dark mode uses 13% white. Shadows are
  a close contact shadow plus a soft cast shadow.
- Accent: blue `#1f63e8` light, `#8ab8ff` dark; violet and mint only for accents.
  Solid accent surfaces use `--primary-ink`, including the calendar.
- The header is a sticky glass band; the rail and its active edge marker use the same
  stronger material.

## Typography

- UI scale: 12 / 13 / 14 / 16 / 20 / 24 / 32 / 40 / 48.
- Headings use 600. Chinese headings keep near-zero tracking; negative tracking is
  reserved for Latin display text.
- UI body 15px/1.65. Reading body 17px/1.9, measure about 72ch.
- `text-wrap: balance` on headings and `pretty` on paragraphs; tabular numbers for
  dates and counts.
- The decorative English eyebrows were removed. Page metadata now sits below the page
  `h1` (“共 9 篇”, “共 3 个标签”). The home `h1` is the featured post title when one
  exists; otherwise a visually hidden site title is used.

## Layout

- Home: main column plus a 320px rail, 64px gap. The intro is quieter, the search field
  is capped at 560px, and the featured card uses a 30% category field when the post has
  no cover.
- Latest posts are open rows with alternating media alignment, not a card grid. Rows
  use a 224px thumbnail, a 620px text measure and a 100% wide list; the even rows put
  the thumbnail at the far right. Text-only posts become full-width rows.
- `/articles` and tag results reuse the same row list; `/archive` stays a paper list.
- Article: 1040px page, 780px reading column plus a 200px sticky rail. Cover heroes are
  380px with a dark gradient and white text; coverless heroes are a paper band with ink
  text. Mobile collapses to a single column at 900px.
- Mobile: 64px header, 20px gutters, 64px bottom navigation plus safe area; the home
  rail stacks, author card hides and the featured category field hides.

## Motion

- Tokens: 140 / 220 / 420ms, `cubic-bezier(.2,.8,.2,1)`.
- Page content rises 8px and fades in once; list children stagger by 40ms up to six.
- Cards lift 3px with a cover scale of 1.03; buttons keep their press feedback.
- Skeletons use a shimmer; all effects respect `prefers-reduced-motion` and the in-app
  reduced-motion setting.

## Brand exception

The glass chrome is retained as the project's established material and the user's
chosen standard. The design-taste house rule against glass applies to content surfaces
and is satisfied there: cards and reading surfaces are paper, not glass. The taste
inspector could not run in this sandbox because spawning Chromium is denied; visual QA
used the existing browser tab at 1912px and same-origin 390/768 iframe previews.

## Scope

CSS tokens and layouts in `tokens.css`, `base.css`, `public.css`, `markdown.css`;
templates for real counts, the home `h1`, the article hero state, the post row variant
and the featured category field. No API, business logic or copy changes. Admin and
editor inherit the new tokens.
