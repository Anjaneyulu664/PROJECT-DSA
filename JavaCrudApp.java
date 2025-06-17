import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaCrudApp extends Application {

    public static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    public static final String DB_USER = "system"; 
    private String currentDbPassword; 
    private VBox operationPanel;
    private VBox queryPanel;
    private VBox outputPanel;
    private TextArea queryTextArea;
    private TextArea outputTextArea;
    private TableView<TableRowData> resultTable;
    private String currentOperation = "";
    private TextField tableNameField;
    private ComboBox<String> tableDropdown;
    private VBox columnContainer;
    private List<ColumnRow> columnRows;
    private VBox insertFieldsContainer;
    private TextField setClauseField;
    private TextField whereClauseField;
    private List<String> userTables;

    private Button selectAllBtn;
    private Button deleteSelectedBtn;
    private CheckBox selectAllCheckBox;

    @Override
    public void start(Stage primaryStage) {

        LoginScreen loginScreen = new LoginScreen(DB_USER, DB_URL);
        loginScreen.display(primaryStage);

        if (loginScreen.isLoginSuccessful()) {
            this.currentDbPassword = loginScreen.getPassword(); 

            primaryStage.setTitle("Oracle Database Management System - JDBC CRUD Operations");
            primaryStage.setFullScreen(false); 
            primaryStage.setResizable(true); 
            primaryStage.setFullScreenExitHint("Press ESC to exit fullscreen");

            initializeComponents();

            HBox mainLayout = new HBox(15);
            mainLayout.setPadding(new Insets(15));
            mainLayout.setStyle("-fx-background-color: #f5f5f5;");

            createOperationPanel();
            createQueryPanel();
            createOutputPanel();
            operationPanel.setPrefWidth(250); 
            queryPanel.setPrefWidth(550); 
            outputPanel.setPrefWidth(450);

            mainLayout.getChildren().addAll(operationPanel, queryPanel, outputPanel);

            Scene scene = new Scene(mainLayout);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();

            loadUserTables();
        } else {
            Platform.exit();
        }
    }

    private void initializeComponents() {
        columnRows = new ArrayList<>();
        userTables = new ArrayList<>();

        queryTextArea = new TextArea();
        queryTextArea.setPrefRowCount(5); 
        queryTextArea.setEditable(false);
        queryTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 20px; " +
                               "-fx-background-color: #2d3748; -fx-text-fill:rgb(11, 11, 11); -fx-border-radius: 8;");
        queryTextArea.setWrapText(true);

        outputTextArea = new TextArea();
        outputTextArea.setPrefRowCount(5); 
        outputTextArea.setEditable(false);
        outputTextArea.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; " +
                               "-fx-border-radius: 8; -fx-font-size: 16px;");
        outputTextArea.setWrapText(true);

        resultTable = new TableView<>();
        resultTable.setPrefHeight(200);
        resultTable.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        resultTable.setRowFactory(tv -> {
            TableRow<TableRowData> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && newItem.isSelected().get()) {
                    row.setStyle("-fx-background-color: #e6f3ff;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });

        selectAllBtn = new Button("Select All");
        selectAllBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        selectAllBtn.setOnAction(e -> toggleSelectAll());

        deleteSelectedBtn = new Button("Delete Selected");
        deleteSelectedBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteSelectedBtn.setOnAction(e -> deleteSelectedRecords());
        deleteSelectedBtn.setVisible(false);

        selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setOnAction(e -> toggleSelectAll());
    }

    private void createOperationPanel() {
        operationPanel = new VBox(12);
        operationPanel.setPadding(new Insets(25));
        operationPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                                "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Database Operations");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");

        Button createBtn = createOperationButton("CREATE TABLE", "create", "#10b981", currentOperation.equals("create"));
        Button insertBtn = createOperationButton("INSERT DATA", "insert", "#3b82f6", currentOperation.equals("insert"));
        Button updateBtn = createOperationButton("UPDATE DATA", "update", "#f59e0b", currentOperation.equals("update"));
        Button deleteBtn = createOperationButton("DELETE DATA", "delete", "#ef4444", currentOperation.equals("delete"));
        Button selectBtn = createOperationButton("SELECT DATA", "select", "#8b5cf6", currentOperation.equals("select"));
        Button truncateBtn = createOperationButton("TRUNCATE TABLE", "truncate", "#6b7280", currentOperation.equals("truncate"));
        Button dropBtn = createOperationButton("DROP TABLE", "drop", "#dc2626", currentOperation.equals("drop"));

        Label statusLabel = new Label("Connection Status:");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");

        Button testConnBtn = new Button("Test Connection");
        testConnBtn.setPrefHeight(40);
        testConnBtn.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        testConnBtn.setOnAction(e -> testConnection());

        Button clearConsoleBtn = new Button("Clear Console");
        clearConsoleBtn.setPrefHeight(40);
        clearConsoleBtn.setStyle("-fx-background-color: #718096; -fx-text-fill: white; -fx-font-weight: bold; " +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        clearConsoleBtn.setOnAction(e -> clearConsole());

        operationPanel.getChildren().addAll(title,
            new Separator(),
            createBtn, insertBtn, updateBtn, deleteBtn, selectBtn, truncateBtn, dropBtn,
            new Separator(),
            statusLabel, testConnBtn, clearConsoleBtn);
    }

    private Button createOperationButton(String text, String operation, String color, boolean isCurrent) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(isCurrent ? 70 : 55); 
        button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                     "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));

        button.setOnMouseEntered(e -> {
            button.setStyle(String.format("-fx-background-color: derive(%s, -20%%); -fx-text-fill: white; " +
                                         "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                         "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);", color));
        });

        button.setOnMouseExited(e -> {
            if (!operation.equals(currentOperation)) {
                button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                             "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));
            }
        });

        button.setOnAction(e -> {
            currentOperation = operation;
            updateButtonStyles();
            button.setStyle(String.format("-fx-background-color: derive(%s, -30%%); -fx-text-fill: white; " +
                                         "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                         "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);", color));
            showOperationForm(operation);
            clearFormFields();
        });

        return button;
    }

    private void updateButtonStyles() {
        operationPanel.getChildren().stream()
            .filter(node -> node instanceof Button &&
                !((Button) node).getText().equals("Test Connection") &&
                !((Button) node).getText().equals("Clear Console"))
            .forEach(node -> {
                Button btn = (Button) node;
                String text = btn.getText();
                String color = getButtonColor(text);
                btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                         "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));
                btn.setPrefHeight(55); 
            });

        Button currentButton = (Button) operationPanel.getChildren().stream()
            .filter(node -> node instanceof Button && (node instanceof Button && ((Button) node).getText().startsWith(currentOperation.toUpperCase())))
            .findFirst()
            .orElse(null);

        if (currentButton != null) {
            currentButton.setPrefHeight(70); 
            currentButton.setStyle(String.format("-fx-background-color: derive(%s, -30%%); -fx-text-fill: white; " +
                                                 "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                                 "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);",
                                                 getButtonColor(currentButton.getText())));
        }
    }

    private String getButtonColor(String buttonText) {
        switch (buttonText) {
            case "CREATE TABLE": return "#10b981";
            case "INSERT DATA": return "#3b82f6";
            case "UPDATE DATA": return "#f59e0b";
            case "DELETE DATA": return "#ef4444";
            case "SELECT DATA": return "#8b5cf6";
            case "TRUNCATE TABLE": return "#6b7280";
            case "DROP TABLE": return "#dc2626";
            default: return "#6b7280";
        }
    }

    private void createQueryPanel() {
        queryPanel = new VBox(15);
        queryPanel.setPadding(new Insets(25));
        queryPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                            "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Query Builder");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");

        ScrollPane scrollPane = new ScrollPane();
        VBox formContainer = new VBox(15);
        scrollPane.setContent(formContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        Button executeBtn = new Button("Execute Query");
        executeBtn.setPrefWidth(250);
        executeBtn.setPrefHeight(60);
        executeBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 18px; " +
                            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        executeBtn.setOnMouseEntered(e -> executeBtn.setStyle("-fx-background-color: #047857; -fx-text-fill: white; " +
                                                            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                                                            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"));
        executeBtn.setOnMouseExited(e -> executeBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                                                            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        executeBtn.setOnAction(e -> executeQuery());

        HBox executeContainer = new HBox();
        executeContainer.setAlignment(Pos.CENTER);
        executeContainer.getChildren().add(executeBtn);

        queryPanel.getChildren().addAll(title, new Separator(), scrollPane, executeContainer);

        queryPanel.setUserData(formContainer);
    }

    private void createOutputPanel() {
        outputPanel = new VBox(15);
        outputPanel.setPadding(new Insets(25));
        outputPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                            "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Query Output");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");

        Label queryLabel = new Label("Generated SQL:");
        queryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");

        Label outputLabel = new Label("Execution Result:");
        outputLabel.setStyle("-fx-font-weight: bold; -fx-text-fill:rgb(1, 2, 4); -fx-font-size: 16px;");

        HBox tableHeaderBox = new HBox(10);
        tableHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Label tableLabel = new Label("Result Data:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");

        HBox selectionControls = new HBox(10);
        selectionControls.setAlignment(Pos.CENTER_RIGHT);
        selectionControls.getChildren().addAll(selectAllBtn, deleteSelectedBtn);

        tableHeaderBox.getChildren().addAll(tableLabel, new Region(), selectionControls);
        HBox.setHgrow(tableHeaderBox.getChildren().get(1), Priority.ALWAYS);

        outputPanel.getChildren().addAll(title, new Separator(), queryLabel, queryTextArea,
                                         outputLabel, outputTextArea, tableHeaderBox, resultTable);
    }

    private void showOperationForm(String operation) {
        VBox formContainer = (VBox) queryPanel.getUserData();
        formContainer.getChildren().clear();

        switch (operation) {
            case "create":
                showCreateForm(formContainer);
                break;
            case "insert":
                showInsertForm(formContainer);
                break;
            case "select":
                showSelectForm(formContainer);
                break;
            case "update":
                showUpdateForm(formContainer);
                break;
            case "delete":
                showDeleteForm(formContainer);
                break;
            case "truncate":
                showTruncateForm(formContainer);
                break;
            case "drop":
                showDropForm(formContainer);
                break;
        }
    }

    private void showCreateForm(VBox container) {
        Label instruction = new Label("Create a new table with custom columns");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableNameField = new TextField();
        tableNameField.setPromptText("Enter table name (e.g., EMPLOYEES)");
        tableNameField.setPrefHeight(40);
        tableNameField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        Label columnsLabel = new Label("Table Columns:");
        columnsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        columnContainer = new VBox(8);

        Button addColumnBtn = new Button("Add Column");
        addColumnBtn.setPrefHeight(35);
        addColumnBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        addColumnBtn.setOnAction(e -> addColumnRow());

        columnRows.clear();
        addColumnRow();

        container.getChildren().addAll(instruction, tableLabel, tableNameField, columnsLabel, columnContainer, addColumnBtn);
    }

    private void showInsertForm(VBox container) {
        Label instruction = new Label("Insert data into an existing table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        tableDropdown.setOnAction(e -> loadTableFieldsForInsert());

        insertFieldsContainer = new VBox(10);

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, insertFieldsContainer);
    }

    private void showSelectForm(VBox container) {
        Label instruction = new Label("Retrieve data from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);

        Label whereLabel = new Label("WHERE Clause (Optional):");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID > 10 AND NAME LIKE '%John%'");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, whereLabel, whereClauseField);
    }

    private void showUpdateForm(VBox container) {
        Label instruction = new Label("Update existing records in a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);

        Label setLabel = new Label("SET Clause:");
        setLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        setClauseField = new TextField();
        setClauseField.setPromptText("e.g., NAME = 'John Doe', SALARY = 50000");
        setClauseField.setPrefHeight(40);
        setClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        Label whereLabel = new Label("WHERE Clause:");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID = 1");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, setLabel, setClauseField, whereLabel, whereClauseField);
    }

    private void showDeleteForm(VBox container) {
        Label instruction = new Label("Delete records from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);

        Label whereLabel = new Label("WHERE Clause:");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID = 1 OR STATUS = 'INACTIVE'");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        Label warningLabel = new Label(" Warning: This will permanently delete data!");
        warningLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 16px;");

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, whereLabel, whereClauseField, warningLabel);
    }

    private void showTruncateForm(VBox container) {
        Label instruction = new Label("Remove all data from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);

        Label warningLabel = new Label("DANGER: This will delete ALL data in the table!");
        warningLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label warningLabel2 = new Label("This operation cannot be undone!");
        warningLabel2.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 14px;");

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, warningLabel, warningLabel2);
    }

    private void showDropForm(VBox container) {
        Label instruction = new Label("Drop (delete) an entire table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");

        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");

        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);

        Label warningLabel = new Label("EXTREME DANGER: This will delete the entire table!");
        warningLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label warningLabel2 = new Label("Table structure and all data will be permanently lost!");
        warningLabel2.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 14px;");

        container.getChildren().addAll(instruction, tableLabel, tableDropdown, warningLabel, warningLabel2);
    }

    private void addColumnRow() {
        HBox columnRow = new HBox(10);
        columnRow.setAlignment(Pos.CENTER_LEFT);

        TextField columnName = new TextField();
        columnName.setPromptText("Column Name");
        columnName.setPrefWidth(180);
        columnName.setPrefHeight(35);
        columnName.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

        ComboBox<String> dataType = new ComboBox<>();
        dataType.getItems().addAll(
            "VARCHAR2(255)", "VARCHAR2(100)", "VARCHAR2(50)",
            "NUMBER", "NUMBER(10)", "NUMBER(10,2)",
            "DATE", "TIMESTAMP", "CHAR(10)", "CHAR(1)",
            "CLOB", "BLOB", "INTEGER", "FLOAT"
        );
        dataType.setValue("VARCHAR2(255)");
        dataType.setPrefWidth(150);
        dataType.setPrefHeight(35);
        dataType.setStyle("-fx-background-radius: 6; -fx-font-size: 14px;");

        Button removeBtn = new Button("X"); 
        removeBtn.setPrefHeight(35);
        removeBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        removeBtn.setOnAction(e -> {
            columnContainer.getChildren().remove(columnRow);
            columnRows.removeIf(cr -> cr.getContainer() == columnRow);
        });

        columnRow.getChildren().addAll(columnName, dataType, removeBtn);
        columnContainer.getChildren().add(columnRow);

        columnRows.add(new ColumnRow(columnRow, columnName, dataType));
    }

    private void loadTableFieldsForInsert() {
        String tableName = tableDropdown.getValue();
        if (tableName == null || tableName.isEmpty()) return;

        insertFieldsContainer.getChildren().clear();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, currentDbPassword)) { 
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null); 

            boolean hasColumns = false;
            while (columns.next()) {
                hasColumns = true;
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");

                HBox fieldRow = new HBox(10);
                fieldRow.setAlignment(Pos.CENTER_LEFT);

                Label label = new Label(columnName);
                label.setPrefWidth(150);
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 14px;");

                TextField valueField = new TextField();
                valueField.setPromptText("Enter " + columnName.toLowerCase());
                valueField.setPrefWidth(250);
                valueField.setPrefHeight(35);
                valueField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");

                Label typeLabel = new Label(dataType + "(" + columnSize + ")");
                typeLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

                VBox fieldInfo = new VBox(2);
                fieldInfo.getChildren().addAll(valueField, typeLabel);

                fieldRow.getChildren().addAll(label, fieldInfo);
                insertFieldsContainer.getChildren().add(fieldRow);
            }

            if (!hasColumns) {
                Label noFields = new Label("Table '" + tableName + "' not found or has no columns.");
                noFields.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 14px;");
                insertFieldsContainer.getChildren().add(noFields);
            }

        } catch (SQLException e) {
            showAlert("Database Error", "Error loading table fields: " + e.getMessage());
            outputTextArea.setText("Error: " + e.getMessage());
        }
    }

    private void loadUserTables() {
        userTables.clear();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, currentDbPassword); 
             Statement stmt = conn.createStatement()) {

            String query = "SELECT object_name FROM user_objects WHERE object_type = 'TABLE' ORDER BY created DESC";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                userTables.add(rs.getString("object_name"));
            }

            if (tableDropdown != null) {
                tableDropdown.getItems().clear();
                tableDropdown.getItems().addAll(userTables);
            }
            outputTextArea.setText("Successfully loaded user tables.");

        } catch (SQLException e) {
            showAlert("Database Error", "Error loading user tables: " + e.getMessage());
            outputTextArea.setText("Error loading user tables: " + e.getMessage());
        }
    }

    private void testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, currentDbPassword)) {
            if (conn != null) {
                showAlert("Connection Successful", "Connected to Oracle Database!");
                outputTextArea.setText("Connection Successful: Connected to Oracle Database!");
            } else {
                showAlert("Connection Failed", "Failed to establish database connection.");
                outputTextArea.setText("Connection Failed: Failed to establish database connection.");
            }
        } catch (SQLException e) {
            showAlert("Connection Error", "Error connecting to database: " + e.getMessage());
            outputTextArea.setText("Connection Error: " + e.getMessage());
        }
    }

    private void clearConsole() {
        queryTextArea.clear();
        outputTextArea.clear();
        resultTable.getColumns().clear();
        resultTable.getItems().clear();
        deleteSelectedBtn.setVisible(false);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFormFields() {
        if (tableNameField != null) tableNameField.clear();
        if (tableDropdown != null) tableDropdown.getSelectionModel().clearSelection();
        if (columnContainer != null) columnContainer.getChildren().clear();
        columnRows.clear();
        if (insertFieldsContainer != null) insertFieldsContainer.getChildren().clear();
        if (setClauseField != null) setClauseField.clear();
        if (whereClauseField != null) whereClauseField.clear();
        resultTable.getColumns().clear();
        resultTable.getItems().clear();
        deleteSelectedBtn.setVisible(false);
    }

    private void toggleSelectAll() {
        boolean select = selectAllCheckBox.isSelected();
        for (TableRowData item : resultTable.getItems()) {
            item.setSelected(select);
        }
    }

    private void deleteSelectedRecords() {
        List<TableRowData> selectedItems = new ArrayList<>();
        for (TableRowData item : resultTable.getItems()) {
            if (item.isSelected().get()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            showAlert("No Selection", "No rows selected for deletion.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Selected Rows?");
        confirmAlert.setContentText("Are you sure you want to delete " + selectedItems.size() + " selected row(s)? This action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            outputTextArea.setText("Attempting to delete selected rows. (Not yet fully implemented for dynamic tables)");
        }
    }

    public static class TableRowData {
        private final BooleanProperty selected;
        private final List<StringProperty> cells;

        public TableRowData() {
            this.selected = new SimpleBooleanProperty(false);
            this.cells = new ArrayList<>();
            this.selected.addListener((obs, oldVal, newVal) -> {
            });
        }

        public BooleanProperty isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public List<StringProperty> getCells() {
            return cells;
        }

        public StringProperty getCell(int index) {
            return cells.get(index);
        }

        public void addCell(String value) {
            cells.add(new SimpleStringProperty(value));
        }
    }

    private static class ColumnRow {
        private final HBox container;
        private final TextField columnNameField;
        private final ComboBox<String> dataTypeComboBox;

        public ColumnRow(HBox container, TextField columnNameField, ComboBox<String> dataTypeComboBox) {
            this.container = container;
            this.columnNameField = columnNameField;
            this.dataTypeComboBox = dataTypeComboBox;
        }

        public HBox getContainer() {
            return container;
        }

        public String getColumnName() {
            return columnNameField.getText();
        }

        public String getDataType() {
            return dataTypeComboBox.getValue();
        }
    }

    private void executeQuery() {
        String sqlQuery = generateQuery();
        queryTextArea.setText(sqlQuery); 

        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            outputTextArea.setText("Error: No query generated. Please complete the form.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, currentDbPassword)) {
            if (currentOperation.equals("select")) {
                executeSelect(conn, sqlQuery);
            } else {
                executeDML(conn, sqlQuery);
            }
            loadUserTables(); 
        } catch (SQLException e) {
            outputTextArea.setText("Error executing query: " + e.getMessage());
            showAlert("Execution Error", "Failed to execute query: " + e.getMessage());
        }
    }

    private String generateQuery() {
        String tableName;
        StringBuilder queryBuilder = new StringBuilder();

        switch (currentOperation) {
            case "create":
                tableName = tableNameField.getText().trim();
                if (tableName.isEmpty()) {
                    showAlert("Missing Input", "Please enter a table name for CREATE operation.");
                    return null;
                }
                if (columnRows.isEmpty()) {
                    showAlert("Missing Input", "Please add at least one column for CREATE operation.");
                    return null;
                }
                queryBuilder.append("CREATE TABLE ").append(tableName).append(" (");
                List<String> columnDefinitions = new ArrayList<>();
                for (ColumnRow row : columnRows) {
                    String colName = row.getColumnName().trim();
                    String dataType = row.getDataType();
                    if (!colName.isEmpty() && dataType != null) {
                        columnDefinitions.add(colName + " " + dataType);
                    }
                }
                if (columnDefinitions.isEmpty()) {
                    showAlert("Invalid Columns", "No valid column definitions provided.");
                    return null;
                }
                queryBuilder.append(String.join(", ", columnDefinitions)).append(")");
                break;

            case "insert":
                tableName = tableDropdown.getValue();
                if (tableName == null || tableName.isEmpty()) {
                    showAlert("Missing Input", "Please select a table for INSERT operation.");
                    return null;
                }

                List<String> columns = new ArrayList<>();
                List<String> values = new ArrayList<>();
                VBox container = insertFieldsContainer;
                for (javafx.scene.Node node : container.getChildren()) {
                    if (node instanceof HBox) {
                        HBox fieldRow = (HBox) node;
                        Label colLabel = (Label) fieldRow.getChildren().get(0);
                        VBox fieldInfo = (VBox) fieldRow.getChildren().get(1);
                        TextField valueField = (TextField) fieldInfo.getChildren().get(0);

                        String columnName = colLabel.getText();
                        String value = valueField.getText();

                        if (!columnName.isEmpty()) {
                            columns.add(columnName);
                            if (value.matches("-?\\d+(\\.\\d+)?")) { 
                                values.add(value);
                            } else {
                                values.add("'" + value + "'"); 
                            }
                        }
                    }
                }

                if (columns.isEmpty()) {
                    showAlert("Missing Data", "No column data provided for INSERT operation.");
                    return null;
                }

                queryBuilder.append("INSERT INTO ").append(tableName).append(" (")
                            .append(String.join(", ", columns)).append(") VALUES (")
                            .append(String.join(", ", values)).append(")");
                break;

            case "select":
                tableName = tableDropdown.getValue();
                if (tableName == null || tableName.isEmpty()) {
                    showAlert("Missing Input", "Please select a table for SELECT operation.");
                    return null;
                }
                queryBuilder.append("SELECT * FROM ").append(tableName);
                String whereClauseSelect = whereClauseField.getText().trim();
                if (!whereClauseSelect.isEmpty()) {
                    queryBuilder.append(" WHERE ").append(whereClauseSelect);
                }
                break;

            case "update":
                tableName = tableDropdown.getValue();
                String setClause = setClauseField.getText().trim();
                String whereClauseUpdate = whereClauseField.getText().trim();

                if (tableName == null || tableName.isEmpty() || setClause.isEmpty() || whereClauseUpdate.isEmpty()) {
                    showAlert("Missing Input", "Please select a table, SET clause, and WHERE clause for UPDATE operation.");
                    return null;
                }
                queryBuilder.append("UPDATE ").append(tableName).append(" SET ").append(setClause)
                            .append(" WHERE ").append(whereClauseUpdate);
                break;

            case "delete":
                tableName = tableDropdown.getValue();
                String whereClauseDelete = whereClauseField.getText().trim();

                if (tableName == null || tableName.isEmpty() || whereClauseDelete.isEmpty()) {
                    showAlert("Missing Input", "Please select a table and WHERE clause for DELETE operation.");
                    return null;
                }
                queryBuilder.append("DELETE FROM ").append(tableName).append(" WHERE ").append(whereClauseDelete);
                break;

            case "truncate":
                tableName = tableDropdown.getValue();
                if (tableName == null || tableName.isEmpty()) {
                    showAlert("Missing Input", "Please select a table for TRUNCATE operation.");
                    return null;
                }
                queryBuilder.append("TRUNCATE TABLE ").append(tableName);
                break;

            case "drop":
                tableName = tableDropdown.getValue();
                if (tableName == null || tableName.isEmpty()) {
                    showAlert("Missing Input", "Please select a table for DROP operation.");
                    return null;
                }
                queryBuilder.append("DROP TABLE ").append(tableName);
                break;

            default:
                outputTextArea.setText("Please select an operation.");
                return null;
        }
        return queryBuilder.toString();
    }

    private void executeDML(Connection conn, String sqlQuery) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            int rowsAffected = stmt.executeUpdate(sqlQuery);
            outputTextArea.setText("Query executed successfully.\nRows affected: " + rowsAffected);
            showAlert("Success", "Operation completed successfully.\nRows affected: " + rowsAffected);
        }
    }

    private void executeSelect(Connection conn, String sqlQuery) throws SQLException {
        resultTable.getColumns().clear();
        resultTable.getItems().clear();
        deleteSelectedBtn.setVisible(false); 

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            TableColumn<TableRowData, Boolean> selectColumn = new TableColumn<>("");
            selectColumn.setPrefWidth(30);
            selectColumn.setResizable(false);
            selectColumn.setCellValueFactory(param -> param.getValue().isSelected());
            selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
            resultTable.getColumns().add(selectColumn);

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1; 
                TableColumn<TableRowData, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(param -> param.getValue().getCell(columnIndex));
                resultTable.getColumns().add(column);
            }

            while (rs.next()) {
                TableRowData rowData = new TableRowData();
                for (int i = 1; i <= columnCount; i++) {
                    rowData.addCell(rs.getString(i));
                }
                resultTable.getItems().add(rowData);
            }

            if (resultTable.getItems().isEmpty()) {
                outputTextArea.setText("Query executed successfully. No records found.");
            } else {
                outputTextArea.setText("Query executed successfully. " + resultTable.getItems().size() + " records retrieved.");
                deleteSelectedBtn.setVisible(true); 
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}