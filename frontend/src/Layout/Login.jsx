// src/pages/Login.jsx

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./button.css";
import SignupForm from "./SignupForm";
import api, { setJwtToken } from "../config/axios";

export const Login = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSignup, setIsSignup] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const storedUsername = localStorage.getItem("chat-username");
    const storedToken = localStorage.getItem("jwt-token");
    if (storedUsername && storedToken) {
      navigate("/chat");
    }
  }, [navigate]);

  const handleLogin = async () => {
    try {
      setError("");
      setIsLoading(true);

      if (!username.trim() || !password.trim()) {
        setError("Please enter both username and password.");
        setIsLoading(false);
        return;
      }

      
      const response = await api.post("/users/login", {
        username: username.trim(),
        password: password.trim(),
      });


      if (response.status === 200 && response.data.token) {
        // Store JWT token using the new function
        setJwtToken(response.data.token);
        localStorage.setItem("chat-username", username);
        navigate("/chat");
      } else {
        setError("Invalid response from server. Please try again.");
      }
    } catch (error) {
      if (error.response) {
        // Server responded with error status
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 401) {
          setError("Invalid username or password.");
        } else if (status === 400) {
          setError(data || "Invalid request data.");
        } else if (status === 404) {
          setError("User not found.");
        } else if (status === 500) {
          setError("Server error. Please try again later.");
        } else {
          setError(`Server error (${status}): ${data || "Unknown error"}`);
        }
      } else if (error.request) {
        // Request was made but no response received
        setError("Cannot connect to server. Please check if the backend is running.");
      } else {
        // Something else happened
        setError("An error occurred. Please try again.");
      }
    } finally {
      setIsLoading(false);
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
            padding-left: 1rem;
            padding-right: 1rem;
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

          .login-btn:disabled {
            background-color: #6c757d;
            cursor: not-allowed;
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
          <SignupForm
            onSignupSuccess={handleSignupSuccess}
            onBackToLogin={() => setIsSignup(false)}
          />
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
              onKeyUp={(e) => e.key === "Enter" && !isLoading && handleLogin()}
              disabled={isLoading}
            />

            <div className="input-group mb-3">
              <input
                type={showPassword ? "text" : "password"}
                className="form-control glass-input"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyUp={(e) => e.key === "Enter" && !isLoading && handleLogin()}
                disabled={isLoading}
              />
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                disabled={isLoading}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>

            <button 
              className="btn login-btn w-100 mb-3" 
              onClick={handleLogin}
              disabled={isLoading}
            >
              {isLoading ? "Connecting..." : "Connect"}
            </button>

            <div className="text-center">
              <button
                className="btn btn-link link-button"
                onClick={() => setIsSignup(true)}
                disabled={isLoading}
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
