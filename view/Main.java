package view;

import javafx.application.Application;
import javafx.stage.Stage;
import src.controller.MainController;
import src.controller.NavigationController;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Khởi tạo LoginUI (chỉ một lần)
        RegisterUI registerUI = new RegisterUI(primaryStage);

        // Khởi tạo NavigationController với LoginUI
        NavigationController navigationController = new NavigationController(registerUI);

        // Khởi tạo MainController với LoginUI và NavigationController
        MainController mainController = new MainController(registerUI, navigationController);

        // Thiết lập các controller cho LoginUI
        registerUI.setControllers(mainController, navigationController);

        // Hiển thị LoginUI
        RegisterUI.show(primaryStage);

        // Thiết lập xử lý khi đóng ứng dụng
        primaryStage.setOnCloseRequest(event -> {
            mainController.onAppExit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
