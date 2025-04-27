package view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import src.controller.MainController;
import src.controller.NavigationController;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Khởi tạo các thành phần cốt lõi
        UI ui = new UI();
        NavigationController navigationController = new NavigationController(ui);
        MainController mainController = new MainController(ui, navigationController);

        // Liên kết UI với các controller
        ui.setControllers(mainController, navigationController);

        // Tạo giao diện
        Scene scene = ui.createScene();

        // Cấu hình và hiển thị stage
        primaryStage.setTitle("AITalk - Hệ thống quản lý đào tạo");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();

        // Khởi động ứng dụng
        mainController.onAppStart();

        // Thiết lập xử lý khi đóng ứng dụng
        primaryStage.setOnCloseRequest(event -> {
            mainController.onAppExit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
