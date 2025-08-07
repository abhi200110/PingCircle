import React from 'react';

const UserList = ({ users, selectedUser, onUserSelect, onlineUsers = [] }) => {
  const isOnline = (username) => {
    return onlineUsers.includes(username);
  };

  return (
    <div className="bg-light border-end border-secondary" style={{ width: "256px" }}>
      <div className="p-4 border-bottom border-secondary">
        <h2 className="h5 fw-semibold text-dark">Users</h2>
        <p className="small text-muted">
          {onlineUsers.length} online
        </p>
      </div>
      
      <div className="overflow-auto h-100">
        {users.map((user) => (
          <div
            key={user.username}
            onClick={() => onUserSelect(user)}
            className={`p-4 border-bottom border-light cursor-pointer hover-bg-light transition-colors ${
              selectedUser?.username === user.username ? 'bg-primary bg-opacity-10 border-primary' : ''
            }`}
          >
            <div className="d-flex align-items-center gap-3">
              <div className="position-relative">
                <div className="bg-secondary rounded-circle d-flex align-items-center justify-content-center" style={{ width: "40px", height: "40px" }}>
                  <span className="text-muted fw-medium">
                    {user.name.charAt(0).toUpperCase()}
                  </span>
                </div>
                {isOnline(user.username) && (
                  <div className="position-absolute bottom-0 end-0 bg-success rounded-circle border border-white" style={{ width: "16px", height: "16px", transform: "translate(25%, 25%)" }}></div>
                )}
              </div>
              
              <div className="flex-grow-1 min-w-0">
                <div className="d-flex align-items-center justify-content-between">
                  <p className="small fw-medium text-dark text-truncate">
                    {user.name}
                  </p>
                  {isOnline(user.username) && (
                    <span className="small text-success">Online</span>
                  )}
                </div>
                <p className="small text-muted text-truncate">
                  @{user.username}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UserList; 