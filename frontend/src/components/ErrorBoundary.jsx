import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error: error,
      errorInfo: errorInfo
    });
    
    // Log error to console or external service
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
          <div className="w-100" style={{ maxWidth: "400px" }}>
            <div className="bg-white shadow rounded p-4">
              <div className="d-flex align-items-center justify-content-center mx-auto bg-danger bg-opacity-10 rounded-circle" style={{ width: "48px", height: "48px" }}>
                <svg className="text-danger" fill="none" stroke="currentColor" viewBox="0 0 24 24" style={{ width: "24px", height: "24px" }}>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <div className="mt-4 text-center">
                <h3 className="h5 fw-medium text-dark">Something went wrong</h3>
                <p className="mt-2 small text-muted">
                  We're sorry, but something unexpected happened. Please try refreshing the page.
                </p>
                <button
                  onClick={() => window.location.reload()}
                  className="btn btn-primary mt-4 px-4 py-2"
                >
                  Refresh Page
                </button>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary; 