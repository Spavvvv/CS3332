package src;

import javafx.application.Application;
import javafx.stage.Stage;
import src.controller.MainController;
import src.controller.NavigationController;
import src.view.components.Screen.LoginUI;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Khởi tạo LoginUI (chỉ một lần)
        LoginUI loginUI = new LoginUI(primaryStage);

        // Khởi tạo NavigationController với LoginUI
        NavigationController navigationController = new NavigationController(loginUI);

        // Khởi tạo MainController với LoginUI và NavigationController
        MainController mainController = new MainController(loginUI, navigationController);

        // Thiết lập các controller cho LoginUI
        loginUI.setControllers(mainController, navigationController);

        // Hiển thị LoginUI
        LoginUI.show(primaryStage);

        // Thiết lập xử lý khi đóng ứng dụng
        primaryStage.setOnCloseRequest(event -> {
            mainController.onAppExit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
