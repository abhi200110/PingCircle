import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import api from '../config/axios';

const ScheduledMessagesList = ({ currentUser, onCancelMessage, onClose }) => {
  const [scheduledMessages, setScheduledMessages] = useState([]);
  const [pendingMessages, setPendingMessages] = useState([]);
  const [reminders, setReminders] = useState([]);
  const [activeTab, setActiveTab] = useState('scheduled');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchScheduledMessages = async () => {
    try {
      setIsLoading(true);
      setError('');
      

      // First, test if backend is accessible
      try {

      } catch (testError) {
        setError('Backend server is not accessible. Please ensure the server is running.');
        return;
      }

      const [scheduledRes, pendingRes, remindersRes] = await Promise.all([
        api.get(`/chat/scheduled-messages?senderName=${currentUser}`),
        api.get(`/chat/pending-messages?senderName=${currentUser}`),
        api.get(`/chat/reminders?senderName=${currentUser}`)
      ]);


      setScheduledMessages(scheduledRes.data);
      setPendingMessages(pendingRes.data);
      setReminders(remindersRes.data);
    } catch (error) {
      const errorMessage = error.response?.data || error.message || 'Failed to fetch scheduled messages';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (currentUser) {
      fetchScheduledMessages();
    }
  }, [currentUser]);

  const handleCancelMessage = async (messageId) => {
    try {
      await api.delete(`/chat/cancel-message/${messageId}?senderName=${currentUser}`);
      
      // Refresh the lists
      fetchScheduledMessages();
      if (onCancelMessage) {
        onCancelMessage(messageId);
      }
    } catch (error) {
      const errorMessage = error.response?.data || error.message || 'Failed to cancel message';
      setError(errorMessage);
    }
  };

  const formatScheduledTime = (timestamp) => {
    try {
      const date = new Date(timestamp);
      return format(date, 'MMM dd, yyyy HH:mm');
    } catch (error) {
      return 'Invalid date';
    }
  };

  const getMessageTypeInfo = (messageType) => {
    switch (messageType) {
      case 'SCHEDULED':
        return { icon: '📅', label: 'Scheduled', color: 'primary' };
      case 'REMINDER':
        return { icon: '⏰', label: 'Reminder', color: 'info' };
      case 'BIRTHDAY':
        return { icon: '🎂', label: 'Birthday', color: 'warning' };
      case 'ANNIVERSARY':
        return { icon: '💕', label: 'Anniversary', color: 'danger' };
      default:
        return { icon: '📝', label: 'Message', color: 'secondary' };
    }
  };

  const getStatusBadge = (isSent) => {
    if (isSent) {
      return (
        <span className="badge bg-success d-inline-flex align-items-center">
          <i className="bi bi-check-circle me-1"></i>
          Sent
        </span>
      );
    } else {
      return (
        <span className="badge bg-warning d-inline-flex align-items-center">
          <i className="bi bi-clock me-1"></i>
          Pending
        </span>
      );
    }
  };

  const renderMessageCard = (message, showCancelButton = true) => {
    const typeInfo = getMessageTypeInfo(message.messageType);
    const isOverdue = !message.isSent && new Date(message.scheduledTime) < new Date();

    return (
      <div key={message.id} className={`card mb-3 ${isOverdue ? 'border-danger bg-light' : ''}`}>
        <div className="card-body">
          {/* Header */}
          <div className="d-flex justify-content-between align-items-start mb-3">
            <div className="d-flex align-items-center">
              <div className={`rounded-circle bg-${typeInfo.color} text-white d-flex justify-content-center align-items-center me-3`} style={{ width: '48px', height: '48px' }}>
                <span className="fs-4">{typeInfo.icon}</span>
              </div>
              <div>
                <h6 className="card-title mb-1">{typeInfo.label} Message</h6>
                <small className="text-muted">To: {message.receiverName}</small>
              </div>
            </div>
            <div className="d-flex align-items-center gap-2">
              {getStatusBadge(message.isSent)}
              {showCancelButton && !message.isSent && (
                <button
                  onClick={() => handleCancelMessage(message.id)}
                  className="btn btn-outline-danger btn-sm"
                  title="Cancel message"
                >
                  <i className="bi bi-x-lg"></i>
                </button>
              )}
            </div>
          </div>

          {/* Message Content */}
          <div className="mb-3">
            <div className="bg-light p-3 rounded">
              <p className="mb-0">{message.message}</p>
            </div>
          </div>

          {/* Details */}
          <div className="small text-muted">
            <div className="d-flex align-items-center mb-2">
              <i className="bi bi-calendar me-2"></i>
              <span>Scheduled for: <strong>{formatScheduledTime(message.scheduledTime)}</strong></span>
            </div>

            {message.reminderTitle && (
              <div className="d-flex align-items-center mb-2">
                <i className="bi bi-tag me-2"></i>
                <span>Title: <strong>{message.reminderTitle}</strong></span>
              </div>
            )}

            {message.reminderDescription && (
              <div className="d-flex align-items-start mb-2">
                <i className="bi bi-file-text me-2 mt-1"></i>
                <span>Description: <strong>{message.reminderDescription}</strong></span>
              </div>
            )}

            {isOverdue && (
              <div className="alert alert-danger small mb-0">
                <i className="bi bi-exclamation-triangle me-2"></i>
                Overdue - This message should have been sent already
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  const renderMessageList = (messages, showCancelButton = true) => {
    if (messages.length === 0) {
      return (
        <div className="text-center py-5">
          <div className="mb-3">
            <i className="bi bi-inbox fs-1 text-muted"></i>
          </div>
          <h5 className="text-muted">No messages found</h5>
          <p className="text-muted">You haven't scheduled any messages yet.</p>
        </div>
      );
    }

    return (
      <div>
        {messages.map((message) => renderMessageCard(message, showCancelButton))}
      </div>
    );
  };

  const tabs = [
    { id: 'scheduled', label: 'All Messages', count: scheduledMessages.length, icon: '📋' },
    { id: 'pending', label: 'Pending', count: pendingMessages.length, icon: '⏳' },
    { id: 'reminders', label: 'Reminders', count: reminders.length, icon: '⏰' }
  ];

  if (isLoading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-3 text-muted">Loading scheduled messages...</p>
      </div>
    );
  }

  return (
    <div className="modal-content">
      {/* Modal Header */}
      <div className="modal-header bg-primary text-white">
        <div className="d-flex align-items-center">
          <span className="fs-4 me-2">📅</span>
          <div>
            <h5 className="modal-title mb-0">Scheduled Messages</h5>
            <small className="text-light">Manage your scheduled messages and reminders</small>
          </div>
        </div>
        {onClose && (
          <button
            type="button"
            className="btn-close btn-close-white"
            onClick={onClose}
          ></button>
        )}
      </div>

      {/* Modal Body */}
      <div className="modal-body">
        {/* Tabs */}
        <ul className="nav nav-tabs mb-3">
          {tabs.map((tab) => (
            <li key={tab.id} className="nav-item">
              <button
                className={`nav-link ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                <span className="me-2">{tab.icon}</span>
                {tab.label}
                <span className={`badge bg-secondary ms-2 ${activeTab === tab.id ? 'bg-light text-dark' : ''}`}>
                  {tab.count}
                </span>
              </button>
            </li>
          ))}
        </ul>

        {/* Error Message */}
        {error && (
          <div className="alert alert-danger small mb-3">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
          </div>
        )}

        {/* Manual Trigger Button for Testing */}
        <div className="mb-3">
          <button
            onClick={async () => {
              try {
        
                alert('Scheduled messages processing triggered!');
                fetchScheduledMessages(); // Refresh the list
              } catch (error) {
                alert('Error triggering scheduled messages: ' + error.message);
              }
            }}
            className="btn btn-outline-primary btn-sm"
          >
            <i className="bi bi-play-circle me-1"></i>
            Trigger Scheduled Messages (Test)
          </button>
        </div>

        {/* Content */}
        {activeTab === 'scheduled' && renderMessageList(scheduledMessages, false)}
        {activeTab === 'pending' && renderMessageList(pendingMessages, true)}
        {activeTab === 'reminders' && renderMessageList(reminders, true)}
      </div>
    </div>
  );
};

export default ScheduledMessagesList;
