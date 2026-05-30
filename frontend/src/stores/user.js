import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/request'

let _router = null
export const setRouter = (router) => { _router = router }

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = ref(!!token.value)

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password })
    token.value = res.data?.token
    isLoggedIn.value = true
    localStorage.setItem('token', token.value)
    if (res.data?.user) {
      userInfo.value = res.data.user
      localStorage.setItem('userInfo', JSON.stringify(res.data.user))
    }
    return res
  }

  const register = async (username, email, password) => {
    const res = await api.post('/auth/register', { username, email, password })
    if (res.data?.token) {
      token.value = res.data.token
      isLoggedIn.value = true
      localStorage.setItem('token', token.value)
      if (res.data?.user) {
        userInfo.value = res.data.user
        localStorage.setItem('userInfo', JSON.stringify(res.data.user))
      }
    }
    return res
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    if (_router) _router.push('/login')
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const res = await api.get('/user/profile')
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    } catch (e) {
      console.error('Failed to fetch user info:', e)
    }
  }

  if (isLoggedIn.value) {
    const saved = localStorage.getItem('userInfo')
    if (saved) {
      try {
        userInfo.value = JSON.parse(saved)
      } catch {}
    }
  }

  return { userInfo, token, isLoggedIn, login, register, logout, fetchUserInfo }
})
