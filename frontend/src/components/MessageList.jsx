import React, { useEffect, useRef } from 'react';
import { format } from 'date-fns';

const MessageList = ({ messages, currentUser, onMessageRead }) => {
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const formatTime = (timestamp) => {
    return format(new Date(timestamp), 'HH:mm');
  };

  const isOwnMessage = (senderName) => {
    return senderName === currentUser;
  };

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-4">
      {messages.map((message, index) => (
        <div
          key={index}
          className={`flex ${isOwnMessage(message.senderName) ? 'justify-end' : 'justify-start'}`}
        >
          <div
            className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
              isOwnMessage(message.senderName)
                ? 'bg-blue-500 text-white'
                : 'bg-gray-200 text-gray-800'
            }`}
          >
            {!isOwnMessage(message.senderName) && (
              <div className="text-xs text-gray-600 mb-1">
                {message.senderName}
              </div>
            )}
            
            {message.media && message.mediaType?.startsWith('image/') ? (
              <img
                src={message.media}
                alt="Message media"
                className="max-w-full h-auto rounded"
                onClick={() => window.open(message.media, '_blank')}
              />
            ) : (
              <div className="break-words">{message.message}</div>
            )}
            
            <div className={`text-xs mt-1 ${
              isOwnMessage(message.senderName) ? 'text-blue-100' : 'text-gray-500'
            }`}>
              {formatTime(message.timestamp)}
              {isOwnMessage(message.senderName) && (
                <span className="ml-2">
                  {message.status === 'READ' ? '✓✓' : '✓'}
                </span>
              )}
            </div>
          </div>
        </div>
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
};

export default MessageList; 