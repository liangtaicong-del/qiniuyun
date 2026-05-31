import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/request'

let _router = null
export const setRouter = (router) => { _router = router }

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')

  const isLoggedIn = ref(!!token.value)

  const _saveToken = (t, rt) => {
    token.value = t
    isLoggedIn.value = true
    localStorage.setItem('token', t)
    if (rt) {
      refreshToken.value = rt
      localStorage.setItem('refreshToken', rt)
    }
  }

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password })
    const data = res.data?.data
    if (data?.token) {
      _saveToken(data.token, data.refreshToken)
      if (data.user) {
        userInfo.value = data.user
        localStorage.setItem('userInfo', JSON.stringify(data.user))
      }
    }
    return res
  }

  const register = async (username, email, password) => {
    const res = await api.post('/auth/register', { username, email, password })
    const data = res.data?.data
    if (data?.token) {
      _saveToken(data.token, data.refreshToken)
      if (data.user) {
        userInfo.value = data.user
        localStorage.setItem('userInfo', JSON.stringify(data.user))
      }
    }
    return res
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch (e) {
      // 忽略logout失败，继续清理本地状态
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    if (_router) _router.push('/login')
  }

  const refreshAccessToken = async () => {
    if (!refreshToken.value) return false
    try {
      const res = await api.post('/auth/refresh', { refreshToken: refreshToken.value })
      const data = res.data?.data
      if (data?.token) {
        _saveToken(data.token, data.refreshToken || refreshToken.value)
        return true
      }
    } catch (e) {
      // refresh token 过期，清除登录状态
    }
    return false
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const res = await api.get('/user/profile')
      userInfo.value = res.data?.data
      localStorage.setItem('userInfo', JSON.stringify(res.data?.data))
    } catch (e) {
      if (e.response?.status === 401) {
        const refreshed = await refreshAccessToken()
        if (refreshed) {
          fetchUserInfo()
        } else {
          logout()
        }
      }
    }
  }

  if (isLoggedIn.value) {
    const saved = localStorage.getItem('userInfo')
    if (saved) {
      try {
        userInfo.value = JSON.parse(saved)
      } catch (e) {
        console.warn('No saved userInfo found:', e)
      }
    }
  }

  return {
    userInfo, token, refreshToken, isLoggedIn,
    login, register, logout, refreshAccessToken, fetchUserInfo
  }
})
