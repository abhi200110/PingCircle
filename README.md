# PingCircle 💬

A real-time chat application built with Spring Boot backend and React frontend, featuring WebSocket communication, JWT authentication, and modern UI design.

![PingCircle Logo](https://img.shields.io/badge/PingCircle-Chat%20App-blue?style=for-the-badge&logo=chat)

## 🌟 Features

### 🔐 Authentication & Security
- **JWT-based authentication** with secure token management
- **User registration and login** with password hashing (BCrypt)
- **Automatic login** after successful registration
- **Token validation** for protected endpoints

### 💬 Real-time Messaging
- **WebSocket communication** using STOMP protocol
- **Public chat room** for group conversations
- **Private messaging** between individual users
- **Real-time message delivery** with instant notifications
- **Message history** persistence in database

### 👥 User Management
- **User search** functionality to find other users
- **Contact list** based on chat history
- **Pin/unpin users** for quick access to favorite contacts
- **Online user tracking** with real-time status updates
- **User profiles** with customizable information

### 📅 Advanced Features
- **Scheduled messaging** - send messages at specific times
- **Message scheduling interface** with date/time picker
- **Scheduled messages list** to view and manage pending messages
- **Message deletion** for both public and private chats
- **Unread message indicators** for better user experience

### 🎨 Modern UI/UX
- **Glass morphism design** with backdrop blur effects
- **Responsive layout** that works on desktop and mobile
- **Dark/light theme** with beautiful gradients
- **Emoji picker** for expressive messaging
- **Loading states** and error handling
- **Smooth animations** and transitions

## 🛠️ Tech Stack

### Backend
- **Java 17** - Core programming language
- **Spring Boot 3.x** - Main framework
- **Spring Security** - Authentication and authorization
- **Spring WebSocket** - Real-time communication
- **JPA/Hibernate** - Database ORM
- **MySQL/PostgreSQL** - Database (configurable)
- **JWT** - Token-based authentication
- **Maven** - Dependency management

### Frontend
- **React 18** - UI framework
- **Vite** - Build tool and development server
- **Bootstrap 5** - CSS framework
- **SockJS** - WebSocket client
- **StompJS** - STOMP protocol implementation
- **Axios** - HTTP client
- **React Router** - Client-side routing

### Development Tools
- **Git** - Version control
- **Maven** - Java build tool
- **npm** - Node.js package manager
- **ESLint** - Code linting
- **Prettier** - Code formatting

## 📋 Prerequisites

Before running this application, make sure you have the following installed:

- **Java 17** or higher
- **Node.js 16** or higher
- **npm** or **yarn**
- **MySQL** or **PostgreSQL** database
- **Git**

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/PingCircle.git
cd PingCircle
```

### 2. Backend Setup

#### Database Configuration
1. Create a MySQL/PostgreSQL database
2. Update `backend/src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/pingcircle
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT Configuration
jwt.secret=your_jwt_secret_key_here
jwt.expiration=86400000
```

#### Run the Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

### 4. Access the Application

Open your browser and navigate to `http://localhost:5173`

## 📁 Project Structure

```
PingCircle/
├── backend/                          # Spring Boot backend
│   ├── src/main/java/com/pingcircle/
│   │   ├── configuration/            # Security and WebSocket config
│   │   ├── controller/               # REST API controllers
│   │   ├── entity/                   # Database entities
│   │   ├── model/                    # DTOs and request/response models
│   │   ├── repository/               # Data access layer
│   │   ├── service/                  # Business logic layer
│   │   └── PingCircleApplication.java
│   ├── src/main/resources/
│   │   └── application.properties    # Configuration file
│   └── pom.xml                       # Maven dependencies
├── frontend/                         # React frontend
│   ├── src/
│   │   ├── components/               # React components
│   │   ├── Layout/                   # Page layouts
│   │   ├── config/                   # Configuration files
│   │   ├── hooks/                    # Custom React hooks
│   │   └── utils/                    # Utility functions
│   ├── public/                       # Static assets
│   └── package.json                  # Node.js dependencies
└── README.md
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the frontend directory:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

### JWT Configuration

Update the JWT secret in `application.properties`:

```properties
jwt.secret=your_very_secure_jwt_secret_key_here
jwt.expiration=86400000
```

## 📚 API Documentation

### Authentication Endpoints

- `POST /api/users/signup` - User registration
- `POST /api/users/login` - User login

### User Management Endpoints

- `GET /api/users/search` - Search users
- `GET /api/users/contacts` - Get user contacts
- `GET /api/users/online-users` - Get online users
- `GET /api/users/pinned-users` - Get pinned users
- `POST /api/users/pin-user` - Pin/unpin user
- `DELETE /api/users/delete-account` - Delete account

### Chat Endpoints

- `GET /api/chat/public` - Get public chat history
- `GET /api/chat/private/{username}` - Get private chat history
- `POST /api/chat/schedule` - Schedule a message
- `DELETE /api/chat/delete/{username}` - Delete conversation

## 🎯 Usage Guide

### Getting Started

1. **Register a new account** or **login** with existing credentials
2. **Explore the public chat room** to see group conversations
3. **Search for users** using the search bar
4. **Start private conversations** by clicking on user names
5. **Pin favorite users** for quick access
6. **Schedule messages** for future delivery

### Features Walkthrough

#### Real-time Chat
- Messages appear instantly in both public and private chats
- Online user status is updated in real-time
- Unread message indicators show new messages

#### User Management
- Search for users by username or name
- Pin frequently contacted users
- View online status of all users

#### Scheduled Messaging
- Click the schedule button to set future message delivery
- Choose date and time for message delivery
- View and manage all scheduled messages

## 🐛 Troubleshooting

### Common Issues

1. **Backend won't start**
   - Check database connection settings
   - Ensure Java 17+ is installed
   - Verify Maven dependencies are downloaded

2. **Frontend build errors**
   - Clear node_modules and reinstall: `rm -rf node_modules && npm install`
   - Check Node.js version compatibility

3. **WebSocket connection issues**
   - Verify backend is running on port 8080
   - Check CORS configuration
   - Ensure firewall allows WebSocket connections

4. **JWT authentication errors**
   - Verify JWT secret is properly configured
   - Check token expiration settings
   - Clear browser localStorage if needed

### Logs and Debugging

Backend logs are available in the console where you run the Spring Boot application. Frontend logs can be viewed in the browser's developer console.

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow Java coding conventions for backend
- Use ESLint and Prettier for frontend code formatting
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React team for the amazing UI library
- Bootstrap team for the responsive CSS framework
- All contributors and users of PingCircle

## 📞 Support

If you encounter any issues or have questions:

- Create an issue on GitHub
- Check the troubleshooting section above
- Review the API documentation

---

**Made with ❤️ by the PingCircle Team**

[![GitHub stars](https://img.shields.io/github/stars/yourusername/PingCircle?style=social)](https://github.com/yourusername/PingCircle/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/yourusername/PingCircle?style=social)](https://github.com/yourusername/PingCircle/network)
[![GitHub issues](https://img.shields.io/github/issues/yourusername/PingCircle)](https://github.com/yourusername/PingCircle/issues)
