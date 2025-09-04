import React, { useState, useEffect } from 'react';
import api from '../config/axios';

const PinnedUsers = ({ currentUser, onUserSelect, selectedUser, onlineUsers = new Set(), unreadMessages = new Set(), pinnedUsers = [], onUnpin }) => {
  const [loading, setLoading] = useState(false);
  

  // Remove the useEffect since we're now getting pinnedUsers as props

  const fetchPinnedUsers = async () => {
    if (!currentUser) return;
    
    try {
      setLoading(true);
      const response = await api.get(`/users/pinnedUsers?username=${currentUser}`);
      setPinnedUsers(response.data || []);
    } catch (error) {
      setPinnedUsers([]); // Set empty array on error
    } finally {
      setLoading(false);
    }
  };

  const handleUnpin = async (pinnedUsername) => {
    try {
      setLoading(true);
      const response = await api.post('/users/pinUser', {
        username: currentUser,
        pinnedUsername: pinnedUsername,
        pin: false
      });
      
      if (response.status === 200) {
        // Call the parent's onUnpin function to update the state
        if (onUnpin) {
          onUnpin(pinnedUsername);
        }
        logger.debug('User unpinned successfully', { pinnedUsername });
      }
    } catch (error) {
      logger.error('Error unpinning user', { 
        error: error.message, 
        details: error.response?.data 
      });
      // Show error message to user
      alert(`Failed to unpin user: ${error.response?.data || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-4 bg-white">
        <div className="placeholder-glow">
          <div className="placeholder col-9 mb-2"></div>
          <div className="placeholder col-6"></div>
        </div>
      </div>
    );
  }

  if (pinnedUsers.length === 0) {
    return (
      <div className="p-4 text-center text-muted bg-white">
        <p className="small">No pinned users</p>
        <p className="small mt-1">Pin users to see them here</p>
      </div>
    );
  }

  return (
    <div className="p-4 bg-white">
      <h3 className="small fw-semibold text-primary mb-3 d-flex align-items-center">
        📌 Pinned Users
      </h3>
      <div className="d-flex flex-column gap-2">
        {pinnedUsers.map((username) => {
                     const isOnline = onlineUsers.has(username);
           const hasUnread = unreadMessages.has(username);
           logger.debug(`Pinned user status`, { 
             username, 
             isOnline, 
             hasUnread 
           });
          return (
          <div
            key={username}
            className={`d-flex align-items-center justify-content-between p-2 rounded cursor-pointer transition-colors ${
              selectedUser?.username === username
                ? 'bg-primary bg-opacity-10 border border-primary'
                : 'bg-light hover-bg-primary hover-text-white'
            }`}
            onClick={() => onUserSelect({ username, name: username })}
          >
                         <div className="d-flex align-items-center gap-2">
               <div className="bg-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: "32px", height: "32px" }}>
                 <span className="text-white small fw-medium">
                   {username.charAt(0).toUpperCase()}
                 </span>
               </div>
               <div className="d-flex align-items-center gap-2">
                 <span className="small fw-medium text-dark">
                   {username}
                 </span>
                                  <div className="d-flex align-items-center gap-1">
                   {onlineUsers.has(username) && (
                     <div className="bg-success rounded-circle" style={{ width: "10px", height: "10px", border: "1px solid #fff" }} title="Online"></div>
                   )}
                   {unreadMessages.has(username) && (
                     <div className="bg-primary rounded-circle" style={{ width: "10px", height: "10px", border: "1px solid #fff" }} title="New message"></div>
                   )}
                 </div>
               </div>
             </div>
             <button
               onClick={(e) => {
                 e.stopPropagation();
                 handleUnpin(username);
               }}
               disabled={loading}
               className="btn btn-link text-muted hover-text-danger transition-colors p-0"
               title="Unpin user"
             >
               {loading ? (
                 <div className="spinner-border spinner-border-sm text-danger" role="status">
                   <span className="visually-hidden">Unpinning...</span>
                 </div>
               ) : (
                 <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" style={{ width: "16px", height: "16px" }}>
                   <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                 </svg>
               )}
             </button>
           </div>
         );
       })}
       </div>
     </div>
   );
 };

export default PinnedUsers; 