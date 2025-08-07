import { useState, useCallback } from 'react';
import api from '../config/axios';

/**
 * Custom hook for handling API requests with loading and error states
 * Provides a centralized way to manage API calls with consistent error handling
 * and loading state management
 * 
 * @returns {Object} Object containing loading state, error state, and API functions
 */
const useApi = () => {
  // State for tracking API request loading status
  const [loading, setLoading] = useState(false);
  // State for storing any API request errors
  const [error, setError] = useState(null);

  /**
   * Generic request wrapper that handles loading states and error handling
   * @param {Function} requestFn - The API request function to execute
   * @returns {Promise} Promise that resolves with the API response or rejects with error
   */
  const makeRequest = useCallback(async (requestFn) => {
    setLoading(true);
    setError(null);
    
    try {
      const result = await requestFn();
      return result;
    } catch (err) {
      // Extract error message from response or use fallback
      const errorMessage = err.response?.data || err.message || 'An error occurred';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Authenticate user with username and password
   * @param {string} username - User's username
   * @param {string} password - User's password
   * @returns {Promise} Promise that resolves with login response
   */
  const login = useCallback(async (username, password) => {
    return makeRequest(() => 
      api.post('/users/login', { username, password })
    );
  }, [makeRequest]);

  /**
   * Register a new user
   * @param {Object} userData - User registration data (username, password, etc.)
   * @returns {Promise} Promise that resolves with signup response
   */
  const signup = useCallback(async (userData) => {
    return makeRequest(() => 
      api.post('/users/signup', userData)
    );
  }, [makeRequest]);

  /**
   * Search for users by search term
   * @param {string} searchTerm - Term to search for in usernames
   * @returns {Promise} Promise that resolves with search results
   */
  const searchUsers = useCallback(async (searchTerm) => {
    return makeRequest(() => 
      api.get(`/users/search?searchTerm=${searchTerm}`)
    );
  }, [makeRequest]);

  /**
   * Get complete chat history between two users
   * @param {string} user1 - First user's username
   * @param {string} user2 - Second user's username
   * @returns {Promise} Promise that resolves with chat history
   */
  const getChatHistory = useCallback(async (user1, user2) => {
    return makeRequest(() => 
      api.get(`/users/api/messages/history/${user1}/${user2}`)
    );
  }, [makeRequest]);

  /**
   * Get paginated chat history between two users
   * @param {string} user1 - First user's username
   * @param {string} user2 - Second user's username
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Number of messages per page (default: 20)
   * @returns {Promise} Promise that resolves with paginated chat history
   */
  const getChatHistoryPaginated = useCallback(async (user1, user2, page = 0, size = 20) => {
    return makeRequest(() => 
      api.get(`/users/api/messages/history/${user1}/${user2}/paginated?page=${page}&size=${size}`)
    );
  }, [makeRequest]);

  /**
   * Get user's contact list
   * @param {string} username - Username to get contacts for
   * @returns {Promise} Promise that resolves with user contacts
   */
  const getUserContacts = useCallback(async (username) => {
    return makeRequest(() => 
      api.get(`/users/contacts?username=${username}`)
    );
  }, [makeRequest]);

  /**
   * Get count of unread messages for a user
   * @param {string} username - Username to get unread count for
   * @returns {Promise} Promise that resolves with unread message count
   */
  const getUnreadMessageCount = useCallback(async (username) => {
    return makeRequest(() => 
      api.get(`/users/unread-count?username=${username}`)
    );
  }, [makeRequest]);

  /**
   * Mark a specific message as read
   * @param {string} messageId - ID of the message to mark as read
   * @returns {Promise} Promise that resolves when message is marked as read
   */
  const markMessageAsRead = useCallback(async (messageId) => {
    return makeRequest(() => 
      api.post(`/users/mark-read?messageId=${messageId}`)
    );
  }, [makeRequest]);

  /**
   * Mark all messages between two users as read
   * @param {string} sender - Username of the message sender
   * @param {string} receiver - Username of the message receiver
   * @returns {Promise} Promise that resolves when all messages are marked as read
   */
  const markAllMessagesAsRead = useCallback(async (sender, receiver) => {
    return makeRequest(() => 
      api.post(`/users/mark-all-read?sender=${sender}&receiver=${receiver}`)
    );
  }, [makeRequest]);

  // Return all API functions and state variables
  return {
    loading,        // Boolean indicating if any API request is in progress
    error,          // String containing the last error message, or null if no error
    login,          // Function to authenticate user
    signup,         // Function to register new user
    searchUsers,    // Function to search for users
    getChatHistory, // Function to get complete chat history
    getChatHistoryPaginated, // Function to get paginated chat history
    getUserContacts, // Function to get user contacts
    getUnreadMessageCount,   // Function to get unread message count
    markMessageAsRead,       // Function to mark single message as read
    markAllMessagesAsRead    // Function to mark all messages as read
  };
};

export default useApi; 