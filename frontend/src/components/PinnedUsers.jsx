import React, { useState, useEffect } from 'react';
import api from '../config/axios';

const PinnedUsers = ({ currentUser, onUserSelect, selectedUser }) => {
  const [pinnedUsers, setPinnedUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchPinnedUsers();
  }, [currentUser]);

  const fetchPinnedUsers = async () => {
    if (!currentUser) return;
    
    try {
      setLoading(true);
      const response = await api.get(`/users/pinned-users?username=${currentUser}`);
      setPinnedUsers(response.data || []);
    } catch (error) {
      console.error('Error fetching pinned users:', error);
      setPinnedUsers([]); // Set empty array on error
    } finally {
      setLoading(false);
    }
  };

  const handleUnpin = async (pinnedUsername) => {
    try {
      await api.post('/users/pin-user', {
        username: currentUser,
        pinnedUsername: pinnedUsername,
        pin: false
      });
      // Remove from local state
      setPinnedUsers(prev => prev.filter(user => user !== pinnedUsername));
    } catch (error) {
      console.error('Error unpinning user:', error);
      // Show error message to user
      alert('Failed to unpin user. Please try again.');
    }
  };

  if (loading) {
    return (
      <div className="p-4">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  if (pinnedUsers.length === 0) {
    return (
      <div className="p-4 text-center text-gray-500">
        <p className="text-sm">No pinned users</p>
        <p className="text-xs mt-1">Pin users to see them here</p>
      </div>
    );
  }

  return (
    <div className="p-4">
      <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center">
        📌 Pinned Users
      </h3>
      <div className="space-y-2">
        {pinnedUsers.map((username) => (
          <div
            key={username}
            className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition-colors ${
              selectedUser?.username === username
                ? 'bg-blue-100 border border-blue-300'
                : 'bg-gray-50 hover:bg-gray-100'
            }`}
            onClick={() => onUserSelect({ username, name: username })}
          >
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                <span className="text-white text-sm font-medium">
                  {username.charAt(0).toUpperCase()}
                </span>
              </div>
              <span className="text-sm font-medium text-gray-800">
                {username}
              </span>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleUnpin(username);
              }}
              className="text-gray-400 hover:text-red-500 transition-colors"
              title="Unpin user"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PinnedUsers; 