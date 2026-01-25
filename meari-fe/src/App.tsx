import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Signup from './pages/Signup';
import { useAuthStore } from './store/auth.store';
import './App.css';

const Home = () => {
  const { logout, user } = useAuthStore();
  return (
    <div>
      <h1>홈 페이지</h1>
      <p>환영합니다! {user?.email}님</p>
      <button onClick={logout}>로그아웃</button>
    </div>
  );
};

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

const PublicRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        } />

        <Route path="/signup" element={
          <PublicRoute>
            <Signup />
          </PublicRoute>
        } />

        <Route path="/" element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
