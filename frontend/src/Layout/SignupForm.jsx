import { useState } from "react";
import axios from "axios";
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

    try {
      const payload = { username, name, email, password };
      const response = await axios.post("http://localhost:8080/api/users/signup", payload);
      onSignupSuccess(username);
      setSuccess(true);
      setUsername("");
      setName("");
      setEmail("");
      setPassword("");
    } catch (error) {
      if (error.response) {
        setError(error.response.data.message || "Signup failed.");
      } else {
        setError("Network error. Please try again.");
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
              ✅ Signup successful! You can now log in.
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
