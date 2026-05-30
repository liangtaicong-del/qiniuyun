import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref(localStorage.getItem('theme') || 'light')

  const setTheme = (newTheme) => {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    if (newTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else if (newTheme === 'light') {
      document.documentElement.removeAttribute('data-theme')
    }
  }

  const initTheme = () => {
    if (theme.value === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else if (theme.value === 'auto') {
      applyAutoTheme()
    }
  }

  const applyAutoTheme = () => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (prefersDark) {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
  }

  if (typeof window !== 'undefined') {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    mq.addEventListener('change', () => {
      if (theme.value === 'auto') applyAutoTheme()
    })
  }

  return { theme, setTheme, initTheme }
})
