<template>
  <footer class="site-footer" aria-label="站点备案信息">
    <a
      class="filing-link"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >
      <span class="filing-seal" aria-hidden="true">晋</span>
      <span>晋ICP备2026007967-1号</span>
    </a>
  </footer>
</template>

<style scoped>
.site-footer {
  display: flex;
  justify-content: center;
  width: min(1180px, calc(100% - 32px));
  margin: 54px auto 0;
  padding: 0 0 24px;
}

.filing-link {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  gap: 9px;
  padding: 6px 14px 6px 7px;
  border: 1px solid color-mix(in srgb, var(--line) 78%, white);
  border-radius: 999px;
  color: var(--muted);
  background: color-mix(in srgb, var(--glass-strong) 72%, transparent);
  box-shadow:
    0 12px 32px color-mix(in srgb, var(--accent) 10%, transparent),
    inset 0 1px rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
  font-size: 12px;
  letter-spacing: 0.02em;
  transition:
    transform 180ms ease,
    color 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.filing-link:hover {
  border-color: color-mix(in srgb, var(--accent) 34%, var(--line));
  color: var(--accent-deep);
  box-shadow:
    0 15px 38px color-mix(in srgb, var(--accent) 15%, transparent),
    inset 0 1px rgba(255, 255, 255, 0.72);
  transform: translateY(-2px);
}

.filing-seal {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(145deg, var(--accent), var(--accent-deep));
  box-shadow: 0 7px 16px color-mix(in srgb, var(--accent) 24%, transparent);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 720px) {
  .site-footer {
    width: calc(100% - 24px);
    margin-top: 38px;
    padding-bottom: 18px;
  }
}
</style>
