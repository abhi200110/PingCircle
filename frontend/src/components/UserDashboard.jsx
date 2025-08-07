import React from 'react';

const UserDashboard = ({ username, onLogout, isOnline = true }) => {
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

        {/* Logout Button */}
        <div className="d-flex align-items-center gap-2">
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