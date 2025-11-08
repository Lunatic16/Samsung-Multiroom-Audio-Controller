#!/bin/bash

# Multiroom Audio Application Control Script
# Author: Qwen Assistant
# Description: Script to start/stop the Multiroom Audio Application server

set -e

# Configuration
APP_NAME="multiroom-app"
APP_DIR="/home/god2/Downloads/Wireless Audio-Multiroom 4156/multiroom-app/backend"
JAR_FILE="$APP_DIR/target/multiroom-app-1.0.0.jar"
LOG_FILE="$APP_DIR/multiroom-app.log"
PID_FILE="$APP_DIR/multiroom-app.pid"
PORT=8080

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if application is running
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    else
        return 1
    fi
}

# Function to start the application
start_app() {
    if is_running; then
        print_error "Application is already running (PID: $(cat $PID_FILE))"
        exit 1
    fi

    print_status "Starting $APP_NAME..."

    # Check if JAR file exists, if not build it
    if [ ! -f "$JAR_FILE" ]; then
        print_status "JAR file not found. Building application..."
        cd "$APP_DIR"
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            print_error "Failed to build application"
            exit 1
        fi
    fi

    # Start the application in background
    cd "$APP_DIR"
    nohup mvn spring-boot:run > "$LOG_FILE" 2>&1 &
    PID=$!
    
    # Write PID to file
    echo $PID > "$PID_FILE"
    
    print_status "$APP_NAME started successfully with PID: $PID"
    print_status "Check logs at: $LOG_FILE"
    print_status "Application will be available at: http://localhost:$PORT"
    
    # Wait a few seconds to ensure the app starts
    sleep 10
    
    # Verify that the app is actually running
    if is_running; then
        print_status "$APP_NAME is running and accessible"
    else
        print_error "$APP_NAME failed to start properly. Check logs at: $LOG_FILE"
        exit 1
    fi
}

# Function to stop the application
stop_app() {
    if ! is_running; then
        print_warning "$APP_NAME is not running"
        return 0
    fi

    PID=$(cat "$PID_FILE")
    print_status "Stopping $APP_NAME (PID: $PID)..."

    # Try graceful shutdown first
    kill -TERM "$PID"
    
    # Wait for graceful shutdown
    sleep 5
    
    # Check if process is still running
    if ps -p "$PID" > /dev/null 2>&1; then
        print_warning "Application did not stop gracefully. Forcing shutdown..."
        kill -KILL "$PID"
        
        # Wait briefly to ensure process is killed
        sleep 2
    fi
    
    # Remove PID file
    rm -f "$PID_FILE"
    
    print_status "$APP_NAME stopped successfully"
}

# Function to show application status
status_app() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        print_status "$APP_NAME is running with PID: $PID"
    else
        print_warning "$APP_NAME is not running"
    fi
}

# Function to show logs
show_logs() {
    if [ -f "$LOG_FILE" ]; then
        echo "Showing logs from $LOG_FILE (Ctrl+C to stop):"
        tail -f "$LOG_FILE"
    else
        print_warning "Log file not found: $LOG_FILE"
        echo "Starting application first to create log file..."
        start_app
        tail -f "$LOG_FILE"
    fi
}

# Main script logic
case "$1" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        stop_app
        sleep 3
        start_app
        ;;
    status)
        status_app
        ;;
    logs)
        show_logs
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs}"
        echo ""
        echo "Commands:"
        echo "  start   - Start the Multiroom Audio Application server"
        echo "  stop    - Stop the Multiroom Audio Application server"
        echo "  restart - Restart the Multiroom Audio Application server"
        echo "  status  - Show status of the Multiroom Audio Application server"
        echo "  logs    - Show application logs (follow mode)"
        exit 1
        ;;
esac

exit 0