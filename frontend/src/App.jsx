import { Route, Routes, Navigate } from "react-router-dom";
import { Login } from "./Layout/Login";
import { ChatPage } from "./Layout/ChatPage";
import ErrorBoundary from "./components/ErrorBoundary";
import "./index.css";

function App() {
  return (
    <ErrorBoundary>
      <Routes>
        <Route
          path="/"
          element={
            localStorage.getItem("chat-username") ? (
              <Navigate to="/chat" replace />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route path="/login" element={<Login />} />
        <Route path="/chat" element={<ChatPage />} />
      </Routes>
    </ErrorBoundary>
  );
}

export default App;
