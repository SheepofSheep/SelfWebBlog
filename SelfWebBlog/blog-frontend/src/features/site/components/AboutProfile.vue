<script setup>
import { computed } from 'vue'
import { optimizedImageUrl, toAbsoluteUrl } from '../../../utils/url'

const props = defineProps({ profile: { type: Object, required: true } })
const avatar = computed(() => toAbsoluteUrl(props.profile.avatarUrl || ''))
</script>

<template>
  <section class="about-profile">
    <span class="profile-avatar">
      <img
        v-if="avatar"
        :src="optimizedImageUrl(avatar, 160)"
        :alt="profile.nickname || 'Gabriel'"
      />
      <span v-else>G</span>
    </span>
    <div>
      <p>关于作者</p>
      <h1>{{ profile.nickname || 'Gabriel' }}</h1>
      <div v-if="profile.bio" class="profile-bio">{{ profile.bio }}</div>
    </div>
  </section>
</template>

<style scoped>
.about-profile {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  align-items: start;
  gap: clamp(28px, 6vw, 72px);
  padding-block: clamp(30px, 6vw, 72px);
  border-bottom: 1px solid var(--border-strong);
}
.profile-avatar {
  width: 160px;
  aspect-ratio: 1;
  display: grid;
  overflow: hidden;
  place-items: center;
  border-radius: var(--radius-card);
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-family: var(--font-reading);
  font-size: 4rem;
  font-weight: 900;
}
.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
p,
h1 {
  margin: 0;
}
p {
  color: var(--accent-strong);
  font-size: 0.72rem;
  font-weight: 900;
}
h1 {
  margin-top: 8px;
  font-family: var(--font-reading);
  font-size: clamp(2.8rem, 7vw, 5.8rem);
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0;
}
.profile-bio {
  max-width: 55ch;
  margin-top: 24px;
  color: var(--text-secondary);
  font-family: var(--font-reading);
  font-size: 1rem;
  line-height: 1.9;
  white-space: pre-wrap;
}
@media (max-width: 620px) {
  .about-profile {
    grid-template-columns: 88px minmax(0, 1fr);
    gap: 18px;
  }
  .profile-avatar {
    width: 88px;
    font-size: 2rem;
  }
  h1 {
    font-size: 2.5rem;
  }
  .profile-bio {
    grid-column: 1 / -1;
  }
}
</style>
