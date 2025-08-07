

import React, { useState } from 'react';
import { format } from 'date-fns';

const ScheduledMessageModal = ({ isOpen, onClose, onSchedule, currentUser, selectedUser }) => {
  const [message, setMessage] = useState('');
  const [scheduledDate, setScheduledDate] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');
  const [messageType, setMessageType] = useState('SCHEDULED');
  const [reminderTitle, setReminderTitle] = useState('');
  const [reminderDescription, setReminderDescription] = useState('');
  const [contactName, setContactName] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const now = new Date();
  const currentDate = format(now, 'yyyy-MM-dd');
  const currentTime = format(now, 'HH:mm');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      let calculatedScheduledTime = null;

      if (messageType === 'SCHEDULED' || messageType === 'REMINDER') {
        if (!scheduledDate || !scheduledTime) {
          throw new Error('Please select both date and time for scheduled messages');
        }
        const dateTimeString = `${scheduledDate}T${scheduledTime}`;
        calculatedScheduledTime = new Date(dateTimeString).getTime();
      } else if (messageType === 'BIRTHDAY' || messageType === 'ANNIVERSARY') {
        if (!eventDate) {
          throw new Error('Please enter the event date for birthday/anniversary reminders');
        }
        const eventDateTime = new Date(`${new Date().getFullYear()}-${eventDate}`);
        calculatedScheduledTime = eventDateTime.getTime();
      }

      if (!calculatedScheduledTime) {
        throw new Error('Failed to calculate scheduled time');
      }

      const requestData = {
        senderName: currentUser,
        receiverName: selectedUser,
        message,
        scheduledTime: calculatedScheduledTime,
        messageType,
        reminderTitle,
        reminderDescription,
        contactName,
        eventDate,
      };

      await onSchedule(requestData);
      handleClose();
    } catch (error) {
      setError(error.message || 'Failed to schedule message');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setMessage('');
    setScheduledDate('');
    setScheduledTime('');
    setMessageType('SCHEDULED');
    setReminderTitle('');
    setReminderDescription('');
    setContactName('');
    setEventDate('');
    setError('');
    onClose();
  };

  const getMessageTypeInfo = (type) => {
    switch (type) {
      case 'SCHEDULED':
        return { icon: '📅', label: 'Scheduled Message', description: 'Send a message at a specific date and time' };
      case 'REMINDER':
        return { icon: '⏰', label: 'Reminder', description: 'Set a reminder with title and description' };
      case 'BIRTHDAY':
        return { icon: '🎂', label: 'Birthday Reminder', description: 'Automatically send birthday wishes yearly' };
      case 'ANNIVERSARY':
        return { icon: '💕', label: 'Anniversary Reminder', description: 'Automatically send anniversary wishes yearly' };
      default:
        return { icon: '📝', label: 'Message', description: '' };
    }
  };

  const messageTypeInfo = getMessageTypeInfo(messageType);
  if (!isOpen) return null;

  return (
    <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          {/* Modal Header */}
          <div className="modal-header bg-primary text-white">
            <div className="d-flex align-items-center">
              <span className="fs-4 me-2">{messageTypeInfo.icon}</span>
              <div>
                <h5 className="modal-title mb-0">Schedule Message</h5>
                <small className="text-light">{messageTypeInfo.description}</small>
              </div>
            </div>
            <button
              type="button"
              className="btn-close btn-close-white"
              onClick={handleClose}
            ></button>
          </div>

          {/* Modal Body */}
          <div className="modal-body">
            <form onSubmit={handleSubmit}>
              {/* Message Type */}
              <div className="mb-3">
                <label className="form-label fw-semibold small">Message Type</label>
                <div className="row g-2">
                  {['SCHEDULED', 'REMINDER', 'BIRTHDAY', 'ANNIVERSARY'].map((type) => {
                    const info = getMessageTypeInfo(type);
                    return (
                      <div key={type} className="col-6 col-md-3">
                        <button
                          type="button"
                          className={`btn w-100 border ${
                            messageType === type
                              ? 'border-primary text-primary bg-light'
                              : 'border-secondary text-secondary bg-white'
                          }`}
                          onClick={() => setMessageType(type)}
                        >
                          <div className="d-flex flex-column align-items-center">
                            <span>{info.icon}</span>
                            <small>{info.label.split(' ')[0]}</small>
                          </div>
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Recipient Info */}
              <div className="d-flex align-items-center mb-3 bg-light p-2 rounded">
                <div className="rounded-circle bg-primary text-white d-flex justify-content-center align-items-center me-2" style={{ width: '32px', height: '32px' }}>
                  <span className="fw-bold small">{selectedUser?.charAt(0)?.toUpperCase()}</span>
                </div>
                <div className="small text-secondary">To: <strong>{selectedUser}</strong></div>
              </div>

              {/* Message */}
              <div className="mb-3">
                <label className="form-label fw-semibold small">Message</label>
                <div className="position-relative">
                  <textarea
                    className="form-control"
                    placeholder="Enter your message..."
                    rows="4"
                    maxLength="500"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    required
                  ></textarea>
                  <div className="position-absolute bottom-0 end-0 me-2 mb-1 text-muted small">
                    {message.length}/500
                  </div>
                </div>
              </div>

              {/* Date & Time for Scheduled/Reminder */}
              {(messageType === 'SCHEDULED' || messageType === 'REMINDER') && (
                <div className="row g-3 mb-3">
                  <div className="col-md-6">
                    <label className="form-label small fw-semibold">📆 Date</label>
                    <input
                      type="date"
                      className="form-control"
                      min={currentDate}
                      value={scheduledDate}
                      onChange={(e) => setScheduledDate(e.target.value)}
                      required
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label small fw-semibold">⏱ Time</label>
                    <input
                      type="time"
                      className="form-control"
                      value={scheduledTime}
                      onChange={(e) => setScheduledTime(e.target.value)}
                      required
                    />
                  </div>
                </div>
              )}

              {/* Reminder Title & Description */}
              {messageType === 'REMINDER' && (
                <div className="mb-3">
                  <div className="mb-2">
                    <label className="form-label small fw-semibold">Reminder Title</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Enter reminder title..."
                      value={reminderTitle}
                      onChange={(e) => setReminderTitle(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="form-label small fw-semibold">Reminder Description</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Enter reminder description..."
                      value={reminderDescription}
                      onChange={(e) => setReminderDescription(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Birthday/Anniversary Fields */}
              {(messageType === 'BIRTHDAY' || messageType === 'ANNIVERSARY') && (
                <div className="mb-3">
                  <div className="mb-2">
                    <label className="form-label small fw-semibold">Contact Name</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Enter contact name..."
                      value={contactName}
                      onChange={(e) => setContactName(e.target.value)}
                      required
                    />
                  </div>
                  <div>
                    <label className="form-label small fw-semibold">Event Date (MM-DD)</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="MM-DD (e.g., 12-25)"
                      pattern="^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$"
                      value={eventDate}
                      onChange={(e) => setEventDate(e.target.value)}
                      required
                    />
                    <small className="text-muted">Format: MM-DD (e.g., 12-25 for December 25th)</small>
                  </div>
                </div>
              )}

              {/* Error Message */}
              {error && (
                <div className="alert alert-danger small mb-3">
                  <i className="bi bi-exclamation-triangle me-2"></i>
                  {error}
                </div>
              )}

              {/* Action Buttons */}
              <div className="d-flex justify-content-end gap-2 pt-2">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={handleClose}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary btn-sm d-flex align-items-center gap-1"
                  disabled={isLoading}
                >
                  {isLoading ? (
                    <>
                      <div className="spinner-border spinner-border-sm me-1"></div>
                      Scheduling...
                    </>
                  ) : (
                    <>
                      {messageTypeInfo.icon} <span>Schedule</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ScheduledMessageModal;
