import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import SockJS from "sockjs-client"; // WebSocket client library
import { over } from "stompjs"; // STOMP protocol for WebSocket messaging
import SearchBar from "../components/other/SearchBar"; // Component to search for users
import PinnedUsers from "../components/PinnedUsers"; // Component to display pinned users
import UserChatItem from "../components/UserChatItem"; // Component for individual user chat items
import api from "../config/axios"; // Configured axios instance with JWT authentication

// Global WebSocket client variable
var stompClient = null;

export const ChatPage2 = () => {
  // ===== STATE MANAGEMENT =====
  
  // User selection and chat state
  const [selectedUser, setSelectedUser] = useState(null); // Currently selected user to chat with
  const [receiver, setReceiver] = useState(""); // Username of the person receiving messages
  const [message, setMessage] = useState(""); // Current message being typed
  const [media, setMedia] = useState(""); // File attachment (base64 encoded) like images ,videos
  const [tab, setTab] = useState("CHATROOM"); // Current active tab (CHATROOM or username)
  
  // Chat data storage
  const [publicChats, setPublicChats] = useState([]); // Array of public chat messages
  const [privateChats, setPrivateChats] = useState(new Map()); // Map of private chats: {username (key): [messages](value)}
  const [username] = useState(localStorage.getItem("chat-username")); // Current logged-in user fetched from local storage
  const [pinnedUsers, setPinnedUsers] = useState([]); // Array of pinned usernames
  const [allUsers, setAllUsers] = useState([]); // All available users (currently unused)
  
  // Navigation and refs
  const navigate = useNavigate(); // React Router navigation hook
  const connected = useRef(false); // Ref to track WebSocket connection status (avoid duplicate connections)
  const chatContainerRef = useRef(null); // Ref to chat container for auto-scroll

  // ===== AUTHENTICATION CHECK =====
  // Redirect to login if no username is stored
  if (!username.trim()) {
    navigate("/login");
  }

  // ===== WEB SOCKET CONNECTION MANAGEMENT =====
  // Initialize WebSocket connection when component mounts
  useEffect(() => {
    if (!connected.current) {
      connect(); // Establish WebSocket connection
    }
    // Cleanup: disconnect WebSocket when component unmounts (like a logout or page change).
    return () => {
      if (stompClient) {
        stompClient.disconnect();
        connected.current = false;
      }
    };
  }, []);

  // ===== USER SELECTION HANDLER =====
  // Called when a user is selected from search or pinned users to startchat with.
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
    const payloadData = JSON.parse(payload.body);
    console.log("Public message received:", payloadData);
    switch (payloadData.status) {
      case "JOIN":
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
        // Add public message to public chat
        setPublicChats((prev) => [...prev, payloadData]);
        break;
      default:
        console.warn("Unknown status received:", payloadData.status);
    }
  };

  // Handle private messages from other users
  const onPrivateMessage = (payload) => {
    const payloadData = JSON.parse(payload.body);
    console.log("Private message received:", payloadData);
    
    // Add the received message to the sender's chat history
    setPrivateChats((prevChats) => {
      const newChats = new Map(prevChats);
      const existingMessages = newChats.get(payloadData.senderName) || [];
      newChats.set(payloadData.senderName, [...existingMessages, payloadData]); // Append new message
      return newChats;
    });
  };

  // ===== WEB SOCKET CONNECTION FUNCTIONS =====
  
  // Called when WebSocket connection is established
  const onConnect = () => {
    console.log("Connected to WebSocket");
    connected.current = true;

    // Subscribe to public chat channel
    stompClient.subscribe("/chatroom/public", onMessageReceived);
    // Subscribe to private messages for this user
    stompClient.subscribe(`/user/${username}/private`, onPrivateMessage);

    userJoin(); // Send join message to public chat
  };

  // Called when WebSocket connection fails
  const onError = (err) => {
    console.error("WebSocket connection error:", err);
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

  // ===== FILE HANDLING FUNCTIONS =====
  
  // Handle file selection and conversion to base64
  const base64ConversionForImages = (e) => {
    if (e.target.files[0]) {
      getBase64(e.target.files[0]); // Convert selected file to base64
    }
  };

  // Convert file to base64 string for sending via WebSocket
  const getBase64 = (file) => {
    let reader = new FileReader();
    reader.readAsDataURL(file); // Read file as data URL (base64)
    reader.onload = () => setMedia(reader.result); // Store base64 string in state
    reader.onerror = (error) => console.error("Error converting file:", error);
  };

  // ===== MESSAGE SENDING FUNCTIONS =====
  
  // Send message to public chat room
  const sendMessage = () => {
    if (message.trim().length > 0 || media) {
      stompClient.send(
        "/app/message", // Public chat endpoint
        {},
        JSON.stringify({
          senderName: username,
          status: "MESSAGE",
          media: media, // Base64 encoded file
          message: message,
        })
      );
      setMessage(""); // Clear message input
      setMedia(""); // Clear media attachment
    }
  };

  // Send private message to specific user
  const sendPrivate = () => {
    if (message.trim().length > 0 && receiver) {
      let chatMessage = {
        senderName: username,
        receiverName: receiver, // Target user
        message: message,
        media: media, // Base64 encoded file
        status: "MESSAGE",
        timestamp: Date.now(), // Add timestamp for proper ordering
      };

      // Add message to local state immediately for instant feedback
      setPrivateChats((prevChats) => {
        const newChats = new Map(prevChats);
        const existingMessages = newChats.get(receiver) || [];
        newChats.set(receiver, [...existingMessages, chatMessage]); // Append new message
        return newChats;
      });

      // Send message via WebSocket to backend
      stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));

      setMessage(""); // Clear message input
      setMedia(""); // Clear media attachment
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
        setPrivateChats((prevChats) => {
          const newChats = new Map(prevChats);
          newChats.set(user2, messages); // Update chat history for this user
          return newChats;
        });
        console.log(`Chat history loaded for ${user2}:`, messages.length, 'messages');
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

  // ===== RENDER UI =====
  
  return (
    <div className="flex items-center justify-center h-screen w-[100%]">
      <div className="flex w-full h-full">
        {/* Left Sidebar - User List and Navigation */}
        <div className="flex flex-col w-[300px] h-full bg-gray-50 border-r border-gray-200">
          {/* Sidebar Header - Title and Search */}
          <div className="p-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-800">Chats</h2>
            <SearchBar onUserSelect={handlePrivateMessage} /> {/* Search for users */}
          </div>

          {/* Sidebar Content - Scrollable Area */}
          <div className="flex-1 overflow-y-auto">
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
            <div className="p-4 border-t border-gray-200">
              <div
                className={`p-3 rounded-lg cursor-pointer transition-colors ${
                  tab === "CHATROOM" 
                    ? "bg-blue-500 text-white" // Active state
                    : "bg-gray-100 hover:bg-gray-200 text-gray-800" // Inactive state
                }`}
                onClick={() => setTab("CHATROOM")} // Switch to public chat
              >
                <div className="flex items-center space-x-2">
                  <span className="text-lg">💬</span>
                  <span className="font-medium">Chat Room</span>
                </div>
              </div>
            </div>

            {/* Recent Chats Section - List of users with recent conversations */}
            <div className="p-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Recent Chats</h3>
              <div className="space-y-2">
                {/* Map through all users in privateChats to show recent conversations */}
                {[...privateChats.keys()].map((name, index) => (
                  <UserChatItem
                    key={index}
                    user={{ username: name }}
                    currentUser={username}
                    isSelected={tab === name} // Highlight if this user is selected
                    onUserSelect={(user) => {
                      tabReceiverSet(user.username); // Switch to user's chat
                      fetchChatHistory(username, user.username); // Load chat history
                    }}
                    onPinChange={handlePinChange} // Handle pin/unpin actions
                  />
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Right Side - Chat Area */}
        <div className="flex flex-col flex-1 mt-3">
          {/* Chat Messages Display Area */}
          <div
            ref={chatContainerRef} // Ref for auto-scrolling
            className="p-3 flex-grow overflow-auto bg-gray-300 border border-green-500 flex flex-col space-y-2 rounded-md"
            style={{ height: "500px" }}
          >
            {/* Conditional Rendering: Public Chat vs Private Chat */}
            {tab === "CHATROOM"
              ? // Render Public Chat Messages
                publicChats.map((message, index) => (
                  <div
                    className={`flex ${
                      message.senderName !== username
                        ? "justify-start" // Other user's message (left side)
                        : "justify-end" // Current user's message (right side)
                    }`}
                    key={index}
                  >
                    <div
                      className={`p-2 flex flex-col max-w-lg ${
                        message.senderName !== username
                          ? "bg-white rounded-t-lg rounded-r-lg" // Other user's message style
                          : "bg-blue-500 rounded-t-lg rounded-l-lg" // Current user's message style
                      }`}
                    >
                      {/* Show sender name for other users' messages */}
                      {message.senderName !== username && (
                        <div className="rounded bg-blue-400 mb-2 p-1 text-white">
                          {message.senderName}
                        </div>
                      )}
                      {/* Message text */}
                      <div
                        className={
                          message.senderName === username ? "text-white" : ""
                        }
                      >
                        {message.message}
                      </div>
                      {/* Display image attachments */}
                      {message.media &&
                        message.media
                          .split(";")[0]
                          .split("/")[0]
                          .split(":")[1] === "image" && (
                          <img src={message.media} alt="" width={"250px"} />
                        )}
                      {/* Display video attachments */}
                      {message.media &&
                        message.media
                          .split(";")[0]
                          .split("/")[0]
                          .split(":")[1] === "video" && (
                          <video width="320" height="240" controls>
                            <source src={message.media} type="video/mp4" />
                          </video>
                        )}
                    </div>
                  </div>
                ))
              : // Render Private Chat Messages
                privateChats.get(tab).map((message, index) => (
                  <div
                    className={`flex ${
                      message.senderName !== username
                        ? "justify-start" // Other user's message (left side)
                        : "justify-end" // Current user's message (right side)
                    }`}
                    key={index}
                  >
                    <div
                      className={`p-2 flex flex-col max-w-lg ${
                        message.senderName !== username
                          ? "bg-white rounded-t-lg rounded-r-lg" // Other user's message style
                          : "bg-blue-500 rounded-t-lg rounded-l-lg" // Current user's message style
                      }`}
                    >
                      {/* Message text */}
                      <div
                        className={
                          message.senderName === username ? "text-white" : ""
                        }
                      >
                        {message.message}
                      </div>
                      {/* Display image attachments */}
                      {message.media &&
                        message.media
                          .split(";")[0]
                          .split("/")[0]
                          .split(":")[1] === "image" && (
                          <img src={message.media} alt="" width={"250px"} />
                        )}
                      {/* Display video attachments */}
                      {message.media &&
                        message.media
                          .split(";")[0]
                          .split("/")[0]
                          .split(":")[1] === "video" && (
                          <video width="320" height="240" controls>
                            <source src={message.media} type="video/mp4" />
                          </video>
                        )}
                    </div>
                  </div>
                ))}
          </div>

          {/* Message Input Area - Bottom of chat */}
          <div className="flex items-center p-2">
            {/* Text input for typing messages */}
            <input
              className="flex-grow p-2 border outline-blue-600 rounded-l-lg"
              type="text"
              placeholder="Message"
              value={message}
              onKeyUp={(e) => {
                if (e.key === "Enter" || e.key === 13) {
                  tab === "CHATROOM" ? sendMessage() : sendPrivate(); // Send on Enter key
                }
              }}
              onChange={(e) => setMessage(e.target.value)} // Update message state
            />
            {/* File attachment button (paperclip icon) */}
            <label
              htmlFor="file"
              className="p-2 bg-blue-700 text-white rounded-r-none cursor-pointer"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="24"
                fill="currentColor"
                className="bi bi-paperclip"
                viewBox="0 0 16 16"
              >
                <path d="M4.5 3a2.5 2.5 0 0 1 5 0v9a1.5 1.5 0 0 1-3 0V5a.5.5 0 0 1 1 0v7a.5.5 0 0 0 1 0V3a1.5 1.5 0 1 0-3 0v9a2.5 2.5 0 0 0 5 0V5a.5.5 0 0 1 1 0v7a3.5 3.5 0 1 1-7 0V3z" />
              </svg>
            </label>
            {/* Hidden file input for selecting files */}
            <input
              id="file"
              type="file"
              onChange={(e) => base64ConversionForImages(e)} // Handle file selection
              className="hidden"
            />
            {/* Send button - sends to public or private chat based on current tab */}
            <input
              type="button"
              className="ml-2 p-2 bg-blue-700 text-white rounded cursor-pointer"
              value="Send"
              onClick={tab === "CHATROOM" ? sendMessage : sendPrivate}
            />
            {/* Logout button */}
            <input
              type="button"
              className="ml-2 p-2 bg-blue-700 text-white rounded cursor-pointer"
              value="Logout"
              onClick={handleLogout}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
