/**
 * Shared logger utility for the PingCircle frontend application
 * Provides structured logging with different levels and categories
 * Can be imported by any component in the application
 */

class Logger {
  constructor() {
    this.isDevelopment = process.env.NODE_ENV === 'development';
    this.isProduction = process.env.NODE_ENV === 'production';
  }

  /**
   * Log a message with INFO level
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  info(message, data = {}) {
    if (this.isDevelopment) {
      console.log(`[INFO] ${message}`, data);
    }
  }

  /**
   * Log a message with DEBUG level
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  debug(message, data = {}) {
    if (this.isDevelopment) {
      console.log(`[DEBUG] ${message}`, data);
    }
  }

  /**
   * Log a message with WARN level
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  warn(message, data = {}) {
    console.warn(`[WARN] ${message}`, data);
  }

  /**
   * Log a message with ERROR level
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  error(message, data = {}) {
    console.error(`[ERROR] ${message}`, data);
  }

  /**
   * Log WebSocket related messages
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  websocket(message, data = {}) {
    if (this.isDevelopment) {
      console.log(`[WEBSOCKET] ${message}`, data);
    }
  }

  /**
   * Log API related messages
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  api(message, data = {}) {
    if (this.isDevelopment) {
      console.log(`[API] ${message}`, data);
    }
  }

  /**
   * Log chat related messages
   * @param {string} message - The message to log
   * @param {Object} data - Additional data to include in the log
   */
  chat(message, data = {}) {
    if (this.isDevelopment) {
      console.log(`[CHAT] ${message}`, data);
    }
  }
}

// Create and export a singleton instance
const logger = new Logger();
export default logger;
