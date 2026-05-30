import api from '@/utils/request'

export const getUserPlatforms = () => {
  return api.get('/platforms/user')
}

export const getAllPlatforms = () => {
  return api.get('/platforms')
}

export const bindPlatform = (platform, authCode) => {
  return api.post('/platforms/bind', { platform, authCode })
}

export const unbindPlatform = (platform) => {
  return api.delete(`/platforms/${platform}/unbind`)
}
