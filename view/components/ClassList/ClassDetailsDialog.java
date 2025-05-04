package view.components.ClassList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClassDetailsDialog {
    private Stage dialogStage;
    private VBox mainContainer;
    private ScrollPane scrollPane;

    // Lớp học cần hiển thị chi tiết
    private ClassListScreenView.ClassInfo classInfo;

    // Constants for styling
    private static final String PRIMARY_COLOR = "#4F46E5";
    private static final String BORDER_COLOR = "#E2E8F0";
    private static final String TEXT_COLOR = "#334155";
    private static final String LABEL_COLOR = "#64748B";
    private static final String BG_COLOR = "#F8FAFC";

    public ClassDetailsDialog(ClassListScreenView.ClassInfo classInfo) {
        this.classInfo = classInfo;
        createDialog();
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Xem lớp học");
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setMinWidth(700);
        dialogStage.setMinHeight(500);

        // Main container
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: white;");

        // Tạo form hiển thị thông tin
        VBox formContainer = createFormContainer();

        // Tạo thanh sidebar hiển thị lịch sử
        VBox sidebar = createSidebar();

        // Kết hợp form và sidebar
        HBox contentLayout = new HBox(20);
        contentLayout.getChildren().addAll(formContainer, sidebar);
        HBox.setHgrow(formContainer, Priority.ALWAYS);

        // Tạo footer với nút đóng
        HBox footer = createFooter();

        // Thêm vào container chính
        mainContainer.getChildren().addAll(contentLayout, footer);

        // Tạo scroll pane
        scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: white;");

        // Tạo scene
        Scene scene = new Scene(scrollPane, 700, 500);
        dialogStage.setScene(scene);
    }

    private VBox createFormContainer() {
        VBox form = new VBox(15);
        form.setStyle("-fx-background-color: white;");

        // GridPane cho form
        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(15);

        // Tạo các label và text field
        int row = 0;

        Label codeLabel = new Label("Mã:");
        TextField codeField = createReadOnlyTextField(classInfo.getCode());
        gridPane.add(codeLabel, 0, row);
        gridPane.add(codeField, 1, row);

        Label nameLabel = new Label("Tên lớp:");
        TextField nameField = createReadOnlyTextField(classInfo.getName());
        gridPane.add(nameLabel, 2, row);
        gridPane.add(nameField, 3, row);
        row++;

        Label courseLabel = new Label("Khóa học:");
        TextField courseField = createReadOnlyTextField("Lớp chính"); // Giả sử là "Lớp chính"
        gridPane.add(courseLabel, 0, row);
        gridPane.add(courseField, 1, row, 3, 1); // Span 3 columns
        row++;

        Label locationLabel = new Label("Cơ sở:");
        TextField locationField = createReadOnlyTextField("Cơ sở Láng Hạ"); // Giả sử
        gridPane.add(locationLabel, 0, row);
        gridPane.add(locationField, 1, row, 3, 1); // Span 3 columns
        row++;

        Label statusLabel = new Label("Trạng thái:");
        TextField statusField = createReadOnlyTextField("Đang học");
        gridPane.add(statusLabel, 0, row);
        gridPane.add(statusField, 1, row);

        Label startDateLabel = new Label("Ngày bắt đầu:");
        TextField startDateField = createReadOnlyTextField(classInfo.getStartDate());
        gridPane.add(startDateLabel, 2, row);
        gridPane.add(startDateField, 3, row);
        row++;

        Label endDateLabel = new Label("Ngày kết thúc:");
        TextField endDateField = createReadOnlyTextField(classInfo.getEndDate());
        gridPane.add(endDateLabel, 0, row);
        gridPane.add(endDateField, 1, row);

        Label priceLabel = new Label("Giá:");
        HBox priceBox = new HBox(0);
        TextField priceField = createReadOnlyTextField("150.000");
        Label priceUnit = new Label(" / Buổi/Thu theo tháng");
        priceUnit.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 8 15;");
        priceBox.getChildren().addAll(priceField, priceUnit);
        gridPane.add(priceLabel, 2, row);
        gridPane.add(priceBox, 3, row);
        row++;

        Label sessionsLabel = new Label("Số buổi:");
        TextField sessionsField = createReadOnlyTextField("35");
        gridPane.add(sessionsLabel, 0, row);
        gridPane.add(sessionsField, 1, row);

        Label durationLabel = new Label("Thời lượng:");
        HBox durationBox = new HBox(0);
        TextField durationField = createReadOnlyTextField("90");
        Label durationUnit = new Label(" phút");
        durationUnit.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 8 15;");
        durationBox.getChildren().addAll(durationField, durationUnit);
        gridPane.add(durationLabel, 2, row);
        gridPane.add(durationBox, 3, row);

        // Style cho tất cả label
        gridPane.getChildren().filtered(node -> node instanceof Label).forEach(node -> {
            Label label = (Label) node;
            label.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 13px;");
        });

        // Add padding to the grid
        gridPane.setPadding(new Insets(15));

        // Add the grid to form
        form.getChildren().add(gridPane);

        return form;
    }

    private TextField createReadOnlyTextField(String text) {
        TextField textField = new TextField(text);
        textField.setEditable(false);
        textField.setStyle(
                "-fx-background-color: #f8fafc; " +
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-padding: 8 15;"
        );
        textField.setPrefWidth(200);
        return textField;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(200);
        sidebar.setStyle(
                "-fx-background-color: " + BG_COLOR + "; " +
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-width: 0 0 0 1; " +
                        "-fx-padding: 15;"
        );

        Label headerLabel = new Label("Lịch sử");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label noHistoryLabel = new Label("Không có hoạt động gần đây");
        noHistoryLabel.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 12px;");

        sidebar.getChildren().addAll(headerLabel, noHistoryLabel);

        return sidebar;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 0, 0, 0));
        footer.setSpacing(10);

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 4px;"
        );
        closeButton.setOnAction(e -> dialogStage.close());

        footer.getChildren().add(closeButton);

        return footer;
    }

    public void show() {
        dialogStage.showAndWait();
    }
}
