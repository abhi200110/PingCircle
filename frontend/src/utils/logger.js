
const LOG_LEVELS = {
  ERROR: 0,
  WARN: 1,
  INFO: 2,
  DEBUG: 3
};

// Get log level from environment or default to INFO
const getLogLevel = () => {
  const envLevel = import.meta.env.VITE_LOG_LEVEL;
  if (envLevel && LOG_LEVELS[envLevel.toUpperCase()] !== undefined) {
    return LOG_LEVELS[envLevel.toUpperCase()];
  }
  return import.meta.env.DEV ? LOG_LEVELS.DEBUG : LOG_LEVELS.INFO;
};

const currentLogLevel = getLogLevel();


const shouldLog = (level) => {
  return level <= currentLogLevel;
};


const formatMessage = (level, message, data = null) => {
  const timestamp = new Date().toISOString();
  const prefix = `[${timestamp}] [${level}]`;
  
  if (data) {
    return `${prefix} ${message}`, data;
  }
  return `${prefix} ${message}`;
};


class Logger {
  
  error(message, data = null) {
    if (shouldLog(LOG_LEVELS.ERROR)) {
      console.error(formatMessage('ERROR', message, data));
    }
  }


  warn(message, data = null) {
    if (shouldLog(LOG_LEVELS.WARN)) {
      console.warn(formatMessage('WARN', message, data));
    }
  }


  info(message, data = null) {
    if (shouldLog(LOG_LEVELS.INFO)) {
      console.info(formatMessage('INFO', message, data));
    }
  }

  debug(message, data = null) {
    if (shouldLog(LOG_LEVELS.DEBUG)) {
      console.log(formatMessage('DEBUG', message, data));
    }
  }


  websocket(message, data = null) {
    if (shouldLog(LOG_LEVELS.DEBUG)) {
      console.log(formatMessage('WEBSOCKET', message, data));
    }
  }

  /**
   * Log API specific messages
   * @param {string} message - API message
   * @param {any} data - API data
   */
  api(message, data = null) {
    if (shouldLog(LOG_LEVELS.DEBUG)) {
      console.log(formatMessage('API', message, data));
    }
  }

  /**
   * Log chat specific messages
   * @param {string} message - Chat message
   * @param {any} data - Chat data
   */
  chat(message, data = null) {
    if (shouldLog(LOG_LEVELS.DEBUG)) {
      console.log(formatMessage('CHAT', message, data));
    }
  }
}

// Create and export logger instance
const logger = new Logger();

export default logger;
