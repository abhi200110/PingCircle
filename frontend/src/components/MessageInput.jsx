import React, { useState, useRef } from 'react';
import EmojiPicker from 'emoji-picker-react';

const MessageInput = ({ message, setMessage, onSendMessage, disabled = false }) => {
  const [isTyping, setIsTyping] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const emojiPickerRef = useRef(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (message.trim() && !disabled) {
      onSendMessage(message);
      setMessage('');
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleTextareaChange = (e) => {
    setMessage(e.target.value);
    // Close emoji picker when user starts typing
    if (showEmojiPicker) {
      setShowEmojiPicker(false);
    }
  };

  const onEmojiClick = (emojiObject) => {
    setMessage(prev => prev + emojiObject.emoji);
  };

  const toggleEmojiPicker = () => {
    setShowEmojiPicker(!showEmojiPicker);
  };

  // Close emoji picker when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event) => {
      if (emojiPickerRef.current && !emojiPickerRef.current.contains(event.target)) {
        setShowEmojiPicker(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  return (
    <div className="border-top border-secondary p-4 message-input-container">
      <form onSubmit={handleSubmit} className="d-flex align-items-end gap-2">
        <div className="flex-grow-1">
          <textarea
            value={message}
            onChange={handleTextareaChange}
            onKeyPress={handleKeyPress}
            placeholder="Type a message..."
            className="form-control"
            rows="2"
            disabled={disabled}
            style={{ resize: 'none', minHeight: '60px', maxHeight: '120px' }}
          />
        </div>
        
        <div className="position-relative" ref={emojiPickerRef}>
          <button
            type="button"
            onClick={toggleEmojiPicker}
            className="btn btn-link text-muted p-2"
            disabled={disabled}
            title="Add emoji"
          >
            😊
          </button>
          {showEmojiPicker && (
            <div 
              className="position-absolute"
              style={{
                bottom: '100%',
                right: '0',
                zIndex: 1000,
                marginBottom: '10px'
              }}
            >
              <EmojiPicker
                onEmojiClick={onEmojiClick}
                width={350}
                height={400}
                searchPlaceholder="Search emoji..."
                previewConfig={{
                  showPreview: false
                }}
              />
            </div>
          )}
        </div>
        
        <button
          type="submit"
          disabled={!message.trim() || disabled}
          className="btn btn-primary px-4 py-2"
        >
          Send
        </button>
      </form>
    </div>
  );
};

export default MessageInput; 