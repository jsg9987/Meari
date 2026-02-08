"use client"

import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"

export function Navbar() {
  const navigate = useNavigate()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener("scroll", handleScroll, { passive: true })
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  return (
    <nav
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled
          ? "bg-white/90 backdrop-blur-xl border-b border-gray-200/60 shadow-sm"
          : "bg-transparent"
      }`}
    >
      <div className="w-full flex justify-center px-6 py-4">
        <div className="w-full max-w-6xl flex items-center justify-between">
          {/* Logo */}
          <a href="#" className="flex items-center gap-2">
            <img src="/src/assets/images/common/logo-dark-2.svg" alt="logo" className="h-7" />
          </a>

          {/* CTA */}
          <div className="hidden md:flex items-center gap-3">
            <button
              onClick={() => navigate('/login')}
              className="text-sm text-navy-muted hover:text-navy transition-colors"
            >
              로그인
            </button>
            <button
              onClick={() => navigate('/signup')}
              className="text-sm text-white bg-accent-blue hover:opacity-90 px-4 py-2 rounded-lg transition-opacity font-medium"
            >
              회원가입
            </button>
          </div>
        </div>
      </div>
    </nav>
  )
}
