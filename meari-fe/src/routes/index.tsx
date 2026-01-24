import { Routes, Route } from "react-router-dom";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/shadowing/:roomId" element={<ShadowingRoom />} />
    </Routes>
  );
}
