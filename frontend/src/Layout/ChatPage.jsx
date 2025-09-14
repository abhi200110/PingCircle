// Import necessary React hooks and libraries
import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import SockJS from "sockjs-client"; // WebSocket client library
import { over } from "stompjs"; // STOMP protocol for WebSocket messaging
import SearchBar from "../components/other/SearchBar"; // Component to search for users
import PinnedUsers from "../components/PinnedUsers"; // Component to display pinned users
import UserChatItem from "../components/UserChatItem"; // Component for individual user chat items
import UserDashboard from "../components/UserDashboard"; // Component for user dashboard
import MessageInput from "../components/MessageInput"; // Component for message input with emoji picker
import ScheduledMessageModal from "../components/ScheduledMessageModal"; // Component for scheduling messages
import ScheduledMessagesList from "../components/ScheduledMessagesList"; // Component for viewing scheduled messages
import api from "../config/axios"; // Configured axios instance with JWT authentication
// Simple console-based logger for browser environment
const logger = {
  info: (message, data = {}) => console.log(`[INFO] ${message}`, data),
  debug: (message, data = {}) => console.log(`[DEBUG] ${message}`, data),
  warn: (message, data = {}) => console.warn(`[WARN] ${message}`, data),
  error: (message, data = {}) => console.error(`[ERROR] ${message}`, data),
  websocket: (message, data = {}) => console.log(`[WEBSOCKET] ${message}`, data),
  api: (message, data = {}) => console.log(`[API] ${message}`, data),
  chat: (message, data = {}) => console.log(`[CHAT] ${message}`, data)
};

// Global WebSocket client variable
var stompClient = null;

