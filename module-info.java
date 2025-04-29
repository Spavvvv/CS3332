module CS3323 {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.sql;
    requires java.desktop;

    exports view;
    exports src.controller;
    exports src.model;

    //opens src.controller to javafx.fxml;
    //opens src.model to javafx.fxml;
    opens view.components to javafx.base, javafx.fxml;

}