# CS3332
Project for Software Engineer Course of Troy University
Aigle methods applied


//Diagram for the whole program
src
├── controller
│   ├── MainController.java          # Controller chính điều phối toàn bộ ứng dụng
│   ├── NavigationController.java    # Xử lý điều hướng giữa các màn hình
│   ├── training                     # Controllers cho mục Đào tạo
│   │   ├── ScheduleController.java  # Xử lý logic cho Lịch học
│   │   ├── AttendanceController.java # Xử lý logic cho Điểm danh
│   │   ├── StudyController.java     # Xử lý logic cho Học tập
│   │   ├── ExamController.java      # Xử lý logic cho Kỳ thi
│   │   └── CertificateController.java # Xử lý logic cho Chứng chỉ
│   ├── student                      # Controllers cho mục Học viên
│   │   ├── StudentController.java   # Xử lý logic cho Học viên
│   │   └── ClassController.java     # Xử lý logic cho Lớp học
│   ├── report                       # Controllers cho mục Báo cáo
│   │   ├── StudyReportController.java # Xử lý logic cho Tình hình học tập
│   │   ├── WorkReportController.java  # Xử lý logic cho Báo cáo công việc
│   │   └── TeachingTimeController.java # Xử lý logic cho Thống kê giờ giảng
│   └── management                   # Controllers cho mục Quản lý
│       ├── ClassroomController.java # Xử lý logic cho Phòng học
│       ├── NewsController.java      # Xử lý logic cho Tin tức
│       └── HolidayController.java   # Xử lý logic cho Ngày nghỉ
│
├── model
│   ├── person                       # Models cho dữ liệu người dùng
│   │   ├── Admin.java
│   │   ├── Parent.java
│   │   ├── Person.java
│   │   ├── Student.java
│   │   └── Teacher.java
│   ├── system                       # Models cho hệ thống
│   │   ├── course                   # Models cho khóa học
│   │   │   ├── Course.java
│   │   │   └── Lesson.java
│   │   ├── schedule                 # Models cho lịch học
│   │   │   ├── Schedule.java
│   │   │   └── TimeSlot.java
│   │   ├── Center.java
│   │   ├── Department.java
│   │   └── Invoice.java
│   ├── training                     # Models cho mục Đào tạo
│   │   ├── Attendance.java          # Model cho điểm danh
│   │   ├── StudyProgress.java       # Model cho tiến độ học tập
│   │   ├── Exam.java                # Model cho kỳ thi
│   │   └── Certificate.java         # Model cho chứng chỉ
│   └── report                       # Models cho báo cáo
│       ├── StudyReport.java
│       ├── WorkReport.java
│       └── TeachingTimeReport.java
│
├── view
│   ├── Main.java                    # Entry point của ứng dụng
│   ├── UI.java                      # Giao diện chính và điều hướng
│   ├── components                   # Các component dùng chung
│   │   ├── Header.java              # Component header
│   │   ├── Sidebar.java             # Component sidebar
│   │   ├── StatsCard.java           # Component thẻ thống kê
│   │   ├── Table.java               # Component bảng dữ liệu
│   │   └── Footer.java              # Component footer
│   ├── training                     # Views cho mục Đào tạo
│   │   ├── ScheduleView.java        # UI cho Lịch học
│   │   ├── AttendanceView.java      # UI cho Điểm danh
│   │   ├── StudyView.java           # UI cho Học tập
│   │   ├── ExamView.java            # UI cho Kỳ thi
│   │   └── CertificateView.java     # UI cho Chứng chỉ
│   ├── student                      # Views cho mục Học viên
│   │   ├── StudentView.java         # UI cho Học viên
│   │   └── ClassView.java           # UI cho Lớp học
│   ├── report                       # Views cho mục Báo cáo
│   │   ├── StudyReportView.java     # UI cho Tình hình học tập
│   │   ├── WorkReportView.java      # UI cho Báo cáo công việc
│   │   └── TeachingTimeView.java    # UI cho Thống kê giờ giảng
│   └── management                   # Views cho mục Quản lý
│       ├── ClassroomView.java       # UI cho Phòng học
│       ├── NewsView.java            # UI cho Tin tức
│       └── HolidayView.java         # UI cho Ngày nghỉ
│
└── utils                            # Các tiện ích
├── DatabaseUtil.java            # Tiện ích kết nối database
├── ValidationUtil.java          # Tiện ích validate dữ liệu
├── DateTimeUtil.java            # Tiện ích xử lý thời gian
└── AlertUtil.java               # Tiện ích hiển thị thông báo
