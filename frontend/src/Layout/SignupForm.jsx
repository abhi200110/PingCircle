import React, { useState } from "react";
import api from "../config/axios";
import { PropTypes } from "prop-types";

const SignupForm = ({ onSignupSuccess, onBackToLogin }) => {
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setSuccess(false);

    // Client-side validation
    if (!username || username.length < 3 || username.length > 20) {
      setError("Username must be between 3 and 20 characters");
      setIsSubmitting(false);
      return;
    }

    if (!name || name.trim().length === 0) {
      setError("Name is required");
      setIsSubmitting(false);
      return;
    }

    if (!email || !email.includes('@')) {
      setError("Please enter a valid email address");
      setIsSubmitting(false);
      return;
    }

    if (!password || password.length < 6) {
      setError("Password must be at least 6 characters");
      setIsSubmitting(false);
      return;
    }

    // Check for at least one letter and one number
    const hasLetter = /[A-Za-z]/.test(password);
    const hasNumber = /\d/.test(password);
    if (!hasLetter || !hasNumber) {
      setError("Password must contain at least one letter and one number");
      setIsSubmitting(false);
      return;
    }

    try {
      const payload = { username, name, email, password };
      const response = await api.post("/users/signup", payload);
      
      // Handle the response format that includes JWT token
      if (response.data && response.data.token) {
        // Store the JWT token in localStorage
        localStorage.setItem("jwt-token", response.data.token);
        onSignupSuccess(username);
      } else {
        // Fallback for old response format
        onSignupSuccess(username);
      }
      
      setSuccess(true);
      setUsername("");
      setName("");
      setEmail("");
      setPassword("");
    } catch (error) {
      console.error("Signup error:", error);
      
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 400) {
          setError(data || "Invalid signup data. Please check your information.");
        } else if (status === 409) {
          setError("Username or email already exists. Please choose different credentials.");
        } else if (status === 500) {
          setError("Server error. Please try again later.");
        } else {
          setError(`Signup failed (${status}): ${data || "Unknown error"}`);
        }
      } else if (error.request) {
        setError("Cannot connect to server. Please check if the backend is running.");
      } else {
        setError("An error occurred during signup. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <style>
        {`
          .signup-card {
            backdrop-filter: blur(20px);
            background: rgba(255, 255, 255, 0.4);
            border: 1px solid rgba(0, 123, 255, 0.3);
            max-width: 420px;
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

          .signup-btn {
            background-color: #0d6efd;
            color: white;
            font-weight: bold;
            border-radius: 10px;
            transition: 0.3s ease;
          }

          .signup-btn:hover {
            background-color: #0b5ed7;
          }

          .link-button {
            color: #0d6efd;
          }

          .pingcircle-title {
            font-size: 2rem;
            font-weight: 800;
            margin-bottom: 0.5rem;
            text-align: center;
            color: #0d6efd;
            text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.3);
          }

          .create-account-subtitle {
            font-size: 1.25rem;
            font-weight: 600;
            color: #004080;
            text-align: center;
            margin-bottom: 1.5rem;
          }
        `}
      </style>

      <div className="container my-5 d-flex justify-content-center">
        <form className="signup-card" onSubmit={handleSubmit}>
          <div className="pingcircle-title">PingCircle</div>
          <div className="create-account-subtitle">Create Account</div>

          {error && <div className="alert alert-danger py-1 px-2">{error}</div>}
          {success && (
            <div className="alert alert-success py-1 px-2">
              Signup successful! Redirecting to chat...
            </div>
          )}

          <input
            type="text"
            className="form-control glass-input mb-3"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <input
            type="text"
            className="form-control glass-input mb-3"
            placeholder="Full Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <input
            type="email"
            className="form-control glass-input mb-3"
            placeholder="Email Address"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <div className="input-group mb-3">
            <input
              type={showPassword ? "text" : "password"}
              className="form-control glass-input"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={() => setShowPassword(!showPassword)}
              tabIndex={-1}
            >
              {showPassword ? "Hide" : "Show"}
            </button>
          </div>
          <small className="text-muted mb-3 d-block">
            Password must be at least 6 characters and contain at least one letter and one number.
          </small>

          <button type="submit" className="btn signup-btn w-100 mb-3" disabled={isSubmitting}>
            {isSubmitting ? "Creating..." : "Sign Up"}
          </button>

          <div className="text-center">
            <button
              type="button"
              className="btn btn-link link-button"
              onClick={onBackToLogin}
            >
              ← Back to Login
            </button>
          </div>
        </form>
      </div>
    </>
  );
};

SignupForm.propTypes = {
  onSignupSuccess: PropTypes.func.isRequired,
  onBackToLogin: PropTypes.func.isRequired,
};

export default SignupForm;
