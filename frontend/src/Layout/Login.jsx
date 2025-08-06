// src/pages/Login.jsx

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./button.css"; // Keep if you already have custom button styles
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
      {/* Inline styles for styling the page directly in JSX */}
      <style>
        {`
          .login-bg {
            background: linear-gradient(
                rgba(0, 0, 0, 0.5),
                rgba(0, 0, 0, 0.5)
              ),
              url('https://images.unsplash.com/photo-1542751110-97427bbecf20?auto=format&fit=crop&w=1950&q=80') center center/cover no-repeat;
          }

          .login-card {
            backdrop-filter: blur(15px);
            background: rgba(255, 255, 255, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.25);
            max-width: 400px;
            width: 90%;
            color: white;
            padding: 2rem;
            border-radius: 1.5rem;
          }

          .glass-input {
            background: rgba(255, 255, 255, 0.2);
            border: none;
            border-radius: 12px;
            color: white;
            text-align: center;
          }

          .glass-input::placeholder {
            color: rgba(255, 255, 255, 0.6);
          }

          .glass-input:focus {
            background: rgba(255, 255, 255, 0.3);
            outline: none;
            box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.5);
            color: white;
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
        `}
      </style>

      <div className="login-bg d-flex align-items-center justify-content-center vh-100">
        {isSignup ? (
          <SignupForm onSignupSuccess={handleSignupSuccess} />
        ) : (
          <div className="login-card shadow">
            <h2 className="mb-4 text-center text-white fw-bold">PingCircle</h2>
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
                className="btn btn-link text-white-50"
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
