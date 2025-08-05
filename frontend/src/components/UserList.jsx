import React from 'react';

const UserList = ({ users, selectedUser, onUserSelect, onlineUsers = [] }) => {
  const isOnline = (username) => {
    return onlineUsers.includes(username);
  };

  return (
    <div className="w-64 bg-gray-50 border-r border-gray-200">
      <div className="p-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-800">Users</h2>
        <p className="text-sm text-gray-600">
          {onlineUsers.length} online
        </p>
      </div>
      
      <div className="overflow-y-auto h-full">
        {users.map((user) => (
          <div
            key={user.username}
            onClick={() => onUserSelect(user)}
            className={`p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-100 transition-colors ${
              selectedUser?.username === user.username ? 'bg-blue-50 border-blue-200' : ''
            }`}
          >
            <div className="flex items-center space-x-3">
              <div className="relative">
                <div className="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center">
                  <span className="text-gray-600 font-medium">
                    {user.name.charAt(0).toUpperCase()}
                  </span>
                </div>
                {isOnline(user.username) && (
                  <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
                )}
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {user.name}
                  </p>
                  {isOnline(user.username) && (
                    <span className="text-xs text-green-600">Online</span>
                  )}
                </div>
                <p className="text-xs text-gray-500 truncate">
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