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
      console.error('WebSocket connection error:', error);
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
      console.log('Sending public message:', chatMessage);
      try {
        stompClient.current.send('/app/message', {}, JSON.stringify(chatMessage));
        console.log('Public message sent successfully');
      } catch (error) {
        console.error('Error sending public message:', error);
      }
    } else {
      console.error('Cannot send message: WebSocket not connected');
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
      console.log('Sending private message:', chatMessage);
      try {
        stompClient.current.send('/app/private-message', {}, JSON.stringify(chatMessage));
        console.log('Private message sent successfully');
      } catch (error) {
        console.error('Error sending private message:', error);
      }
    } else {
      console.error('Cannot send message: WebSocket not connected');
    }
  }, [username, connected]);

  const handlePublicMessage = useCallback((payloadData) => {
    console.log('Processing public message:', payloadData);
    console.log('Message status:', payloadData.status);
    console.log('Message sender:', payloadData.senderName);
    console.log('Message content:', payloadData.message);
    
    switch (payloadData.status) {
      case 'JOIN':
        console.log('User joined:', payloadData.senderName);
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
        console.log('User left:', payloadData.senderName);
        if (payloadData.senderName !== username) {
          setPrivateChats(prev => {
            const newChats = new Map(prev);
            newChats.delete(payloadData.senderName);
            return newChats;
          });
        }
        break;
      case 'MESSAGE':
        console.log('Adding public message to chat');
        setPublicChats(prev => {
          console.log('Previous public chats count:', prev.length);
          const newChats = [...prev, payloadData];
          console.log('New public chats count:', newChats.length);
          return newChats;
        });
        break;
      default:
        console.warn('Unknown status received:', payloadData.status);
    }
  }, [username]);

  const handlePrivateMessage = useCallback((payloadData) => {
    console.log('Processing private message:', payloadData);
    console.log('Message sender:', payloadData.senderName);
    console.log('Message receiver:', payloadData.receiverName);
    console.log('Message content:', payloadData.message);
    
    setPrivateChats(prev => {
      const newChats = new Map(prev);
      const userChats = newChats.get(payloadData.senderName) || [];
      console.log('Previous messages for', payloadData.senderName + ':', userChats.length);
      const updatedChats = [...userChats, payloadData];
      console.log('Updated messages for', payloadData.senderName + ':', updatedChats.length);
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