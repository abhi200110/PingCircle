/**
 * Frontend Logging Utility
 * 
 * Provides structured logging with different levels and environment-based filtering.
 * Replaces console.log statements with proper logging that can be controlled
 * based on environment and log levels.
 */

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

/**
 * Check if a log level should be displayed
 * @param {number} level - The log level to check
 * @returns {boolean} - Whether the level should be logged
 */
const shouldLog = (level) => {
  return level <= currentLogLevel;
};

/**
 * Format log message with timestamp and level
 * @param {string} level - Log level name
 * @param {string} message - Log message
 * @param {any} data - Additional data to log
 * @returns {string} - Formatted log message
 */
const formatMessage = (level, message, data = null) => {
  const timestamp = new Date().toISOString();
  const prefix = `[${timestamp}] [${level}]`;
  
  if (data) {
    return `${prefix} ${message}`, data;
  }
  return `${prefix} ${message}`;
};

/**
 * Logger class with different log levels
 */
class Logger {
  /**
   * Log error messages
   * @param {string} message - Error message
   * @param {any} data - Additional error data
   */
  error(message, data = null) {
    if (shouldLog(LOG_LEVELS.ERROR)) {
      console.error(formatMessage('ERROR', message, data));
    }
  }

  /**
   * Log warning messages
   * @param {string} message - Warning message
   * @param {any} data - Additional warning data
   */
  warn(message, data = null) {
    if (shouldLog(LOG_LEVELS.WARN)) {
      console.warn(formatMessage('WARN', message, data));
    }
  }

  /**
   * Log info messages
   * @param {string} message - Info message
   * @param {any} data - Additional info data
   */
  info(message, data = null) {
    if (shouldLog(LOG_LEVELS.INFO)) {
      console.info(formatMessage('INFO', message, data));
    }
  }

  /**
   * Log debug messages (only in development)
   * @param {string} message - Debug message
   * @param {any} data - Additional debug data
   */
  debug(message, data = null) {
    if (shouldLog(LOG_LEVELS.DEBUG)) {
      console.log(formatMessage('DEBUG', message, data));
    }
  }

  /**
   * Log WebSocket specific messages
   * @param {string} message - WebSocket message
   * @param {any} data - WebSocket data
   */
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
