CS3332 - Education Management System

Project Overview

Software Engineering Project for Troy University using Agile methodologies.

Project Structure

Controller Layer

The controller layer handles the application's logic and coordinates between the view and model layers.

src/controller/├── MainController.java         # Main controller coordinating the entire application├── NavigationController.java   # Handles navigation between screens├── training/                   # Training-related controllers│   ├── ScheduleController.java     # Logic for class schedules│   ├── AttendanceController.java   # Logic for attendance tracking│   ├── StudyController.java        # Logic for study progress│   ├── ExamController.java         # Logic for exams│   └── CertificateController.java  # Logic for certificates├── student/                    # Student-related controllers│   ├── StudentController.java      # Logic for student management│   └── ClassController.java        # Logic for class management├── report/                     # Report-related controllers│   ├── StudyReportController.java    # Logic for study reports│   ├── WorkReportController.java     # Logic for work reports│   └── TeachingTimeController.java   # Logic for teaching hours statistics└── management/                 # Management-related controllers    ├── ClassroomController.java     # Logic for classroom management    ├── NewsController.java          # Logic for news management    └── HolidayController.java       # Logic for holiday management

Model Layer

The model layer represents the data structures and business objects used in the application.

src/model/├── person/                     # Person-related models│   ├── Admin.java                 # Administrator model│   ├── Parent.java                # Parent model│   ├── Person.java                # Base person model│   ├── Student.java               # Student model│   └── Teacher.java               # Teacher model├── system/                     # System-related models│   ├── course/                    # Course-related models│   │   ├── Course.java               # Course model│   │   └── Lesson.java               # Lesson model│   ├── schedule/                  # Schedule-related models│   │   ├── Schedule.java             # Schedule model│   │   └── TimeSlot.java             # Time slot model│   ├── Center.java                # Center model│   ├── Department.java            # Department model│   └── Invoice.java               # Invoice model├── training/                   # Training-related models│   ├── Attendance.java            # Attendance model│   ├── StudyProgress.java         # Study progress model│   ├── Exam.java                  # Exam model│   └── Certificate.java           # Certificate model└── report/                     # Report-related models    ├── StudyReport.java           # Study report model    ├── WorkReport.java            # Work report model    └── TeachingTimeReport.java    # Teaching time report model

View Layer

The view layer handles the user interface components and screens.

src/view/├── Main.java                   # Application entry point├── UI.java                     # Main UI and navigation├── components/                 # Shared UI components│   ├── Header.java                # Header component│   ├── Sidebar.java               # Sidebar component│   ├── StatsCard.java             # Statistics card component│   ├── Table.java                 # Data table component│   └── Footer.java                # Footer component├── training/                   # Training-related views│   ├── ScheduleView.java          # Schedule UI│   ├── AttendanceView.java        # Attendance UI│   ├── StudyView.java             # Study UI│   ├── ExamView.java              # Exam UI│   └── CertificateView.java       # Certificate UI├── student/                    # Student-related views│   ├── StudentView.java           # Student UI│   └── ClassView.java             # Class UI├── report/                     # Report-related views│   ├── StudyReportView.java       # Study report UI│   ├── WorkReportView.java        # Work report UI│   └── TeachingTimeView.java      # Teaching time statistics UI└── management/                 # Management-related views    ├── ClassroomView.java         # Classroom UI    ├── NewsView.java              # News UI    └── HolidayView.java           # Holiday UI

Utilities

Various utility classes to support the application.

src/utils/├── DatabaseUtil.java           # Database connection utilities├── ValidationUtil.java         # Data validation utilities├── DateTimeUtil.java           # Date and time utilities└── AlertUtil.java              # Alert display utilities

Features

Training management (schedules, attendance, study progress, exams, certificates)
Student management (student records, class organization)
Reporting system (study reports, work reports, teaching statistics)
Administrative functions (classroom management, news, holidays)

Technology Stack

Java
JavaFX (for UI)
MySQL (database)
Maven/Gradle (build system)

Getting Started

Clone the repository
Configure your database connection in DatabaseUtil.java
Run Main.java to start the application

Contributors

[Your Name]
[Team Member Names]

License

[License Information]