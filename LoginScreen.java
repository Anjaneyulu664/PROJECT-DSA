import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LoginScreen {

    private Stage primaryStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private String dbUser;
    private String dbUrl;
    private boolean loginSuccessful = false;

    public LoginScreen(String dbUser, String dbUrl) {
        this.dbUser = dbUser;
        this.dbUrl = dbUrl;
    }

    public void display(Stage ownerStage) {
        primaryStage = new Stage();
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.initOwner(ownerStage);
        primaryStage.setTitle("Database Login");
        primaryStage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: #f0f2f5; -fx-border-color: #d1d9e6; -fx-border-width: 1; -fx-border-radius: 10;");

        Text scenetitle = new Text("Welcome");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 25));
        scenetitle.setStyle("-fx-fill: #333;");
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Username:");
        userName.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
        grid.add(userName, 0, 1);

        usernameField = new TextField(dbUser);
        usernameField.setPromptText("Enter your database username");
        usernameField.setPrefHeight(35);
        usernameField.setStyle("-fx-background-radius: 5; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-font-size: 14px;");
        usernameField.setEditable(false);
        grid.add(usernameField, 1, 1);

        Label pw = new Label("Password:");
        pw.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
        grid.add(pw, 0, 2);

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your database password");
        passwordField.setPrefHeight(35);
        passwordField.setStyle("-fx-background-radius: 5; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-font-size: 14px;");
        grid.add(passwordField, 1, 2);

        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(120);
        loginButton.setPrefHeight(40);
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;");
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;"));

        Button cancelButton = new Button("Exit");
        cancelButton.setPrefWidth(120);
        cancelButton.setPrefHeight(40);
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #da190b; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5; -fx-cursor: hand;"));

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().addAll(loginButton, cancelButton);
        grid.add(hbBtn, 1, 4);

        final Text actiontarget = new Text();
        actiontarget.setStyle("-fx-fill: red; -fx-font-size: 14px;");
        grid.add(actiontarget, 1, 6);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (password.isEmpty()) {
                actiontarget.setText("Password cannot be empty.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, username, password)) {
                if (conn != null) {
                    actiontarget.setText("Login Successful!");
                    actiontarget.setStyle("-fx-fill: green; -fx-font-size: 14px;");
                    loginSuccessful = true;
                    primaryStage.close();
                }
            } catch (SQLException ex) {
                actiontarget.setText("Login Failed: " + ex.getMessage());
                loginSuccessful = false;
            }
        });

        cancelButton.setOnAction(e -> {
            loginSuccessful = false;
            primaryStage.close();
            Platform.exit();
        });

        Scene scene = new Scene(grid);
        primaryStage.setScene(scene);
        primaryStage.showAndWait();
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    public String getPassword() {
        return passwordField.getText();
    }
}