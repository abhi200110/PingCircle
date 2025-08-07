// src/pages/Login.jsx

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./button.css";
import SignupForm from "./SignupForm";
import axios from "axios";

export const Login = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSignup, setIsSignup] = useState(false);

  useEffect(() => {
    const storedUsername = localStorage.getItem("chat-username");
    if (storedUsername) {
      navigate("/chat");
    }
  }, [navigate]);

  const handleLogin = async () => {
    try {
      setError("");

      if (username && password) {
        const response = await axios.post("http://localhost:8080/api/users/login", {
          username,
          password,
        });

        if (response.status === 200) {
          localStorage.setItem("chat-username", username);
          localStorage.setItem("chat-token", response.data.token);
          navigate("/chat");
        }
      } else {
        setError("Please enter both username and password.");
      }
    } catch (error) {
      console.error("Error logging in:", error);
      if (error.response?.status === 401) {
        setError("Invalid username or password.");
      } else if (error.response?.status === 404) {
        setError("User not found.");
      } else {
        setError("An error occurred. Please try again.");
      }
    }
  };

  const handleSignupSuccess = (newUsername) => {
    localStorage.setItem("chat-username", newUsername);
    navigate("/chat");
  };

  return (
    <>
      <style>
        {`
          .login-bg {
            background: linear-gradient(to right, #eef2f3, #8ec5fc, #c3eafcff);
          }

          .login-card {
            backdrop-filter: blur(20px);
            background: rgba(255, 255, 255, 0.4);
            border: 1px solid rgba(0, 123, 255, 0.3);
            max-width: 400px;
            width: 90%;
            color: #003366;
            padding: 2rem;
            border-radius: 1.5rem;
            box-shadow: 0 0 30px rgba(0, 0, 0, 0.1);
          }

          .glass-input {
            background: rgba(255, 255, 255, 0.8);
            border: 1px solid #cfe2ff;
            border-radius: 12px;
            color: #003366;
            text-align: center;
          }

          .glass-input::placeholder {
            color: #6c757d;
          }

          .glass-input:focus {
            background: rgba(255, 255, 255, 0.95);
            outline: none;
            box-shadow: 0 0 0 2px rgba(13, 110, 253, 0.4);
            color: #003366;
          }

          .login-btn {
            background-color: #0d6efd;
            color: white;
            font-weight: bold;
            border-radius: 10px;
            transition: 0.3s ease;
          }

          .login-btn:hover {
            background-color: #0b5ed7;
          }

          .link-button {
            color: #0d6efd;
          }

          .pingcircle-title {
            font-size: 2rem;
            font-weight: 800;
            margin-bottom: 1.5rem;
            text-align: center;
            color: #0d6efd;
            text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.3);
          }
        `}
      </style>

      <div className="login-bg d-flex align-items-center justify-content-center vh-100">
        {isSignup ? (
          <SignupForm onSignupSuccess={handleSignupSuccess} onBackToLogin={() => setIsSignup(false)} />
        ) : (
          <div className="login-card shadow">
            <div className="pingcircle-title">PingCircle</div>

            {error && <div className="alert alert-danger py-1 px-2">{error}</div>}

            <input
              type="text"
              className="form-control glass-input mb-3"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyUp={(e) => e.key === "Enter" && handleLogin()}
            />

            <input
              type="password"
              className="form-control glass-input mb-3"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyUp={(e) => e.key === "Enter" && handleLogin()}
            />

            <button className="btn login-btn w-100 mb-3" onClick={handleLogin}>
              Connect
            </button>

            <div className="text-center">
              <button
                className="btn btn-link link-button"
                onClick={() => setIsSignup(true)}
              >
                Don't have an account? <strong>Sign up</strong>
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default Login;
