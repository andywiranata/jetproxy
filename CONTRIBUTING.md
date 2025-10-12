# Contributing to JetProxy

First off, thank you for considering contributing to JetProxy! We welcome any contributions that help make this project better.

## Table of Contents
1. [Development Environment Setup](#development-environment-setup)
2. [Getting Started](#getting-started)
3. [Running Tests](#running-tests)
4. [Project Structure](#project-structure)
5. [Contribution Workflow](#contribution-workflow)

## Development Environment Setup

To get started with JetProxy development, you'll need the following tools installed on your system:

*   **Java 21:** The core proxy is built with Java 21. You can use a tool like `sdkman` to manage Java versions.
*   **Node.js and npm:** The dashboard is a modern web application built with Node.js. We recommend using the latest LTS release.
*   **Git:** For version control.

## Getting Started

A unified script, `run.sh`, is provided to manage both the proxy (backend) and the dashboard (frontend) services. It offers several modes for different development scenarios.

### Development Mode

This is the recommended mode for most development tasks. It starts the dashboard with Vite's live-reloading dev server and starts the proxy.

*   **Dashboard:** Accessible at `http://localhost:5173`
*   **Proxy:** Accessible at the port defined in your config (e.g., `http://localhost:8080`)

```bash
# Run dev mode with the default config (demo/config.yaml)
./run.sh --dev
```

### Production Mode

This mode builds the dashboard into an optimized, static bundle and has the proxy serve the files directly. This is useful for testing the application as a single, self-contained unit.

```bash
# Run production mode with the default config (demo/config.yaml)
./run.sh
```

### Running Services Individually

You can also run the dashboard or proxy services by themselves.

```bash
# Run only the dashboard development server
./run.sh --dashboard-only

# Run only the proxy server
./run.sh --proxy-only
```

### Using a Custom Configuration

All modes respect the `--config` flag, allowing you to point to a custom YAML file.

```bash
./run.sh --dev --config=path/to/your.yaml
```

## Running Tests

The project has a suite of unit tests for the core proxy logic. To run them, execute the following command from the project root:

```bash
./gradlew test
```

## Project Structure

*   `src/`: Contains the core source code for the JetProxy server (the "proxy").
*   `frontend/`: Contains the source code for the web UI (the "dashboard").
*   `demo/`: Includes sample configurations and helper scripts for testing.
*   `build.gradle.kts`: The main Gradle build file for the proxy.

## Contribution Workflow

1.  **Fork the repository:** Create your own fork of the project on GitHub.
2.  **Create a new branch:** Make a new branch in your fork for your changes.
    ```bash
    git checkout -b my-awesome-feature
    ```
3.  **Make your changes:** Implement your feature or bug fix.
4.  **Commit your changes:** Write a clear and concise commit message.
    ```bash
    git commit -m "feat: Add my awesome feature"
    ```
5.  **Push to your branch:**
    ```bash
    git push origin my-awesome-feature
    ```
6.  **Create a Pull Request:** Open a pull request from your branch to the main JetProxy repository.
