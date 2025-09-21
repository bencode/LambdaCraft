'use client'

import { useTheme } from 'next-themes'
import { useEffect, useState } from 'react'

export const ThemeToggle = () => {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <div className="fixed top-6 right-6 p-3 rounded-lg bg-[var(--surface)] border border-[var(--border)] shadow-sm">
        <div className="w-6 h-6" />
      </div>
    )
  }

  const getNextTheme = () => {
    switch (theme) {
      case 'light':
        return 'dark'
      case 'dark':
        return 'system'
      case 'system':
        return 'light'
      default:
        return 'light'
    }
  }

  const handleToggle = () => {
    const nextTheme = getNextTheme()
    setTheme(nextTheme)
  }

  const getIcon = () => {
    if (theme === 'system') {
      return (
        <div className="relative w-6 h-6">
          <svg
            className="w-6 h-6 text-purple-500 group-hover:scale-110 transition-transform"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path d="M10 2L13.09 8.26L20 9L14 14.74L15.18 21.02L10 18L4.82 21.02L6 14.74L0 9L6.91 8.26L10 2Z" />
          </svg>
          <span className="absolute -bottom-6 left-1/2 transform -translate-x-1/2 text-xs text-[var(--muted)] whitespace-nowrap">
            AUTO
          </span>
        </div>
      )
    }

    return theme === 'light' ? (
      <div className="relative w-6 h-6">
        <svg
          className="w-6 h-6 text-gray-800 group-hover:scale-110 transition-transform"
          fill="currentColor"
          viewBox="0 0 20 20"
        >
          <path d="M17.293 13.293A8 8 0 716.707 2.707a8.001 8.001 0 1010.586 10.586z" />
        </svg>
      </div>
    ) : (
      <div className="relative w-6 h-6">
        <svg
          className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition-transform"
          fill="currentColor"
          viewBox="0 0 20 20"
        >
          <path
            fillRule="evenodd"
            d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"
            clipRule="evenodd"
          />
        </svg>
      </div>
    )
  }

  return (
    <button
      onClick={handleToggle}
      className="fixed top-6 right-6 p-3 rounded-lg bg-[var(--surface)] hover:bg-[var(--surface-hover)] border border-[var(--border)] transition-all duration-300 group shadow-sm"
      aria-label={`Current theme: ${theme}. Click to cycle through light, dark, and system modes.`}
      title={`主题: ${theme === 'system' ? 'Auto' : theme === 'light' ? 'Light' : 'Dark'}`}
    >
      {getIcon()}
    </button>
  )
}
