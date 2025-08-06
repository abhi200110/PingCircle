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
    <div className="d-flex align-items-center justify-content-center vh-100 bg-light position-relative">
      {/* Watermark */}
      <div className="position-absolute top-0 start-50 translate-middle-x text-muted watermark">
        <h1 className="fw-bold text-primary-opacity-75">PingCircle</h1>
      </div>

      {/* Signup Form */}
      <form
        onSubmit={handleSubmit}
        className="bg-white rounded shadow p-4 animated-form"
        style={{ width: "100%", maxWidth: "400px" }}
      >
        <h2 className="text-center text-primary mb-4">Create Account</h2>

        {error && <div className="alert alert-danger">{error}</div>}
        {success && (
          <div className="alert alert-success">
            ✅ Signup successful! You can now log in.
          </div>
        )}

        <input
          type="text"
          placeholder="Username"
          className="form-control mb-3 input-focus border-primary"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />

        <input
          type="text"
          placeholder="Full Name"
          className="form-control mb-3 input-focus border-primary"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <input
          type="email"
          placeholder="Email Address"
          className="form-control mb-3 input-focus border-primary"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <div className="input-group mb-4">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            className="form-control input-focus border-primary"
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

        <button
          type="submit"
          className="btn btn-primary w-100 fw-semibold mb-2"
          disabled={isSubmitting}
        >
          {isSubmitting ? "Creating..." : "Sign Up"}
        </button>

        <button
          type="button"
          className="btn btn-outline-secondary w-100"
          onClick={onBackToLogin}
        >
          ← Back to Login
        </button>
      </form>

      <style>{`
        .animated-form {
          animation: fadeInUp 0.6s ease-in-out;
        }

        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(20px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .input-focus:focus {
          box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
          transition: all 0.2s ease;
        }

        button:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .watermark {
          padding-top: 1rem;
          font-size: 0.5rem; /* Smaller watermark */
          opacity: 0.9;
          user-select: none;
          z-index: 0;
        }

        .text-primary-opacity-75 {
          color: rgba(13, 110, 253, 0.75);
        }
      `}</style>
    </div>
  );
};

SignupForm.propTypes = {
  onSignupSuccess: PropTypes.func.isRequired,
  onBackToLogin: PropTypes.func.isRequired,
};

export default SignupForm;
