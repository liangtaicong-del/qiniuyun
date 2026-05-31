import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let isRefreshing = false
let refreshSubscribers = []

const subscribeTokenRefresh = (callback) => {
  refreshSubscribers.push(callback)
}

const onTokenRefreshed = (newToken) => {
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
}

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(resolve => {
          subscribeTokenRefresh(newToken => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(api(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        isRefreshing = false
        triggerLogout()
        return Promise.reject(error)
      }

      try {
        const res = await axios.post(
          (import.meta.env.VITE_API_BASE_URL || '') + '/api/auth/refresh',
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        )
        const newToken = res.data?.data?.token
        if (newToken) {
          localStorage.setItem('token', newToken)
          if (res.data?.data?.refreshToken) {
            localStorage.setItem('refreshToken', res.data.data.refreshToken)
          }
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          onTokenRefreshed(newToken)
          isRefreshing = false
          return api(originalRequest)
        }
      } catch (refreshError) {
        isRefreshing = false
        triggerLogout()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

const triggerLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
  window.location.href = '/login'
}

export default api
