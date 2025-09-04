import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { over } from 'stompjs';

const useWebSocket = (username) => {
  const [connected, setConnected] = useState(false);
  const [publicChats, setPublicChats] = useState([]);
  const [privateChats, setPrivateChats] = useState(new Map());
  const stompClient = useRef(null);

  const connect = useCallback(() => {
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient.current = over(socket);
    
    stompClient.current.connect({}, () => {
      setConnected(true);
      
      // Subscribe to public chat
      stompClient.current.subscribe('/chatroom/public', (payload) => {
        const payloadData = JSON.parse(payload.body);
        handlePublicMessage(payloadData);
      });
      
      // Subscribe to private messages
      stompClient.current.subscribe(`/user/${username}/private`, (payload) => {
        const payloadData = JSON.parse(payload.body);
        handlePrivateMessage(payloadData);
      });
      
      // Join the chat
      userJoin();
    }, (error) => {
      setConnected(false);
    });
  }, [username]);

  const disconnect = useCallback(() => {
    if (stompClient.current) {
      userLeft();
      stompClient.current.disconnect();
      setConnected(false);
    }
  }, []);

  const userJoin = useCallback(() => {
    const chatMessage = {
      senderName: username,
      status: 'JOIN'
    };
    stompClient.current?.send('/app/message', {}, JSON.stringify(chatMessage));
  }, [username]);

  const userLeft = useCallback(() => {
    const chatMessage = {
      senderName: username,
      status: 'LEAVE'
    };
    stompClient.current?.send('/app/message', {}, JSON.stringify(chatMessage));
  }, [username]);

  const sendPublicMessage = useCallback((message, media = '', mediaType = '') => {
    if (stompClient.current && connected) {
      const chatMessage = {
        senderName: username,
        message: message,
        media: media,
        mediaType: mediaType,
        status: 'MESSAGE'
      };
      try {
        stompClient.current.send('/app/message', {}, JSON.stringify(chatMessage));
      } catch (error) {
        // Error sending public message
      }
    } else {
      // Cannot send message: WebSocket not connected
    }
  }, [username, connected]);

  const sendPrivateMessage = useCallback((receiverName, message, media = '', mediaType = '') => {
    if (stompClient.current && connected) {
      const chatMessage = {
        senderName: username,
        receiverName: receiverName,
        message: message,
        media: media,
        mediaType: mediaType,
        status: 'MESSAGE'
      };
      try {
        stompClient.current.send('/app/private-message', {}, JSON.stringify(chatMessage));
      } catch (error) {
        // Error sending private message
      }
    } else {
      // Cannot send message: WebSocket not connected
    }
  }, [username, connected]);

  const handlePublicMessage = useCallback((payloadData) => {
    
    switch (payloadData.status) {
      case 'JOIN':
        if (payloadData.senderName !== username) {
          setPrivateChats(prev => {
            const newChats = new Map(prev);
            if (!newChats.has(payloadData.senderName)) {
              newChats.set(payloadData.senderName, []);
            }
            return newChats;
          });
        }
        break;
      case 'LEAVE':
        if (payloadData.senderName !== username) {
          setPrivateChats(prev => {
            const newChats = new Map(prev);
            newChats.delete(payloadData.senderName);
            return newChats;
          });
        }
        break;
      case 'MESSAGE':
        setPublicChats(prev => {
          const newChats = [...prev, payloadData];
          return newChats;
        });
        break;
      default:
    }
  }, [username]);

  const handlePrivateMessage = useCallback((payloadData) => {
    
    setPrivateChats(prev => {
      const newChats = new Map(prev);
      const userChats = newChats.get(payloadData.senderName) || [];
      const updatedChats = [...userChats, payloadData];
      newChats.set(payloadData.senderName, updatedChats);
      return newChats;
    });
  }, []);

  useEffect(() => {
    if (username && !connected) {
      connect();
    }
    
    return () => {
      disconnect();
    };
  }, [username, connect, disconnect, connected]);

  return {
    connected,
    publicChats,
    privateChats,
    sendPublicMessage,
    sendPrivateMessage,
    disconnect
  };
};

export default useWebSocket; 