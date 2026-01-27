import { Routes, Route } from "react-router-dom";
import Login from "../pages/Login";
import Signup from "../pages/Signup";
import ShadowingRoom from "../pages/ShadowingRoom";
import Home from "../pages/Home";
import { PublicRoute } from './guards'
// import {ProtectedRoute} from './guards'

export default function AppRoutes() {
  return (
    <Routes>
      {/* 테스트 목적으로 잠시 protect 해제 */}
      {/* <Route
        path="/"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      /> */}
      <Route
        path="/"
        element={
            <Home />
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
    </Routes>
  );
}
