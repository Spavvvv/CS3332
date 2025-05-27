module CS3332 {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;

    opens src.controller to javafx.fxml;
    opens src.model to javafx.fxml;
    opens src.view.ClassList to javafx.base, javafx.fxml;
    opens src.model.dashboard to javafx.fxml;
    opens src.model.absence to javafx.base;
    opens src.model.classroom to javafx.base;
    opens src.model.teaching to javafx.base;
    opens src.model.teaching.monthly to javafx.base;
    opens src.model.teaching.quarterly to javafx.base;
    opens src.model.teaching.yearly to javafx.base;
    opens src.model.report to javafx.base;
    opens src.model.attendance to javafx.base;

    exports src.view.components.Screen;
    exports src.controller;
    exports src.model;
    exports src.model.dashboard;
    exports src;

    opens src.view.Dashboard to javafx.base, javafx.fxml;
    opens src.view.Schedule to javafx.base, javafx.fxml;
    opens src.view.Attendance to javafx.base, javafx.fxml;
    opens src.view.Report to javafx.base, javafx.fxml;
    opens src.view.Rooms to javafx.base, javafx.fxml;
    opens src.view.Holidays to javafx.base, javafx.fxml;
    exports src.controller.Dashboard;
    opens src.controller.Dashboard to javafx.fxml;
    exports src.controller.Attendance;
    opens src.controller.Attendance to javafx.fxml;
    exports src.controller.Holidays;
    opens src.controller.Holidays to javafx.fxml;
    exports src.controller.Classroom;
    opens src.controller.Classroom to javafx.fxml;
    exports src.controller.Schedules;
    opens src.controller.Schedules to javafx.fxml;
    opens src.controller.Reports to javafx.fxml;
    exports src.controller.Reports;
    exports src.controller.Settings;
    opens src.controller.Settings to javafx.fxml;
    opens src.view.settings to javafx.base;

    exports src.utils;
    exports src.view.Attendance;
}