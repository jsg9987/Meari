import { useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { Hero3DStage } from "@/components/landing/hero-3d-stage"
import { FeatureCardsSection } from "@/components/landing/feature-cards-section"
import { HowItWorks } from "@/components/landing/how-it-works"
import { ShadowingDemo } from "@/components/landing/shadowing-demo"
import { AISection } from "@/components/landing/ai-section"
import { StudyToolsSection } from "@/components/landing/study-tools-section"
import { Footer } from "@/components/landing/footer"
import { getUserInfo } from "@/api/auth.api"

export default function Landing() {
  const navigate = useNavigate()

  useEffect(() => {
    // 로그인 상태 확인
    const checkAuthStatus = async () => {
      const token = localStorage.getItem('access_token')
      if (!token) return

      try {
        await getUserInfo()
        // 토큰이 유효하면 메인으로 이동
        navigate('/main')
      } catch (error: unknown) {
        // 404 또는 다른 에러면 랜딩 페이지 유지
        const axiosError = error as { response?: { status?: number } }
        if (axiosError.response?.status === 401 || axiosError.response?.status === 404) {
          localStorage.removeItem('access_token')
          localStorage.removeItem('last_active_at')
        }
      }
    }

    checkAuthStatus()
  }, [navigate])

  return (
    <main className="overflow-x-hidden">
      <Hero3DStage />
      <FeatureCardsSection />
      <HowItWorks />
      <ShadowingDemo />
      <AISection />
      <StudyToolsSection />
      <Footer />
    </main>
  )
}
