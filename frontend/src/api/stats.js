import api from '@/utils/request'

export const getOverviewStats = () => {
  return api.get('/stats/overview')
}

export const getPlatformStats = () => {
  return api.get('/stats/platforms')
}
