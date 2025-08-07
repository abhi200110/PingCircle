import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import SockJS from "sockjs-client"; // WebSocket client library
import { over } from "stompjs"; // STOMP protocol for WebSocket messaging
import SearchBar from "../components/other/SearchBar"; // Component to search for users
import PinnedUsers from "../components/PinnedUsers"; // Component to display pinned users
import UserChatItem from "../components/UserChatItem"; // Component for individual user chat items
import UserDashboard from "../components/UserDashboard"; // Component for user dashboard
import MessageInput from "../components/MessageInput"; // Component for message input with emoji picker
import api from "../config/axios"; // Configured axios instance with JWT authentication

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
  const [allUsers, setAllUsers] = useState([]); // All available users (currently unused)
  const [isOnline, setIsOnline] = useState(true); // User online status
  const [isSending, setIsSending] = useState(false); // Loading state for sending messages
  const [isDeleting, setIsDeleting] = useState(false); // Loading state for deleting conversations
  const [onlineUsers, setOnlineUsers] = useState(new Set()); // Set of online users
  
  // Navigation and refs
  const navigate = useNavigate(); // React Router navigation hook
  const connected = useRef(false); // Ref to track WebSocket connection status
  const chatContainerRef = useRef(null); // Ref to chat container for auto-scroll

  // ===== AUTHENTICATION CHECK =====
  // Redirect to login if no username is stored
  if (!username.trim()) {
    navigate("/login");
  }

  // ===== WEB SOCKET CONNECTION MANAGEMENT =====
  // Load initial data when component mounts
  useEffect(() => {
    if (username) {
      console.log("Loading initial data for user:", username);
      fetchPinnedUsers(); // Load pinned users
      fetchOnlineUsers(); // Load online users
      connect(); // Establish WebSocket connection
    }
  }, [username]);

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

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
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
      const response = await api.get(`/users/pinned-users?username=${username}`);
      setPinnedUsers(response.data || []); // Set pinned users or empty array
    } catch (error) {
      console.error('Error fetching pinned users:', error);
      setPinnedUsers([]); // Set empty array on error
    }
  };

  // Handle pin/unpin user actions (called from UserChatItem component)
  const handlePinChange = (pinnedUsername, isPinned) => {
    if (isPinned) {
      setPinnedUsers(prev => [...prev, pinnedUsername]); // Add to pinned users
    } else {
      setPinnedUsers(prev => prev.filter(user => user !== pinnedUsername)); // Remove from pinned users
    }
  };

  // ===== WEB SOCKET MESSAGE HANDLERS =====
  
  // Handle public chat messages (JOIN, LEAVE, MESSAGE)
  const onMessageReceived = (payload) => {
    try {
      const payloadData = JSON.parse(payload.body);
      console.log("=== GROUP MESSAGE RECEIVED ===");
      console.log("Payload data:", payloadData);
      console.log("Message status:", payloadData.status);
      console.log("Message content:", payloadData.message);
      console.log("Message sender:", payloadData.senderName);
      console.log("Message receiver:", payloadData.receiverName);
      console.log("Current public chats count:", publicChats.length);
      console.log("Current tab:", tab);
      
      switch (payloadData.status) {
        case "JOIN":
          console.log("User joined:", payloadData.senderName);
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
          console.log("User left:", payloadData.senderName);
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
          console.log("Processing GROUP MESSAGE status");
          console.log("Message content:", payloadData.message);
          console.log("Message sender:", payloadData.senderName);
          
          // Add public message to public chat
          setPublicChats((prev) => {
            console.log("Previous public chats count:", prev.length);
            console.log("Previous messages:", prev);
            const newChats = [...prev, payloadData];
            console.log("New public chats count:", newChats.length);
            console.log("Added message:", payloadData);
            console.log("Updated messages:", newChats);
            return newChats;
          });
          break;
        default:
          console.warn("Unknown status received:", payloadData.status);
      }
    } catch (error) {
      console.error("Error processing group message:", error);
      console.error("Error details:", error.message);
      console.error("Error stack:", error.stack);
    }
  };

  // Handle private messages from other users
  const onPrivateMessage = (payload) => {
    try {
      const payloadData = JSON.parse(payload.body);
      console.log("=== PRIVATE MESSAGE RECEIVED ===");
      console.log("Payload data:", payloadData);
      console.log("Current user:", username);
      console.log("Message sender:", payloadData.senderName);
      console.log("Message receiver:", payloadData.receiverName);
      console.log("Message content:", payloadData.message);
      console.log("Current tab:", tab);
      
      // Determine which user's chat to update
      let targetUser;
      if (payloadData.senderName === username) {
        // This is a message I sent - add to receiver's chat
        targetUser = payloadData.receiverName;
        console.log("I sent this message, adding to receiver's chat:", targetUser);
      } else if (payloadData.receiverName === username) {
        // This is a message sent to me - add to sender's chat
        targetUser = payloadData.senderName;
        console.log("I received this message, adding to sender's chat:", targetUser);
      } else {
        console.log("Message not for current user, ignoring");
        return;
      }
      
      console.log("Target user for chat update:", targetUser);
      
      // Add the received message to the appropriate chat history
      setPrivateChats((prevChats) => {
        const newChats = new Map(prevChats);
        const existingMessages = newChats.get(targetUser) || [];
        console.log("Existing messages count for", targetUser + ":", existingMessages.length);
        
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
          console.log("✅ Message added to chat for", targetUser);
          console.log("Updated messages count:", updatedMessages.length);
          newChats.set(targetUser, updatedMessages);
        } else {
          console.log("⚠️ Message already exists, skipping");
        }
        return newChats;
      });
    } catch (error) {
      console.error("❌ Error processing private message:", error);
    }
  };

  // ===== WEB SOCKET CONNECTION FUNCTIONS =====
  
  // Called when WebSocket connection is established
  const onConnect = () => {
    console.log("Connected to WebSocket");
    connected.current = true;
    setIsOnline(true); // Set user as online when connected

    // Add current user to online users list
    setOnlineUsers(prev => new Set([...prev, username]));

    // Subscribe to public chat channel
    stompClient.subscribe("/chatroom/public", onMessageReceived);
    console.log("Subscribed to /chatroom/public");
    
    // Subscribe to private messages for this user
    stompClient.subscribe(`/user/${username}/private`, onPrivateMessage);
    console.log(`Subscribed to /user/${username}/private`);

    userJoin(); // Send join message to public chat
  };

  // Called when WebSocket connection fails
  const onError = (err) => {
    console.error("WebSocket connection error:", err);
    setIsOnline(false); // Set user as offline when connection fails
    
    // Remove current user from online users list
    setOnlineUsers(prev => {
      const newSet = new Set(prev);
      newSet.delete(username);
      return newSet;
    });
    
    // Try to reconnect after a delay
    setTimeout(() => {
      if (!connected.current) {
        console.log("Attempting to reconnect...");
        connect();
      }
    }, 5000); // Wait 5 seconds before trying to reconnect
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

      console.log("=== SENDING PUBLIC MESSAGE ===");
      console.log("Message object:", chatMessage);
      console.log("Message content:", message);
      console.log("Message status:", chatMessage.status);
      console.log("WebSocket connected:", connected.current);
      console.log("Current tab:", tab);

      // Send message via WebSocket to backend
      try {
        stompClient.send(
          "/app/message", // Public chat endpoint
          {},
          JSON.stringify(chatMessage)
        );
        console.log("✅ Message sent via WebSocket successfully");
      } catch (error) {
        console.error("❌ Error sending message via WebSocket:", error);
      }

      setMessage(""); // Clear message input
      setIsSending(false); // Stop loading
    } else {
      console.log("Message is empty, not sending");
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

      console.log("=== SENDING PRIVATE MESSAGE ===");
      console.log("Message object:", chatMessage);
      console.log("From:", username, "To:", receiver);
      console.log("Message content:", message);
      console.log("WebSocket connected:", connected.current);
      console.log("Current tab:", tab);

      try {
        // Send message via WebSocket to backend
        stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
        console.log("✅ Private message sent via WebSocket successfully");

        setMessage(""); // Clear message input
      } catch (error) {
        console.error("❌ Error sending private message:", error);
        // The backend will store it and deliver when the user comes back online
      }
      
      setIsSending(false); // Stop loading
    } else {
      console.log("Message is empty or no receiver selected");
    }
  };

  // ===== CHAT MANAGEMENT FUNCTIONS =====
  
  // Switch to a specific user's chat tab
  const tabReceiverSet = (name) => {
    setReceiver(name); // Set message receiver
    setTab(name); // Switch to user's chat tab
    // Ensure we have chat history for this user
    if (name && name !== "CHATROOM") {
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
        `/users/api/messages/history/${user1}/${user2}` // API endpoint for chat history
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
        console.log(`Chat history loaded for ${user2}:`, convertedMessages.length, 'messages');
      } else {
        console.error("Failed to fetch chat history:", response.status);
      }
    } catch (error) {
      console.error("Error fetching chat history:", error);
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
    console.log("=== FETCHING PUBLIC CHAT HISTORY ===");
    try {
      const response = await api.get('/users/api/messages/public/history');
      console.log("API response status:", response.status);
      console.log("API response data:", response.data);

      if (response.status === 200) {
        const messages = response.data || [];
        console.log('Raw messages from backend:', messages);
        console.log('Number of messages received:', messages.length);
        
        // Filter out messages with null content and only show actual chat messages
        const validMessages = messages.filter(msg => {
          const isValid = msg.message && msg.message.trim() !== '';
          console.log(`Message from ${msg.senderName}: "${msg.message}" - Valid: ${isValid}`);
          return isValid;
        });
        console.log('Valid messages (with content):', validMessages.length);
        
        // Convert database messages to frontend format
        const convertedMessages = validMessages.map(msg => {
          console.log('Processing message:', msg);
          const converted = {
            senderName: msg.senderName || 'Unknown',
            message: msg.message || '',
            status: "MESSAGE", // Always set to MESSAGE for public chat
            timestamp: msg.timestamp || Date.now()
          };
          console.log('Converted message:', converted);
          return converted;
        });
        
        console.log('Converted messages:', convertedMessages);
        setPublicChats(convertedMessages);
        console.log('Public chat history loaded:', convertedMessages.length, 'messages');
      } else {
        console.error("Failed to fetch public chat history:", response.status);
        console.error("Response data:", response.data);
      }
    } catch (error) {
      console.error("Error fetching public chat history:", error);
      console.error("Error details:", error.message);
      console.error("Error stack:", error.stack);
      setPublicChats([]); // Set empty array on error
    }
  };

  // Delete conversation with a specific user
  const deleteConversation = async (targetUsername) => {
    if (!targetUsername || targetUsername === "CHATROOM") {
      console.log("Cannot delete public chat or invalid user");
      return;
    }

    setIsDeleting(true);
    console.log(`Deleting conversation with ${targetUsername}`);

    try {
      // Call backend API to delete conversation
      const response = await api.delete(`/users/api/messages/delete/${username}/${targetUsername}`);
      
      if (response.status === 200) {
        console.log(`Conversation with ${targetUsername} deleted successfully`);
        
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
        console.error("Failed to delete conversation:", response.status);
        alert("Failed to delete conversation. Please try again.");
      }
    } catch (error) {
      console.error("Error deleting conversation:", error);
      alert("Error deleting conversation. Please try again.");
    } finally {
      setIsDeleting(false);
    }
  };

  // Delete all public chat messages
  const deletePublicChat = async () => {
    setIsDeleting(true);
    console.log("Deleting public chat messages");

    try {
      // Call backend API to delete public chat
      const response = await api.delete(`/users/api/messages/delete/public`);
      
      if (response.status === 200) {
        console.log("Public chat messages deleted successfully");
        
        // Clear public chat from local state
        setPublicChats([]);

        // Show success message
        alert("Public chat messages have been deleted");
      } else {
        console.error("Failed to delete public chat:", response.status);
        alert("Failed to delete public chat. Please try again.");
      }
    } catch (error) {
      console.error("Error deleting public chat:", error);
      alert("Error deleting public chat. Please try again.");
    } finally {
      setIsDeleting(false);
    }
  };

  // Fetch initial online users
  const fetchOnlineUsers = async () => {
    try {
      const response = await api.get('/users/online-users');
      console.log('Fetched online users:', response.data);
      setOnlineUsers(new Set(response.data));
    } catch (error) {
      console.error('Error fetching online users:', error);
    }
  };

  // ===== RENDER UI =====
  
  return (
    <div className="d-flex flex-column vh-100">
      {/* User Dashboard - Top */}
      <UserDashboard 
        username={username} 
        isOnline={isOnline} 
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
              onUserSelect={(user) => {
                setSelectedUser(user); // Set selected user
                setReceiver(user.username); // Set message receiver
                setTab(user.username); // Switch to user's chat tab
                if (user.username) {
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
              selectedUser={selectedUser}
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
    </div>
  );
};