package view;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Tạo UI từ class UI
        UI ui = new UI();
        primaryStage.setScene(ui.createScene());
        primaryStage.setTitle("Học viên - Trung Tâm Luyện Thi iClass");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}