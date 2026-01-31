import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { useAuthStore } from './store/auth.store';
import './App.css';

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth);
  const fetchUserInfo = useAuthStore((state) => state.fetchUserInfo);

  useEffect(() => {
    checkAuth();
    fetchUserInfo();
  }, [checkAuth, fetchUserInfo]);

  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}

export default App;
