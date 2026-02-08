import { Routes, Route, Navigate } from "react-router-dom";
import Landing from "../pages/Landing";
import Login from "../pages/Login";
import Signup from "../pages/Signup";
import ShadowingRoom from "../pages/ShadowingRoom";
import Home from "../pages/Home";
import MyPage from "../pages/MyPage";
import KopicEvaluation from "../pages/KopicEvaluation";
import KopicReport from "../pages/KopicReport";
import WordStudy from "../pages/daily/WordStudy";
import SentenceOrder from "../pages/daily/SentenceOrder";
import { PublicRoute } from './guards'
import { ProtectedRoute } from './guards'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/landing" element={<Navigate to="/" replace />} />
      <Route
        path="/main"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/login"
        element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        }
      />
      <Route
        path="/signup"
        element={
          <PublicRoute>
            <Signup />
          </PublicRoute>
        }
      />
      {/* <Route path="/shadowing/:roomId" element={<ProtectedRoute><ShadowingRoom /></ProtectedRoute>} /> */}
      <Route path="/shadowing/:roomId" element={<ShadowingRoom />} />
      <Route path="/mypage" element={<MyPage />} />
      <Route path="/kopic/evaluation/:themeId" element={<KopicEvaluation />} />
      <Route path="/kopic/report" element={<KopicReport />} />
      <Route path="/daily/word-study" element={<WordStudy />} />
      <Route path="/daily/sentence-order" element={<SentenceOrder />} />
    </Routes>
  );
}
