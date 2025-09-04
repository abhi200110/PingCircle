import { useState, useEffect, useRef } from "react";
import api from "../../config/axios";
import PropTypes from "prop-types";

const SearchBar = ({ onUserSelect }) => {
  const [username, setUsername] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [error, setError] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  const searchTimeoutRef = useRef(null);

  const handleInputChange = (e) => {
    const value = e.target.value;
    setUsername(value);
    
    // Clear previous timeout
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }
    
    // Clear results if input is empty
    if (!value.trim()) {
      setSearchResults([]);
      setError(null);
      return;
    }
    
    // Set up debounced search (500ms delay)
    searchTimeoutRef.current = setTimeout(() => {
      handleSearch(value);
    }, 500);
  };

  const handleSearch = async (searchTerm = username) => {
    if (!searchTerm.trim()) {
      setSearchResults([]);
      setError(null);
      return;
    }

    try {
      setIsSearching(true);
      setError(null);
      const response = await api.get(
        `/users/search?searchTerm=${encodeURIComponent(searchTerm.trim())}`
      );
      setSearchResults(response.data || []);
    } catch (error) {
      if (error.response && error.response.status === 404) {
        setError("No users found.");
      } else {
        setError("An error occurred while searching.");
      }
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const handleUserSelect = (user) => {
    setUsername("");
    setSearchResults([]);
    onUserSelect(user);
  };

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  return (
    <div className="position-relative w-100">
      <div className="input-group">
        <input
          type="text"
          className="form-control border-primary"
          placeholder="Search users by username or name..."
          value={username}
          onChange={handleInputChange}
        />
        <button
          className="btn btn-outline-primary"
          onClick={() => handleSearch()}
          disabled={isSearching}
        >
          {isSearching ? (
            <div className="spinner-border spinner-border-sm" role="status">
              <span className="visually-hidden">Searching...</span>
            </div>
          ) : (
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              style={{ width: "16px", height: "16px" }}
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
          )}
        </button>
      </div>
      
      {error && <p className="text-danger mt-2 small">{error}</p>}
      
      {isSearching && (
        <div className="position-absolute top-100 start-0 mt-1 w-100 bg-white border border-primary rounded shadow-lg p-2" style={{ zIndex: 10 }}>
          <div className="text-center text-muted">
            <div className="spinner-border spinner-border-sm me-2"></div>
            Searching...
          </div>
        </div>
      )}
      
      {searchResults.length > 0 && !isSearching && (
        <div className="position-absolute top-100 start-0 mt-1 w-100 bg-white border border-primary rounded shadow-lg" style={{ zIndex: 10 }}>
          {searchResults.map((user) => (
            <div
              key={user.id}
              className="cursor-pointer p-3 hover-bg-light border-bottom border-light d-flex align-items-center gap-2"
              onClick={() => handleUserSelect(user)}
            >
              <div className="bg-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: "32px", height: "32px" }}>
                <span className="text-white small fw-medium">
                  {user.username.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <div className="fw-medium">{user.username}</div>
                {user.name && user.name !== user.username && (
                  <div className="small text-muted">{user.name}</div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
      
      {searchResults.length === 0 && username.trim() && !isSearching && !error && (
        <div className="position-absolute top-100 start-0 mt-1 w-100 bg-white border border-primary rounded shadow-lg p-3" style={{ zIndex: 10 }}>
          <div className="text-center text-muted">
            No users found matching "{username}"
          </div>
        </div>
      )}
    </div>
  );
};

export default SearchBar;

SearchBar.propTypes = {
  onUserSelect: PropTypes.func.isRequired,
};
