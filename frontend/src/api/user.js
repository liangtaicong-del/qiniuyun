import api from '@/utils/request'

export const getProfile = () => {
  return api.get('/user/profile')
}

export const updateProfile = (data) => {
  return api.put('/user/profile', data)
}

export const changePassword = (data) => {
  return api.post('/user/password', data)
}
