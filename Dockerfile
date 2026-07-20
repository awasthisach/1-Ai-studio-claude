# Dockerfile for VVF Smart Manager Android App
# This container sets up JDK 21, Android SDK (API 35, Build-Tools 34.0.0), and Gradle to build and test the Android application.

# Use eclipse-temurin for a stable JDK 21 environment
FROM eclipse-temurin:21-jdk AS builder

# Set environment variables for Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Install base dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip \
    wget \
    git \
    && rm -rf /var/lib/apt/lists/*

# Create Android SDK directory
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools

# Download and install Android Command Line Tools
# Version 11076708 is a stable and modern release of Android command line tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm cmdline-tools.zip

# Accept licenses and install platform tools, build tools, and SDK platform 35
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "build-tools;34.0.0" "platforms;android-35"

# Set working directory
WORKDIR /app

# Copy gradle files first to cache dependencies
COPY gradle/ /app/gradle/
COPY gradlew /app/
COPY gradlew.bat /app/
COPY gradle.properties /app/
COPY settings.gradle.kts /app/
COPY build.gradle.kts /app/

# Copy the rest of the application files
COPY .env.example /app/.env.example
COPY app/ /app/app/

# Make gradle wrapper executable
RUN chmod +x gradlew

# Run clean and test by default when container starts
CMD ["./gradlew", "clean", "testDebugUnitTest", "assembleDebug", "--no-daemon"]
