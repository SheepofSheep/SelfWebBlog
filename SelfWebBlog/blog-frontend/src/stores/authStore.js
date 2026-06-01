import { ref } from 'vue'
import { getCurrentUser, getProfile, logout as logoutRequest } from '../api'

const user = ref(null)

function restoreUser() {
  const stored = localStorage.getItem('user')
  if (!stored) return null
  try {
    user.value = JSON.parse(stored)
    return user.value
  } catch {
    user.value = null
    return null
  }
}

function saveUser(nextUser) {
  user.value = nextUser
  localStorage.setItem('user', JSON.stringify(nextUser))
}

function clearUserState() {
  user.value = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

async function loadUserInfo() {
  const token = localStorage.getItem('token')
  if (!token) {
    user.value = null
    return null
  }

  try {
    const userInfo = await getCurrentUser()
    if (userInfo?.role === 'ADMIN') {
      try {
        const profile = await getProfile()
        saveUser({
          ...userInfo,
          nickname: profile?.blogInfo?.nickname || userInfo.username,
          avatarUrl: profile?.blogInfo?.avatarUrl || userInfo.avatarUrl
        })
      } catch {
        saveUser(userInfo)
      }
    } else {
      saveUser(userInfo)
    }
    return user.value
  } catch (error) {
    clearUserState()
    throw error
  }
}

async function logoutUser() {
  try {
    await logoutRequest()
  } finally {
    clearUserState()
  }
}

export function useAuthStore() {
  return {
    user,
    restoreUser,
    saveUser,
    clearUserState,
    loadUserInfo,
    logoutUser
  }
}
