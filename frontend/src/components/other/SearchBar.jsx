import { useState } from "react";
import api from "../../config/axios";
import PropTypes from "prop-types";

const SearchBar = ({ onUserSelect }) => {
  const [username, setUsername] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [error, setError] = useState(null);

  const handleInputChange = (e) => {
    setUsername(e.target.value);
  };

  const handleSearch = async () => {
    try {
      setError(null);
      const response = await api.get(
        `/users/search?searchTerm=${username}`
      );
      setSearchResults(response.data || []);
    } catch (error) {
      console.error("Error searching for user:", error);
      if (error.response && error.response.status === 404) {
        setError("User not found.");
      } else {
        setError("An error occurred while searching.");
      }
      setSearchResults([]);
    }
  };

  const handleUserSelect = (user) => {
    setUsername("");
    setSearchResults([]);
    onUserSelect(user);
  };

  return (
    <div className="position-relative w-100">
      <input
        type="text"
        className="form-control border-primary"
        placeholder="Search username..."
        value={username}
        onChange={handleInputChange}
      />
      <button
        className="position-absolute top-0 end-0 mt-2 me-2 btn btn-link p-0"
        onClick={handleSearch}
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="text-primary"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          style={{ width: "24px", height: "24px" }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 11a4 4 0 11-8 0 4 4 0 018 0z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M17.5 17.5l4.5 4.5"
          />
        </svg>
      </button>
      {error && <p className="text-danger mt-2">{error}</p>}
      {searchResults.length > 0 && (
        <div className="position-absolute top-100 start-0 mt-1 w-100 bg-white border border-primary rounded shadow-lg" style={{ zIndex: 10 }}>
          {searchResults.map((user) => (
            <div
              key={user.id}
              className="cursor-pointer p-2 hover-bg-light border-bottom border-light"
              onClick={() => handleUserSelect(user)}
            >
              {user.username}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SearchBar;

SearchBar.propTypes = {
  onUserSelect: PropTypes.func.isRequired,
};