export const ChatPage = () => {
  // ===== STATE MANAGEMENT =====
  
  // User selection and chat state
  const [selectedUser, setSelectedUser] = useState(null); // Currently selected user to chat with
  const [receiver, setReceiver] = useState(""); // Username of the person receiving messages
  const [message, setMessage] = useState(""); // Current message being typed
  const [tab, setTab] = useState("CHATROOM"); // Current active tab (CHATROOM or username)
  
  // Chat data storage
  const [publicChats, setPublicChats] = useState([]); // Array of public chat messages
  const [privateChats, setPrivateChats] = useState(new Map()); // Map of private chats: {username: [messages]}
  const [username] = useState(localStorage.getItem("chat-username")); // Current logged-in user
  const [pinnedUsers, setPinnedUsers] = useState([]); // Array of pinned usernames
  const [isSending, setIsSending] = useState(false); // Loading state for sending messages
  const [isDeleting, setIsDeleting] = useState(false); // Loading state for deleting conversations
  const [onlineUsers, setOnlineUsers] = useState(new Set()); // Set of online users
  
  // Notification system state
  const [unreadMessages, setUnreadMessages] = useState(new Set()); // Set of usernames with unread messages
  
  // Scheduled message state
  const [showScheduleModal, setShowScheduleModal] = useState(false); // Show/hide schedule modal
  const [showScheduledList, setShowScheduledList] = useState(false); // Show/hide scheduled messages list
  
  // Navigation and refs
  const navigate = useNavigate(); // React Router navigation hook
  const connected = useRef(false); // Ref to track WebSocket connection status
  const chatContainerRef = useRef(null); // Ref to chat container for auto-scroll

  // ===== AUTHENTICATION CHECK =====
  // Redirect to login if no username is stored
  if (!username.trim()) {
    navigate("/login");
    return null;
  }

  // ===== WEB SOCKET CONNECTION MANAGEMENT =====
  // Load initial data when component mounts
  useEffect(() => {
    if (username) {
      logger.info("Loading initial data for user", { username });
      
      // Validate token by making a test request
      const validateToken = async () => {
        try {
          await api.get("/users/search?searchTerm=test");
          // If request succeeds, token is valid
          fetchPinnedUsers(); // Load pinned users
          fetchOnlineUsers(); // Load online users
          connect(); // Establish WebSocket connection
          
          // Set up periodic refresh of online users
          const onlineUsersInterval = setInterval(() => {
            fetchOnlineUsers();
          }, 30000); // Refresh every 30 seconds
          
          return () => {
            clearInterval(onlineUsersInterval);
          };
        } catch (error) {
          if (error.response?.status === 401 || error.response?.status === 403) {
            logger.warn("Token validation failed, redirecting to login");
            localStorage.removeItem("chat-username");
            navigate("/login");
          }
        }
      };
      
      validateToken();
    }
  }, [username, navigate]);

  // Cleanup: disconnect WebSocket when component unmounts
  useEffect(() => {
    return () => {
      if (stompClient && connected.current) {
        userLeft(); // Send leave message
        stompClient.disconnect();
        connected.current = false;
      }
    };
  }, [stompClient]);

  // Handle window beforeunload to send leave message
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (stompClient && connected.current) {
        userLeft(); // Send leave message
      }
    };

    const handleVisibilityChange = () => {
      if (document.hidden) {
        // Page is hidden (user switched tabs or minimized)
        logger.debug("Page hidden, user may be away");
      } else {
        // Page is visible again, refresh online users
        logger.debug("Page visible again, refreshing online users");
        fetchOnlineUsers();
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [stompClient]);

  // ===== USER SELECTION HANDLER =====
  // Called when a user is selected from search or pinned users
  const handlePrivateMessage = (user) => {
    setSelectedUser(user); // Set the selected user
    setReceiver(user.username); // Set the message receiver
    setTab(user.username); // Switch to private chat tab

    // Initialize empty chat array if this user doesn't exist in privateChats
    if (!privateChats.has(user.username)) {
      privateChats.set(user.username, []);
      setPrivateChats(new Map(privateChats));
    }
    
    // Clear unread messages for this user when selected
    setUnreadMessages(prev => {
      const newSet = new Set(prev);
      newSet.delete(user.username);
      return newSet;
    });
    
    // Mark all messages as read when opening the chat
    markAllMessagesAsRead(username, user.username);
    
    // Load chat history for the selected user
    fetchChatHistory(username, user.username);
  };

  // ===== AUTO-SCROLL EFFECT =====
  // Automatically scroll to bottom when new messages arrive
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [publicChats, privateChats, tab]); // Trigger when chat data or tab changes

  // ===== CHAT HISTORY LOADING EFFECT =====
  // Load chat history when switching to a private chat
  useEffect(() => {
    if (tab !== "CHATROOM" && selectedUser) {
      fetchChatHistory(username, selectedUser.username);
    }
  }, [tab, selectedUser]);

  // ===== PINNED USERS LOADING EFFECT =====
  // Load pinned users when component mounts or username changes
  useEffect(() => {
    if (username) {
      fetchPinnedUsers();
    }
  }, [username]);

  // ===== PUBLIC CHAT HISTORY LOADING EFFECT =====
  // Load public chat history when component mounts
  useEffect(() => {
    if (username) {
      fetchPublicChatHistory();
    }
  }, [username]);

  // ===== PUBLIC CHAT HISTORY LOADING EFFECT =====
  // Load public chat history when switching to public chat tab
  useEffect(() => {
    if (tab === "CHATROOM") {
      fetchPublicChatHistory();
    }
  }, [tab]);

  // ===== PINNED USERS MANAGEMENT =====
  
  // Fetch pinned users from the backend
  const fetchPinnedUsers = async () => {
    try {
      const response = await api.get(`/users/pinnedUsers?username=${username}`);
      setPinnedUsers(response.data || []); // Set pinned users or empty array
      logger.debug('Fetched pinned users', { pinnedUsers: response.data });
    } catch (error) {
      console.error('Error fetching pinned users:', error);
      setPinnedUsers([]); // Set empty array on error
    }
  };

  // Handle pin/unpin user actions (called from UserChatItem component)
  const handlePinChange = async (pinnedUsername, isPinned) => {
    try {
      logger.debug('Pin change triggered', { pinnedUsername, isPinned });
      
      if (isPinned) {
        // Add to pinned users immediately for better UX
        setPinnedUsers(prev => {
          if (!prev.includes(pinnedUsername)) {
            const newPinnedUsers = [...prev, pinnedUsername];
            logger.debug('Updated pinned users', { newPinnedUsers });
            return newPinnedUsers;
          }
          return prev;
        });
        logger.debug('User pinned', { pinnedUsername });
      } else {
        // Remove from pinned users immediately for better UX
        setPinnedUsers(prev => {
          const newPinnedUsers = prev.filter(user => user !== pinnedUsername);
          logger.debug('Updated pinned users', { newPinnedUsers });
          return newPinnedUsers;
        });
        logger.debug('User unpinned', { pinnedUsername });
      }
      
      // Refresh pinned users from server to ensure consistency
      await fetchPinnedUsers();
    } catch (error) {
      console.error('Error handling pin change:', error);
      // Revert the optimistic update on error
      await fetchPinnedUsers();
    }
  };

  // ===== WEB SOCKET MESSAGE HANDLERS =====
  
  // Handle public chat messages (JOIN, LEAVE, MESSAGE)
  const onMessageReceived = (payload) => {
    try {
      const payloadData = JSON.parse(payload.body);
      logger.websocket("Group message received", {
        status: payloadData.status,
        content: payloadData.message,
        sender: payloadData.senderName,
        receiver: payloadData.receiverName,
        currentChatsCount: publicChats.length,
        currentTab: tab
      });
      
      switch (payloadData.status) {
        case "JOIN":
          logger.websocket("User joined", { username: payloadData.senderName });
          // Add user to online users list
          setOnlineUsers(prev => new Set([...prev, payloadData.senderName]));
          // When a user joins, add them to private chats list if not already there
          if (payloadData.senderName !== username) {
            setPrivateChats((prevChats) => {
              const newChats = new Map(prevChats);
              if (!newChats.has(payloadData.senderName)) {
                newChats.set(payloadData.senderName, []); // Initialize empty chat array
              }
              return newChats;
            });
          }
          break;
        case "LEAVE":
          logger.websocket("User left", { username: payloadData.senderName });
          // Remove user from online users list
          setOnlineUsers(prev => {
            const newSet = new Set(prev);
            newSet.delete(payloadData.senderName);
            return newSet;
          });
          // When a user leaves, remove them from private chats list
          if (payloadData.senderName !== username) {
            setPrivateChats((prevChats) => {
              const newChats = new Map(prevChats);
              newChats.delete(payloadData.senderName);
              return newChats;
            });
          }
          break;
        case "MESSAGE":
          logger.websocket("Processing group message", {
            content: payloadData.message,
            sender: payloadData.senderName
          });
          
          // Add public message to public chat
          setPublicChats((prev) => {
            logger.debug("Updating public chats", {
              previousCount: prev.length,
              newCount: prev.length + 1
            });
            return [...prev, payloadData];
          });
          break;
        default:
          logger.warn("Unknown status received", { status: payloadData.status });
      }
    } catch (error) {
      logger.error("Error processing group message", {
        error: error.message,
        stack: error.stack
      });
    }
  };

  // Handle private messages from other users
  const onPrivateMessage = (payload) => {
    try {
      const payloadData = JSON.parse(payload.body);
      logger.websocket("Private message received", {
        sender: payloadData.senderName,
        receiver: payloadData.receiverName,
        content: payloadData.message,
        currentUser: username,
        currentTab: tab
      });
      
      // Determine which user's chat to update
      let targetUser;
      if (payloadData.senderName === username) {
        // This is a message I sent - add to receiver's chat
        targetUser = payloadData.receiverName;
        logger.debug("I sent this message, adding to receiver's chat", { targetUser });
      } else if (payloadData.receiverName === username) {
        // This is a message sent to me - add to sender's chat
        targetUser = payloadData.senderName;
        logger.debug("I received this message, adding to sender's chat", { targetUser });
      } else {
        logger.debug("Message not for current user, ignoring");
        return;
      }
      
      logger.debug("Target user for chat update", { targetUser });
      
      // Add the received message to the appropriate chat history
      setPrivateChats((prevChats) => {
        const newChats = new Map(prevChats);
        const existingMessages = newChats.get(targetUser) || [];
        logger.debug("Existing messages count", { 
          targetUser, 
          count: existingMessages.length 
        });
        
        // Check if message already exists to prevent duplicates
        // Use a more precise duplicate detection based on sender, receiver, message content, and timestamp
        const messageExists = existingMessages.some(msg => 
          msg.senderName === payloadData.senderName &&
          msg.receiverName === payloadData.receiverName &&
          msg.message === payloadData.message &&
          Math.abs((msg.timestamp || 0) - (payloadData.timestamp || 0)) < 2000 // Within 2 seconds
        );
        
        if (!messageExists) {
          const updatedMessages = [...existingMessages, payloadData];
          logger.debug("Message added to chat", { 
            targetUser, 
            updatedCount: updatedMessages.length 
          });
          newChats.set(targetUser, updatedMessages);
          
          // Add notification logic for received messages
          if (payloadData.senderName !== username) {
            // This is a message received from another user
            logger.debug("Adding notification for message", { 
              sender: payloadData.senderName 
            });
            
            // Add to unread messages if not currently viewing this chat
            if (tab !== payloadData.senderName) {
              setUnreadMessages(prev => new Set([...prev, payloadData.senderName]));
              logger.debug("Blue dot added for unread message", { 
                sender: payloadData.senderName 
              });
            }
          }
        } else {
          logger.debug("Message already exists, skipping");
        }
        return newChats;
      });
    } catch (error) {
      logger.error("Error processing private message", { error: error.message });
    }
  };

  // ===== WEB SOCKET CONNECTION FUNCTIONS =====
  


  // Called when WebSocket connection fails
  const onError = (err) => {
    logger.error("WebSocket connection error", { error: err.message });
    
    // Remove current user from online users list
    setOnlineUsers(prev => {
      const newSet = new Set(prev);
      newSet.delete(username);
      return newSet;
    });
    
    // Try to reconnect after a delay
    setTimeout(() => {
      if (!connected.current) {
        logger.info("Attempting to reconnect");
        connect();
      }
    }, 5000); // Wait 5 seconds before trying to reconnect
  };

  // Called when WebSocket connection is established
  const onConnect = () => {
    logger.info("Connected to WebSocket");
    connected.current = true;

    // Add current user to online users list
    setOnlineUsers(prev => new Set([...prev, username]));

    // Subscribe to public chat channel
    stompClient.subscribe("/chatroom/public", onMessageReceived);
    logger.debug("Subscribed to /chatroom/public");
    
    // Subscribe to private messages for this user
    stompClient.subscribe(`/user/${username}/private`, onPrivateMessage);
    logger.debug(`Subscribed to /user/${username}/private`);

    userJoin(); // Send join message to public chat
    
    // Refresh online users after successful connection
    fetchOnlineUsers();
  };

  // Initialize WebSocket connection
  const connect = () => {
    let sock = new SockJS("http://localhost:8080/ws"); // Create SockJS connection
    stompClient = over(sock); // Wrap with STOMP client
    stompClient.connect({}, onConnect, onError); // Connect with callbacks
  };

  // ===== USER PRESENCE MANAGEMENT =====
  
  // Send join message to public chat when user connects
  const userJoin = () => {
    let chatMessage = {
      senderName: username,
      status: "JOIN", // Indicates user joined the chat
    };

    stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
  };

  // Send leave message to public chat when user disconnects
  const userLeft = () => {
    let chatMessage = {
      senderName: username,
      status: "LEAVE", // Indicates user left the chat
    };

    stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
  };

  // Handle user logout
  const handleLogout = () => {
    userLeft(); // Send leave message
    localStorage.removeItem("chat-username"); // Clear stored username
    localStorage.removeItem("jwt-token"); // Clear JWT token
    navigate("/login"); // Redirect to login page
  };

  // ===== MESSAGE SENDING FUNCTIONS =====
  
  // Send message to public chat room
  const sendMessage = () => {
    if (message.trim().length > 0) {
      setIsSending(true); // Start loading
      
      let chatMessage = {
        senderName: username,
        status: "MESSAGE",
        message: message,
        timestamp: Date.now(), // Add timestamp for proper ordering
      };

      logger.chat("Sending public message", {
        content: message,
        status: chatMessage.status,
        websocketConnected: connected.current,
        currentTab: tab
      });

      // Send message via WebSocket to backend
      try {
        stompClient.send(
          "/app/message", // Public chat endpoint
          {},
          JSON.stringify(chatMessage)
        );
        logger.debug("Public message sent successfully");
      } catch (error) {
        logger.error("Error sending message via WebSocket", { error: error.message });
      }

      setMessage(""); // Clear message input
      setIsSending(false); // Stop loading
    } else {
      logger.debug("Message is empty, not sending");
    }
  };

  // Send private message to specific user
  const sendPrivate = () => {
    if (message.trim().length > 0 && receiver) {
      setIsSending(true); // Start loading
      
      let chatMessage = {
        senderName: username,
        receiverName: receiver, // Target user
        message: message,
        status: "MESSAGE",
        timestamp: Date.now(), // Add timestamp for proper ordering
      };

      logger.chat("Sending private message", {
        from: username,
        to: receiver,
        content: message,
        websocketConnected: connected.current,
        currentTab: tab
      });

      try {
        // Send message via WebSocket to backend
        stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
        logger.debug("Private message sent successfully");

        setMessage(""); // Clear message input
      } catch (error) {
        logger.error("Error sending private message", { error: error.message });
        // The backend will store it and deliver when the user comes back online
      }
      
      setIsSending(false); // Stop loading
    } else {
      logger.debug("Message is empty or no receiver selected");
    }
  };

  // ===== CHAT MANAGEMENT FUNCTIONS =====
  
  // Switch to a specific user's chat tab
  const tabReceiverSet = (name) => {
    setReceiver(name); // Set message receiver
    setTab(name); // Switch to user's chat tab
    
    // Clear unread messages for this user when switching to their chat
    if (name && name !== "CHATROOM") {
      setUnreadMessages(prev => {
        const newSet = new Set(prev);
        newSet.delete(name);
        return newSet;
      });
      
      // Mark all messages as read when opening the chat
      markAllMessagesAsRead(username, name);
      
      // Ensure we have chat history for this user
      setPrivateChats((prevChats) => {
        if (!prevChats.has(name)) {
          const newChats = new Map(prevChats);
          newChats.set(name, []); // Initialize empty chat array
          return newChats;
        }
        return prevChats;
      });
      fetchChatHistory(username, name); // Load chat history from database
    }
  };

  // Fetch chat history between two users from the backend
  const fetchChatHistory = async (user1, user2) => {
    try {
      const response = await api.get(
        `/chat/history/${user1}/${user2}` // API endpoint for chat history
      );

      if (response.status === 200) {
        // Ensure we always set an array, even if response.data is null/undefined
        const messages = response.data || [];
        // Convert database messages to frontend format
        const convertedMessages = messages.map(msg => ({
          senderName: msg.senderName,
          receiverName: msg.receiverName,
          message: msg.message,
          status: msg.status || "MESSAGE", // Use status from database or default to "MESSAGE"
          timestamp: msg.timestamp
        }));
        setPrivateChats((prevChats) => {
          const newChats = new Map(prevChats);
          newChats.set(user2, convertedMessages); // Update chat history for this user
          return newChats;
        });
        logger.api("Chat history loaded", { 
          user: user2, 
          messageCount: convertedMessages.length 
        });
      } else {
        logger.error("Failed to fetch chat history", { status: response.status });
      }
    } catch (error) {
      logger.error("Error fetching chat history", { error: error.message });
      // Set empty array on error to ensure UI works
      setPrivateChats((prevChats) => {
        const newChats = new Map(prevChats);
        newChats.set(user2, []); // Set empty array on error
        return newChats;
      });
    }
  };

  // Fetch public chat history from the backend
  const fetchPublicChatHistory = async () => {
    logger.api("Fetching public chat history");
    try {
      const response = await api.get('/chat/public/history');
      logger.debug("API response", { 
        status: response.status, 
        dataLength: response.data?.length 
      });

      if (response.status === 200) {
        const messages = response.data || [];
        logger.debug("Raw messages from backend", { count: messages.length });
        
        // Filter out messages with null content and only show actual chat messages
        const validMessages = messages.filter(msg => {
          const isValid = msg.message && msg.message.trim() !== '';
          logger.debug("Message validation", { 
            sender: msg.senderName, 
            message: msg.message, 
            isValid 
          });
          return isValid;
        });
        logger.debug("Valid messages count", { count: validMessages.length });
        
        // Convert database messages to frontend format
        const convertedMessages = validMessages.map(msg => {
          const converted = {
            senderName: msg.senderName || 'Unknown',
            message: msg.message || '',
            status: "MESSAGE", // Always set to MESSAGE for public chat
            timestamp: msg.timestamp || Date.now()
          };
          logger.debug("Converted message", converted);
          return converted;
        });
        
        setPublicChats(convertedMessages);
        logger.api("Public chat history loaded", { messageCount: convertedMessages.length });
      } else {
        logger.error("Failed to fetch public chat history", { 
          status: response.status, 
          data: response.data 
        });
      }
    } catch (error) {
      logger.error("Error fetching public chat history", { 
        error: error.message, 
        stack: error.stack 
      });
      setPublicChats([]); // Set empty array on error
    }
  };

  // Delete conversation with a specific user
  const deleteConversation = async (targetUsername) => {
    if (!targetUsername || targetUsername === "CHATROOM") {
      logger.debug("Cannot delete public chat or invalid user");
      return;
    }

    setIsDeleting(true);
    logger.api("Deleting conversation", { targetUsername });

    try {
      // Call backend API to delete conversation
      const response = await api.delete(`/chat/delete/${username}/${targetUsername}`);
      
      if (response.status === 200) {
        logger.api("Conversation deleted successfully", { targetUsername });
        
        // Remove conversation from local state
        setPrivateChats((prevChats) => {
          const newChats = new Map(prevChats);
          newChats.delete(targetUsername);
          return newChats;
        });

        // If we're currently viewing this conversation, switch to public chat
        if (tab === targetUsername) {
          setTab("CHATROOM");
          setReceiver("");
          setSelectedUser(null);
        }

        // Show success message (you can add a toast notification here)
        alert(`Your messages in the conversation with ${targetUsername} have been deleted`);
      } else {
        logger.error("Failed to delete conversation", { status: response.status });
        alert("Failed to delete conversation. Please try again.");
      }
    } catch (error) {
      logger.error("Error deleting conversation", { error: error.message });
      alert("Error deleting conversation. Please try again.");
    } finally {
      setIsDeleting(false);
    }
  };

  // Delete all public chat messages
  const deletePublicChat = async () => {
    setIsDeleting(true);
    logger.api("Deleting public chat messages");

    try {
      // Call backend API to delete public chat
      const response = await api.delete(`/chat/delete/public`);
      
      if (response.status === 200) {
        logger.api("Public chat messages deleted successfully");
        
        // Clear public chat from local state
        setPublicChats([]);

        // Show success message
        alert("Public chat messages have been deleted");
      } else {
        logger.error("Failed to delete public chat", { status: response.status });
        alert("Failed to delete public chat. Please try again.");
      }
    } catch (error) {
      logger.error("Error deleting public chat", { error: error.message });
      alert("Error deleting public chat. Please try again.");
    } finally {
      setIsDeleting(false);
    }
  };

  // Mark all messages as read between two users
  const markAllMessagesAsRead = async (sender, receiver) => {
    try {
      await api.post('/chat/mark-all-read', null, {
        params: {
          sender: sender,
          receiver: receiver
        }
      });
      logger.api("Marked all messages as read", { sender, receiver });
    } catch (error) {
      logger.error("Error marking messages as read", { error: error.message });
    }
  };

  // ===== SCHEDULED MESSAGE FUNCTIONS =====

  // Handle scheduling a message
  const handleScheduleMessage = async (requestData) => {
    try {
      logger.info("Scheduling message", { 
        sender: requestData.senderName, 
        receiver: requestData.receiverName,
        scheduledTime: new Date(requestData.scheduledTime)
      });

      const response = await api.post("/chat/schedule-message", requestData);
      
      if (response.status === 200) {
        logger.info("Message scheduled successfully", { messageId: response.data.id });
        // Show success message or update UI
        alert("Message scheduled successfully!");
      }
    } catch (error) {
      logger.error("Error scheduling message", { error: error.message });
      const errorMessage = error.response?.data || error.message || "Failed to schedule message";
      alert(`Error scheduling message: ${errorMessage}`);
    }
  };

  // Handle canceling a scheduled message
  const handleCancelScheduledMessage = async (messageId) => {
    try {
      logger.info("Canceling scheduled message", { messageId });
      
      const response = await api.delete(`/chat/cancel-message/${messageId}?senderName=${username}`);
      
      if (response.status === 200) {
        logger.info("Message canceled successfully");
        alert("Message canceled successfully!");
        // Refresh the scheduled messages list if it's open
        if (showScheduledList) {
          // The ScheduledMessagesList component will handle refreshing
        }
      }
    } catch (error) {
      logger.error("Error canceling scheduled message", { error: error.message });
      const errorMessage = error.response?.data || error.message || "Failed to cancel message";
      alert(`Error canceling message: ${errorMessage}`);
    }
  };

  // Fetch initial online users
  const fetchOnlineUsers = async () => {
    try {
      const response = await api.get('/chat/online-users');
      logger.api("Fetched online users", { count: response.data?.length });
      setOnlineUsers(new Set(response.data));
    } catch (error) {
      logger.error("Error fetching online users", { error: error.message });
    }
  };

  // ===== RENDER UI =====
  
  return (
    <div className="d-flex flex-column vh-100">
      {/* User Dashboard - Top */}
      <UserDashboard 
        username={username} 
        onLogout={handleLogout} 
      />

      {/* Main Chat Area - Takes remaining height */}
      <div className="d-flex flex-grow-1" style={{ minHeight: 0 }}>
        {/* Left Sidebar - User List and Navigation */}
        <div className="d-flex flex-column bg-light" style={{ 
          width: "300px", 
          height: "100%"
        }}>
          {/* Sidebar Header - Title and Search */}
          <div className="p-4 border-bottom border-secondary bg-white">
            <h2 className="h5 fw-semibold text-primary">Chats</h2>
            <SearchBar onUserSelect={handlePrivateMessage} /> {/* Search for users */}
          </div>

          {/* Single Scrollable Content Area - All sections in one scrollable container */}
          <div className="flex-grow-1 bg-light sidebar-content sidebar-scroll" style={{ 
            overflowY: "auto",
            scrollbarWidth: "thin",
            scrollbarColor: "#6c757d #f8f9fa"
          }}>
            {/* Pinned Users Section - Quick access to frequently contacted users */}
            <PinnedUsers 
              currentUser={username}
              pinnedUsers={pinnedUsers}
              onUserSelect={(user) => {
                setSelectedUser(user); // Set selected user
                setReceiver(user.username); // Set message receiver
                setTab(user.username); // Switch to user's chat tab
                if (user.username) {
                  // Clear unread messages for this user
                  setUnreadMessages(prev => {
                    const newSet = new Set(prev);
                    newSet.delete(user.username);
                    return newSet;
                  });
                  
                  // Mark all messages as read when opening the chat
                  markAllMessagesAsRead(username, user.username);
                  
                  // Initialize empty chat array if user doesn't exist in privateChats
                  setPrivateChats((prevChats) => {
                    if (!prevChats.has(user.username)) {
                      const newChats = new Map(prevChats);
                      newChats.set(user.username, []); // Create empty chat array
                      return newChats;
                    }
                    return prevChats;
                  });
                  fetchChatHistory(username, user.username); // Load chat history
                }
              }}
              onUnpin={(pinnedUsername) => {
                // Remove from pinned users
                setPinnedUsers(prev => prev.filter(user => user !== pinnedUsername));
                logger.debug('User unpinned from PinnedUsers', { pinnedUsername });
              }}
              selectedUser={selectedUser}
              onlineUsers={onlineUsers}
              unreadMessages={unreadMessages}
            />
            
            {/* Public Chat Room Option - Switch to public chat */}
            <div className="p-4 border-top border-secondary bg-white">
              <div
                className={`p-3 rounded cursor-pointer transition-colors ${
                  tab === "CHATROOM" 
                    ? "bg-primary text-white" // Active state
                    : "bg-light hover-bg-primary hover-text-white text-dark" // Inactive state
                }`}
                onClick={() => setTab("CHATROOM")} // Switch to public chat
              >
                <div className="d-flex align-items-center gap-2">
                  <span className="fs-5">💬</span>
                  <span className="fw-medium">Chat Room</span>
                </div>
              </div>
            </div>

            {/* Recent Chats Section - List of users with recent conversations */}
            <div className="p-4 bg-light">
              <h3 className="small fw-semibold text-muted mb-3">Recent Chats</h3>
              <div className="d-flex flex-column gap-2">
                {/* Map through all users in privateChats to show recent conversations */}
                {[...privateChats.keys()].map((name, index) => (
                  <UserChatItem
                    key={index}
                    user={{ username: name }}
                    currentUser={username}
                    isSelected={tab === name} // Highlight if this user is selected
                    isOnline={onlineUsers.has(name)} // Check if user is online
                    hasUnreadMessage={unreadMessages.has(name)} // Show blue dot for unread messages
                    pinnedUsers={pinnedUsers} // Pass pinned users for status checking
                    onUserSelect={(user) => {
                      tabReceiverSet(user.username); // Switch to user's chat
                      fetchChatHistory(username, user.username); // Load chat history
                    }}
                    onPinChange={handlePinChange} // Handle pin/unpin actions
                    onDeleteConversation={deleteConversation} // Pass delete function
                  />
                ))}
              </div>
            </div>
            
            {/* Bottom padding for better scrolling experience */}
            <div className="pb-4"></div>
          </div>
        </div>

        {/* Right Side - Chat Messages and Input */}
        <div className="d-flex flex-column flex-grow-1 bg-white" style={{ minHeight: 0 }}>
          {/* Chat Header */}
          {tab === "CHATROOM" ? (
            // Public Chat Header
            <div className="d-flex justify-content-between align-items-center p-3 border-bottom bg-light">
              <div className="d-flex align-items-center gap-2">
                <span className="fw-semibold text-primary">Public Chat Room</span>
                <span className="badge bg-success">Online</span>
              </div>
              <button
                type="button"
                className="btn btn-outline-danger btn-sm"
                onClick={() => {
                  if (window.confirm("Are you sure you want to clear all public chat messages? This action cannot be undone.")) {
                    deletePublicChat();
                  }
                }}
                disabled={isDeleting}
              >
                {isDeleting ? "Clearing..." : "Clear Chat"}
              </button>
            </div>
          ) : (
            // Private Chat Header
            <div className="d-flex justify-content-between align-items-center p-3 border-bottom bg-light">
              <div className="d-flex align-items-center gap-2">
                <span className="fw-semibold text-primary">Chat with {tab}</span>
                <span className={`badge ${onlineUsers.has(tab) ? 'bg-success' : 'bg-secondary'}`}>
                  {onlineUsers.has(tab) ? 'Online' : 'Offline'}
                </span>
              </div>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-outline-primary btn-sm d-flex align-items-center gap-2"
                  onClick={() => setShowScheduleModal(true)}
                  title="Schedule a message"
                  style={{ 
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    border: 'none',
                    color: 'white',
                    fontWeight: '600',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseOver={(e) => {
                    e.target.style.transform = 'translateY(-2px)';
                    e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.4)';
                  }}
                  onMouseOut={(e) => {
                    e.target.style.transform = 'translateY(0)';
                    e.target.style.boxShadow = 'none';
                  }}
                >
                  <span style={{ fontSize: '16px' }}>📅</span>
                  <span>Schedule</span>
                </button>
                <button
                  type="button"
                  className="btn btn-outline-info btn-sm d-flex align-items-center gap-2"
                  onClick={() => setShowScheduledList(true)}
                  title="View scheduled messages"
                  style={{ 
                    background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                    border: 'none',
                    color: 'white',
                    fontWeight: '600',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseOver={(e) => {
                    e.target.style.transform = 'translateY(-2px)';
                    e.target.style.boxShadow = '0 4px 12px rgba(240, 147, 251, 0.4)';
                  }}
                  onMouseOut={(e) => {
                    e.target.style.transform = 'translateY(0)';
                    e.target.style.boxShadow = 'none';
                  }}
                >
                  <span style={{ fontSize: '16px' }}>⏰</span>
                  <span>Scheduled</span>
                </button>
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm"
                  onClick={() => {
                    if (window.confirm(`Are you sure you want to delete your messages in the conversation with ${tab}? This will only delete messages you sent, not messages from ${tab}. This action cannot be undone.`)) {
                      deleteConversation(tab);
                    }
                  }}
                  disabled={isDeleting}
                >
                  {isDeleting ? "Deleting..." : "Delete Chat"}
                </button>
              </div>
            </div>
          )}

          {/* Chat Messages Area - Scrollable */}
          <div 
            ref={chatContainerRef}
            className="flex-grow-1 overflow-auto p-4 d-flex flex-column gap-3"
            style={{ minHeight: 0 }}
          >
            {tab === "CHATROOM"
              ? // Render Public Chat Messages
                publicChats.map((message, index) => (
                  <div
                    className={`d-flex ${
                      message.senderName !== username
                        ? "justify-content-start" // Other user's message (left side)
                        : "justify-content-end" // Current user's message (right side)
                    }`}
                    key={index}
                  >
                    <div
                      className={`p-2 d-flex flex-column ${
                        message.senderName !== username
                          ? "bg-white rounded-top rounded-end border border-secondary" // Other user's message style
                          : "bg-primary rounded-top rounded-start text-white" // Current user's message style
                      }`}
                      style={{ maxWidth: "400px" }}
                    >
                      {/* Sender name for public chat */}
                      {message.senderName !== username && (
                        <div className="rounded bg-info mb-2 p-1 text-white">
                          {message.senderName}
                        </div>
                      )}
                      {/* Message text */}
                      <div
                        className={
                          message.senderName === username ? "text-white" : "text-dark"
                        }
                      >
                        {message.message}
                      </div>
                    </div>
                  </div>
                ))
              : // Render Private Chat Messages
                (privateChats.get(tab) || []).map((message, index) => (
                  <div
                    className={`d-flex ${
                      message.senderName !== username
                        ? "justify-content-start" // Other user's message (left side)
                        : "justify-content-end" // Current user's message (right side)
                    }`}
                    key={index}
                  >
                    <div
                      className={`p-2 d-flex flex-column ${
                        message.senderName !== username
                          ? "bg-white rounded-top rounded-end border border-secondary" // Other user's message style
                          : "bg-primary rounded-top rounded-start text-white" // Current user's message style
                      }`}
                      style={{ maxWidth: "400px" }}
                    >
                      {/* Message text */}
                      <div
                        className={
                          message.senderName === username ? "text-white" : "text-dark"
                        }
                      >
                        {message.message}
                      </div>
                    </div>
                  </div>
                ))}
          </div>

          {/* Message Input Area - Bottom of chat */}
          <MessageInput
            message={message}
            setMessage={setMessage}
            onSendMessage={tab === "CHATROOM" ? sendMessage : sendPrivate}
            disabled={isSending}
          />
        </div>
      </div>

      {/* Scheduled Message Modal */}
      <ScheduledMessageModal
        isOpen={showScheduleModal}
        onClose={() => setShowScheduleModal(false)}
        currentUser={username}
        selectedUser={receiver}
        onSchedule={handleScheduleMessage}
      />

      {/* Scheduled Messages List Modal */}
      {showScheduledList && (
        <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-xl modal-dialog-centered">
            <ScheduledMessagesList
              currentUser={username}
              onClose={() => setShowScheduledList(false)}
              onCancelMessage={handleCancelScheduledMessage}
            />
          </div>
        </div>
      )}
    </div>
  );
};