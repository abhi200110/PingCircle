import React, { useState } from 'react';
import api from '../config/axios';

const UserDashboard = ({ username, onLogout, isOnline = true }) => {
  const [isDeleting, setIsDeleting] = useState(false);
  // Generate initials from username for the logo
  const getInitials = (name) => {
    return name
      .split(' ')
      .map(word => word.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  // Generate a consistent color based on username - using blue theme
  const getAvatarColor = (name) => {
    const colors = [
      'bg-primary', // Blue
      'bg-info',    // Light blue
      'bg-secondary', // Grey
      'bg-primary', // Blue
      'bg-info',    // Light blue
      'bg-secondary' // Grey
    ];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  };

  // Handle account deletion
  const handleDeleteAccount = async () => {
    const confirmMessage = `Are you sure you want to delete your account "${username}"?\n\nThis action will:\n• Permanently delete your account\n• Remove all your messages\n• Remove you from other users' pinned lists\n• Cannot be undone\n\nType "DELETE" to confirm:`;
    
    const userInput = prompt(confirmMessage);
    
    if (userInput === "DELETE") {
      try {
        setIsDeleting(true);
        
        const response = await api.delete(`/users/delete-account?username=${username}`);
        
        if (response.status === 200) {
          alert("Account deleted successfully. You will be logged out.");
          // Clear local storage and logout
          localStorage.removeItem("chat-username");
          localStorage.removeItem("chat-token");
          onLogout();
        }
      } catch (error) {
        console.error('Error deleting account:', error);
        alert(`Failed to delete account: ${error.response?.data || error.message}`);
      } finally {
        setIsDeleting(false);
      }
    } else if (userInput !== null) {
      alert("Account deletion cancelled.");
    }
  };

  return (
    <div className="bg-primary text-white border-bottom border-primary p-3 w-100">
      <div className="d-flex align-items-center justify-content-between w-100">
        {/* User Info Section */}
        <div className="d-flex align-items-center gap-3">
          {/* User Avatar/Logo */}
          <div className="bg-white rounded-circle d-flex align-items-center justify-content-center text-primary fw-bold" 
               style={{ width: "50px", height: "50px", fontSize: "18px" }}>
            {getInitials(username)}
          </div>
          
          {/* User Details */}
          <div className="d-flex flex-column">
            <div className="fw-semibold text-white">{username}</div>
            <div className="d-flex align-items-center gap-2">
              <div className={`rounded-circle ${isOnline ? 'bg-success' : 'bg-light'}`} 
                   style={{ width: "8px", height: "8px" }}></div>
              <span className="small text-white-50">
                {isOnline ? 'Online' : 'Offline'}
              </span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="d-flex align-items-center gap-2">
          {/* Delete Account Button */}
          <button
            onClick={handleDeleteAccount}
            disabled={isDeleting}
            className="btn btn-outline-danger btn-sm"
            title="Delete Account"
          >
            {isDeleting ? (
              <>
                <div className="spinner-border spinner-border-sm me-1" role="status">
                  <span className="visually-hidden">Deleting...</span>
                </div>
                <span className="d-none d-sm-inline">Deleting...</span>
              </>
            ) : (
              <>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" 
                     className="bi bi-trash" viewBox="0 0 16 16">
                  <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                  <path fillRule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
                </svg>
                <span className="ms-1 d-none d-sm-inline">Delete Account</span>
              </>
            )}
          </button>

          {/* Logout Button */}
          <button
            onClick={onLogout}
            className="btn btn-outline-light btn-sm"
            title="Logout"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" 
                 className="bi bi-box-arrow-right" viewBox="0 0 16 16">
              <path fillRule="evenodd" d="M10 12.5a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v2a.5.5 0 0 0 1 0v-2A1.5 1.5 0 0 0 9.5 2h-8A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-2a.5.5 0 0 0-1 0v2z"/>
              <path fillRule="evenodd" d="M15.854 8.354a.5.5 0 0 0 0-.708l-3-3a.5.5 0 0 0-.708.708L14.293 7.5H5.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3z"/>
            </svg>
            <span className="ms-1 d-none d-sm-inline">Logout</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserDashboard; 