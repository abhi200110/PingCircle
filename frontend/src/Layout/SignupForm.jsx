import { useState } from "react";
import axios from "axios";
import { PropTypes } from "prop-types";

const SignupForm = ({ onSignupSuccess }) => {
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const payload = {
        username,
        name,
        email,
        password,
      };
      console.log("Sending payload:", payload);

      const response = await axios.post(
        "http://localhost:8080/api/users/signup",
        payload
      );

      console.log("Signup successful:", response.data);
      onSignupSuccess(username);
      setUsername("");
      setName("");
      setEmail("");
      setPassword("");
      setError(null);
    } catch (error) {
      console.error("Error signing up:", error);
      if (error.response) {
        console.error("Response data:", error.response.data);
        setError(
          error.response.data.message || "Error signing up. Please try again."
        );
      } else {
        setError("Error signing up. Please try again.");
      }
    }
  };

  return (
    <div className="d-flex align-items-center justify-content-center vh-100 bg-light">
      <form
        onSubmit={handleSubmit}
        className="w-100 bg-white rounded shadow p-4 border border-primary"
        style={{ maxWidth: "400px" }}
      >
        <h2 className="h3 fw-semibold mb-4 text-center text-primary">Sign Up</h2>
        {error && <p className="text-danger mb-4">{error}</p>}
        <div className="mb-3">
          <label
            htmlFor="username"
            className="form-label fw-medium text-dark"
          >
            Username
          </label>
          <input
            type="text"
            id="username"
            className="form-control border-primary"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div className="mb-3">
          <label
            htmlFor="name"
            className="form-label fw-medium text-dark"
          >
            Name
          </label>
          <input
            type="text"
            id="name"
            className="form-control border-primary"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>
        <div className="mb-3">
          <label
            htmlFor="email"
            className="form-label fw-medium text-dark"
          >
            Email
          </label>
          <input
            type="email"
            id="email"
            className="form-control border-primary"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="mb-4">
          <label
            htmlFor="password"
            className="form-label fw-medium text-dark"
          >
            Password
          </label>
          <input
            type="password"
            id="password"
            className="form-control border-primary"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <div className="d-flex align-items-center justify-content-between">
          <button
            type="submit"
            className="btn btn-primary w-100"
          >
            Sign Up
          </button>
        </div>
      </form>
    </div>
  );
};

export default SignupForm;

SignupForm.propTypes = {
  onSignupSuccess: PropTypes.func.isRequired,
};
