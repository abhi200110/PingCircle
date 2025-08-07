import React, { useState, useEffect } from 'react';
import api from '../config/axios';

const UserChatItem = ({ user, currentUser, isSelected, onUserSelect, onPinChange, onDeleteConversation, isOnline = false }) => {
  const [isPinned, setIsPinned] = useState(false);
  const [loading, setLoading] = useState(false);
  const [deleting, setDeleting] = useState(false);

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

  const handleDelete = async (e) => {
    e.stopPropagation();
    if (!currentUser || !user.username) return;

    if (window.confirm(`Are you sure you want to delete your messages in the conversation with ${user.username}? This will only delete messages you sent, not messages from ${user.username}. This action cannot be undone.`)) {
      try {
        setDeleting(true);
        if (onDeleteConversation) {
          await onDeleteConversation(user.username);
        }
      } catch (error) {
        console.error('Error deleting conversation:', error);
      } finally {
        setDeleting(false);
      }
    }
  };

  return (
    <div
      className={`d-flex align-items-center justify-content-between p-3 rounded cursor-pointer transition-colors ${
        isSelected
          ? 'bg-primary bg-opacity-10 border border-primary'
          : 'bg-white hover-bg-light border border-light'
      }`}
      onClick={() => onUserSelect(user)}
    >
      <div className="d-flex align-items-center gap-3">
        <div className="bg-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: "40px", height: "40px" }}>
          <span className="text-white fw-medium">
            {user.name ? user.name.charAt(0).toUpperCase() : user.username.charAt(0).toUpperCase()}
          </span>
        </div>
        <div>
          <div className="fw-medium text-dark d-flex align-items-center gap-2">
            {user.name || user.username}
            {isOnline && (
              <div className="bg-success rounded-circle" style={{ width: "8px", height: "8px" }}></div>
            )}
          </div>
          <div className="small text-muted">
            @{user.username}
          </div>
        </div>
      </div>
      
      <div className="d-flex align-items-center gap-2">
        <button
          onClick={handlePinToggle}
          disabled={loading}
          className={`btn btn-link p-1 rounded transition-colors ${
            isPinned 
              ? 'text-warning hover-text-warning' 
              : 'text-muted hover-text-warning'
          } ${loading ? 'opacity-50' : ''}`}
          title={isPinned ? 'Unpin user' : 'Pin user'}
        >
          {loading ? (
            <div className="spinner-border spinner-border-sm text-warning" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          ) : (
            <svg className="w-4 h-4" fill={isPinned ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24" style={{ width: "16px", height: "16px" }}>
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
            </svg>
          )}
        </button>
        
        <button
          onClick={handleDelete}
          disabled={deleting}
          className="btn btn-link p-1 rounded transition-colors text-danger hover-text-danger"
          title="Delete conversation"
        >
          {deleting ? (
            <div className="spinner-border spinner-border-sm text-danger" role="status">
              <span className="visually-hidden">Deleting...</span>
            </div>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
              <path fillRule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
            </svg>
          )}
        </button>
      </div>
    </div>
  );
};

export default UserChatItem; 