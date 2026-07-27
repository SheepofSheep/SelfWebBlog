<script setup>
import { ref } from 'vue'
import { X } from 'lucide-vue-next'
import { commitTag } from '@/core/tagModel.js'
const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const input = ref('')
const composing = ref(false)
function commit(event) {
  if (composing.value || event?.isComposing) return
  const value = commitTag(props.modelValue, input.value)
  emit('update:modelValue', value.tags)
  input.value = value.input
}
function remove(tag) {
  emit(
    'update:modelValue',
    props.modelValue.filter((item) => item !== tag)
  )
}
</script>
<template>
  <div class="tag-input">
    <span v-for="tag in modelValue" :key="tag"
      >{{ tag
      }}<button :aria-label="`删除 ${tag}`" @click="remove(tag)"><X :size="13" /></button></span
    ><input
      v-model="input"
      maxlength="30"
      placeholder="输入后按回车"
      @keydown.enter.prevent="commit"
      @blur="commit"
      @compositionstart="composing = true"
      @compositionend="composing = false"
    />
  </div>
</template>
