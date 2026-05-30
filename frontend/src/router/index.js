import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore, setRouter } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/pages/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/pages/Dashboard.vue')
        },
        {
          path: 'articles',
          name: 'Articles',
          component: () => import('@/pages/Articles.vue')
        },
        {
          path: 'articles/create',
          name: 'ArticleCreate',
          component: () => import('@/pages/ArticleEdit.vue')
        },
        {
          path: 'articles/:id/edit',
          name: 'ArticleEdit',
          component: () => import('@/pages/ArticleEdit.vue')
        },
        {
          path: 'platforms',
          name: 'Platforms',
          component: () => import('@/pages/Platforms.vue')
        },
        {
          path: 'publish',
          name: 'Publish',
          component: () => import('@/pages/Publish.vue')
        },
        {
          path: 'stats',
          name: 'Stats',
          component: () => import('@/pages/Stats.vue')
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/pages/Profile.vue')
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/pages/Settings.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/pages/NotFound.vue')
    }
  ]
})

setRouter(router)

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)

  if (requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
