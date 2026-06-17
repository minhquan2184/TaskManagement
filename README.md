# Task Management System

## Introduction
The Task Management System is a comprehensive personal and group task management application built on the Kotlin platform.

## Key Features

### 1. Personal Task Management
**Personal List:** A secure space dedicated to individual use. Track categories like Personal and Work.

### 2. Group Management
**Public List:** Share and collaborate in a common space. Customize workspaces for specific teams.

### 3. Core Features and Navigation
- **Home:** The main dashboard containing "My List" (personal tasks) and specific group tasks (e.g., Group A, Group B, etc.). The Home view displays: My Day, All Tasks, Calendar, Personal Tasks, and Group Tasks.
- **My Day:** Displays tasks due today, categorized by personal and team assignments.
- **All Tasks:** Manage all tasks categorized by: Today, Tomorrow, Upcoming, and Done.
- **Calendar:** Integrated calendar featuring two-way synchronization with Google Calendar.

### 4. Real-time Collaboration
**Live Chat & Task Activity:** Discuss directly within each task. Any file transfers, status changes, or group messages are instantly broadcast to all members without requiring a page reload.

### 5. Platform and Integration
- **Google OAuth 2.0:** Secure login mechanism using Google accounts.
- **Architecture:** Built using Clean Architecture combined with the MVVM pattern.

## Technical Stack

### Frontend (Mobile Client)
- **Core Language:** Native Android (Kotlin).
- **UI Framework:** XML with modern design tokens.
- **Dependency Management:** Koin for Dependency Injection.
- **Local Cache:** Room Database for Offline-First capability.

### Backend & Data Infrastructure
- **Framework:** Node.js / Ktor (depending on module).
- **Core Database:** PostgreSQL with Exposed ORM (or Prisma).
- **Real-time Infrastructure:** Firebase Realtime Database acting as an event stream router.
