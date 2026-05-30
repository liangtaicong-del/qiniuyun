import api from '@/utils/request'

export const getPublishTasks = (params) => {
  return api.get('/publish/tasks', { params })
}

export const cancelTask = (id) => {
  return api.post(`/publish/tasks/${id}/cancel`)
}

export const retryTask = (id) => {
  return api.post(`/publish/tasks/${id}/retry`)
}

export const publishArticle = (articleId, platforms, scheduledAt) => {
  return api.post('/publish', { articleId, platforms, scheduledAt })
}
