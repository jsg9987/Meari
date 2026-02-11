import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { useAuthStore } from './store/auth.store';
import './App.css';

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth);
  const fetchUserInfo = useAuthStore((state) => state.fetchUserInfo);
  const logout = useAuthStore((state) => state.logout);

  const INACTIVITY_LIMIT_MS = 30 * 60 * 1000;
  const LAST_ACTIVE_KEY = 'last_active_at';

  useEffect(() => {
    checkAuth();
    fetchUserInfo();
  }, [checkAuth, fetchUserInfo]);

  useEffect(() => {
    const updateLastActive = () => {
      localStorage.setItem(LAST_ACTIVE_KEY, Date.now().toString());
    };

    const checkInactivity = () => {
      const lastActive = Number(localStorage.getItem(LAST_ACTIVE_KEY));
      if (!lastActive) return;
      if (Date.now() - lastActive > INACTIVITY_LIMIT_MS) {
        logout();
      }
    };

    updateLastActive();

    const events: Array<keyof WindowEventMap> = [
      'mousemove',
      'keydown',
      'click',
      'scroll',
      'touchstart'
    ];

    events.forEach((eventName) =>
      window.addEventListener(eventName, updateLastActive, { passive: true })
    );

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        checkInactivity();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    const intervalId = window.setInterval(() => {
      if (!document.hidden) {
        checkInactivity();
      }
    }, 60 * 1000);

    return () => {
      events.forEach((eventName) =>
        window.removeEventListener(eventName, updateLastActive)
      );
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.clearInterval(intervalId);
    };
  }, [logout]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      if (!document.hidden) {
        fetchUserInfo();
      }
    }, 5 * 60 * 1000);

    return () => window.clearInterval(intervalId);
  }, [fetchUserInfo]);

  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}

export default App;
