import React, { useState, useEffect } from 'react';
import api from '../config/axios';

const UserChatItem = ({ user, currentUser, isSelected, onUserSelect, onPinChange }) => {
  const [isPinned, setIsPinned] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkPinStatus();
  }, [user.username, currentUser]);

  const checkPinStatus = async () => {
    if (!currentUser || !user.username) return;
    
    try {
      console.log('Checking pin status for:', currentUser, user.username);
      const response = await api.get(`/users/is-pinned?username=${currentUser}&pinnedUsername=${user.username}`);
      console.log('Pin status response:', response.data);
      setIsPinned(response.data);
    } catch (error) {
      console.error('Error checking pin status:', error);
      console.error('Error details:', error.response?.data);
      setIsPinned(false); // Default to false on error
    }
  };

  const handlePinToggle = async (e) => {
    e.stopPropagation();
    if (!currentUser || !user.username) return;

    try {
      setLoading(true);
      console.log('Attempting to pin/unpin user:', {
        username: currentUser,
        pinnedUsername: user.username,
        pin: !isPinned
      });
      
      const response = await api.post('/users/pin-user', {
        username: currentUser,
        pinnedUsername: user.username,
        pin: !isPinned
      });
      
      console.log('Pin response:', response);
      setIsPinned(!isPinned);
      if (onPinChange) {
        onPinChange(user.username, !isPinned);
      }
    } catch (error) {
      console.error('Error toggling pin:', error);
      console.error('Error details:', error.response?.data);
      alert(`Failed to pin/unpin user: ${error.response?.data || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors ${
        isSelected
          ? 'bg-blue-100 border border-blue-300'
          : 'bg-gray-50 hover:bg-gray-100'
      }`}
      onClick={() => onUserSelect(user)}
    >
      <div className="flex items-center space-x-3">
        <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
          <span className="text-white font-medium">
            {user.name ? user.name.charAt(0).toUpperCase() : user.username.charAt(0).toUpperCase()}
          </span>
        </div>
        <div>
          <div className="font-medium text-gray-800">
            {user.name || user.username}
          </div>
          <div className="text-sm text-gray-500">
            @{user.username}
          </div>
        </div>
      </div>
      
      <button
        onClick={handlePinToggle}
        disabled={loading}
        className={`p-1 rounded transition-colors ${
          isPinned 
            ? 'text-yellow-500 hover:text-yellow-600' 
            : 'text-gray-400 hover:text-yellow-500'
        } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
        title={isPinned ? 'Unpin user' : 'Pin user'}
      >
        {loading ? (
          <div className="w-4 h-4 border-2 border-gray-300 border-t-yellow-500 rounded-full animate-spin"></div>
        ) : (
          <svg className="w-4 h-4" fill={isPinned ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
          </svg>
        )}
      </button>
    </div>
  );
};

export default UserChatItem; 