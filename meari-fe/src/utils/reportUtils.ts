import type { ShadowingReport, ShadowingReportDetail } from '../api/mypage.api'

/**
 * 점수에 따른 색상 반환
 */
export const getScoreColor = (score: number): string => {
  if (score >= 80) return '#10b981' // green-500
  if (score >= 60) return '#f59e0b' // yellow-500
  if (score >= 40) return '#f97316' // orange-500
  return '#ef4444' // red-500
}

/**
 * 점수에 따른 Tailwind 클래스 반환
 */
export const getScoreColorClass = (score: number): string => {
  if (score >= 80) return 'text-green-600'
  if (score >= 60) return 'text-yellow-600'
  if (score >= 40) return 'text-orange-600'
  return 'text-red-600'
}

/**
 * 신뢰도에 따른 색상 반환
 */
export const getConfidenceColor = (confidence: number): string => {
  if (confidence >= 0.9) return '#10b981' // dark green
  if (confidence >= 0.7) return '#86efac' // light green
  if (confidence >= 0.5) return '#fbbf24' // yellow
  return '#ef4444' // red
}

/**
 * 상대적 시간 표시 (예: "2일 전")
 */
export const getRelativeTime = (dateString: string): string => {
  const now = new Date()
  const date = new Date(dateString)
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diffInSeconds < 60) return '방금 전'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}시간 전`
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}일 전`
  if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 604800)}주 전`
  return date.toLocaleDateString('ko-KR')
}

/**
 * 리포트 통계 계산
 */
export const calculateReportStats = (reports: ShadowingReport[]) => {
  if (reports.length === 0) {
    return {
      totalReports: 0,
      averageScore: 0,
      unreadCount: 0,
      scoresTrend: []
    }
  }

  const totalReports = reports.length
  const averageScore = Math.round(
    reports.reduce((sum, r) => sum + r.total_score, 0) / totalReports
  )
  const unreadCount = reports.filter((r) => !r.is_read).length

  // 최근 10개 리포트의 점수 추이 (오래된 순 -> 최신 순)
  const scoresTrend = [...reports]
    .slice(0, 10)
    .reverse()
    .map((r, index) => ({
      name: `#${index + 1}`,
      score: r.total_score,
      date: new Date(r.created_at).toLocaleDateString('ko-KR', {
        month: 'short',
        day: 'numeric'
      })
    }))

  return {
    totalReports,
    averageScore,
    unreadCount,
    scoresTrend
  }
}

/**
 * 오류 타입별 집계
 */
export const aggregateErrorTypes = (detail: ShadowingReportDetail) => {
  if (!detail.detailed_analysis?.sentences) {
    return []
  }

  const errorCounts: Record<string, number> = {}

  detail.detailed_analysis.sentences.forEach((sentence) => {
    sentence.errors.forEach((error) => {
      errorCounts[error.type] = (errorCounts[error.type] || 0) + 1
    })
  })

  const errorTypeNames: Record<string, string> = {
    delete: '누락',
    insert: '삽입',
    replace: '대체'
  }

  const errorTypeColors: Record<string, string> = {
    delete: '#ef4444', // red
    insert: '#3b82f6', // blue
    replace: '#f59e0b' // orange
  }

  return Object.entries(errorCounts).map(([type, count]) => ({
    name: errorTypeNames[type] || type,
    value: count,
    fill: errorTypeColors[type] || '#6b7280'
  }))
}

/**
 * 문장별 데이터 변환 (차트용)
 */
export const transformSentenceData = (detail: ShadowingReportDetail) => {
  if (!detail.detailed_analysis?.sentences) {
    return []
  }

  return detail.detailed_analysis.sentences.map((sentence, index) => ({
    name: `문장 ${index + 1}`,
    accuracy: sentence.accuracy,
    confidence: Math.round(sentence.mean_confidence * 100),
    intonation: sentence.intonation.score
  }))
}

/**
 * 음절별 데이터 변환 (차트용)
 */
export const transformSyllableData = (
  syllables: string[],
  confidences: number[]
) => {
  return syllables.map((syllable, index) => ({
    syllable,
    confidence: Math.round((confidences[index] || 0) * 100),
    fill: getConfidenceColor(confidences[index] || 0)
  }))
}
