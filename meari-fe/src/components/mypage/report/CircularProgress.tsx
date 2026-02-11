import { getScoreColor } from '../../../utils/reportUtils'

interface CircularProgressProps {
  score: number
  size?: number
  strokeWidth?: number
}

const CircularProgress = ({
  score,
  size = 60,
  strokeWidth = 6
}: CircularProgressProps) => {
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (score / 100) * circumference
  const color = getScoreColor(score)

  return (
    <div className='relative inline-flex items-center justify-center'>
      <svg width={size} height={size} className='transform -rotate-90'>
        {/* Background circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke='#e5e7eb'
          strokeWidth={strokeWidth}
          fill='none'
        />
        {/* Progress circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={color}
          strokeWidth={strokeWidth}
          fill='none'
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap='round'
          className='transition-all duration-500 ease-out'
        />
      </svg>
      <div className='absolute inset-0 flex items-center justify-center'>
        <span className='text-sm font-bold' style={{ color }}>
          {score}
        </span>
      </div>
    </div>
  )
}

export default CircularProgress
