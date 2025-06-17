import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class Unified_Calc_FX extends Application {

    static class NumberWrapper {
        double value;
        boolean isFloat;

        NumberWrapper(double value, boolean isFloat) {
            this.value = value;
            this.isFloat = isFloat;
        }

        static NumberWrapper fromString(String str) {
            if (str.contains(".")) {
                return new NumberWrapper(Double.parseDouble(str), true);
            } else {
                return new NumberWrapper(Integer.parseInt(str), false);
            }
        }

        @Override
        public String toString() {
            return isFloat ? String.format("%.2f", value) : String.valueOf((int) value);
        }
    }

    static abstract class Calculator {
        abstract void pushVal(NumberWrapper val);
        abstract NumberWrapper popVal();
        abstract void pushOp(char ch);
        abstract char popOp();
        abstract char peekOp();
        abstract boolean isEmptyOp();
        abstract void clear();

        int precedence(char op) {
            return switch (op) {
                case '+', '-' -> 1;
                case '*', '/', '%' -> 2;
                default -> -1;
            };
        }

        boolean ValidExpression(String expr) {
            if (expr.isEmpty()) return false;
            // Only digits, operators, parentheses, and dot
            if (!expr.matches("[0-9+\\-*/%().\\s]*")) return false;
            // No alphabets or illegal characters
            if (expr.matches(".*[a-zA-Z].*")) return false;
            // No consecutive invalid operators
            if (expr.matches(".*([+*/%.-])\\1+.*")) return false;
            // No multiple decimal points in a number
            if (expr.matches(".*\\d*\\.\\d*\\.\\d*.*")) return false;
            // Cannot start or end with operator (except unary minus not handled)
            if (expr.matches("^[+*/%]+.*") || expr.matches(".*[+*/%.-]$")) return false;

            // Check for balanced parentheses
            int balance = 0;
            for (char ch : expr.toCharArray()) {
                if (ch == '(') balance++;
                else if (ch == ')') balance--;
                if (balance < 0) return false;
            }
            return balance == 0;
        }

        NumberWrapper evaluate(String expr) {
            expr = expr.replaceAll("(?<=\\d|\\))\\(", "*("); // handle 2(3+4) as 2*(3+4)
            clear();

            for (int i = 0; i < expr.length(); ) {
                char ch = expr.charAt(i);
                if (Character.isDigit(ch) || ch == '.') {
                    int j = i;
                    while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                    pushVal(NumberWrapper.fromString(expr.substring(i, j)));
                    i = j;
                } else if (ch == '(') {
                    pushOp(ch);
                    i++;
                } else if (ch == ')') {
                    while (!isEmptyOp() && peekOp() != '(') applyTopOperator();
                    if (!isEmptyOp()) popOp();
                    else throw new RuntimeException("Mismatched parentheses");
                    i++;
                } else if ("+-*/%".indexOf(ch) != -1) {
                    while (!isEmptyOp() && precedence(peekOp()) >= precedence(ch)) applyTopOperator();
                    pushOp(ch);
                    i++;
                } else {
                    throw new RuntimeException("Invalid character encountered: '" + ch + "'");
                }
            }

            while (!isEmptyOp()) {
                if (peekOp() == '(' || peekOp() == ')') throw new RuntimeException("Mismatched parentheses");
                applyTopOperator();
            }

            return popVal();
        }

        void applyTopOperator() {
            char op = popOp();
            NumberWrapper b = popVal();
            NumberWrapper a = popVal();

            boolean isFloat = a.isFloat || b.isFloat;
            double result = switch (op) {
                case '+' -> a.value + b.value;
                case '-' -> a.value - b.value;
                case '*' -> a.value * b.value;
                case '/' -> {
                    if (b.value == 0) throw new ArithmeticException("Division by zero.");
                    yield a.value / b.value;
                }
                case '%' -> {
                    if (b.value == 0) throw new ArithmeticException("Modulo by zero.");
                    yield a.value % b.value;
                }
                default -> throw new RuntimeException("Unknown operator");
            };

            System.out.printf("Evaluated: %s %c %s = %s%n", a, op, b, isFloat ? String.format("%.2f", result) : (int) result);
            pushVal(new NumberWrapper(result, isFloat));
        }
    }

    static class ArrayCalculator extends Calculator {
        static final int MAX = 100;
        NumberWrapper[] valueStack = new NumberWrapper[MAX];
        int valTop = -1;
        char[] opStack = new char[MAX];
        int opTop = -1;

        void pushVal(NumberWrapper val) { valueStack[++valTop] = val; }
        NumberWrapper popVal() { return valueStack[valTop--]; }
        void pushOp(char ch) { opStack[++opTop] = ch; }
        char popOp() { return opStack[opTop--]; }
        char peekOp() { return opStack[opTop]; }
        boolean isEmptyOp() { return opTop == -1; }
        void clear() { valTop = -1; opTop = -1; }
    }

    static class LinkedListCalculator extends Calculator {
        Deque<NumberWrapper> valueStack = new LinkedList<>();
        Deque<Character> opStack = new LinkedList<>();

        void pushVal(NumberWrapper val) { valueStack.push(val); }
        NumberWrapper popVal() { return valueStack.pop(); }
        void pushOp(char ch) { opStack.push(ch); }
        char popOp() { return opStack.pop(); }
        char peekOp() { return opStack.peek(); }
        boolean isEmptyOp() { return opStack.isEmpty(); }
        void clear() { valueStack.clear(); opStack.clear(); }
    }

    static class QueueCalculator extends Calculator {
        LinkedList<NumberWrapper> valueQueue = new LinkedList<>();
        LinkedList<Character> opQueue = new LinkedList<>();

        void pushVal(NumberWrapper val) { valueQueue.offerLast(val); }
        NumberWrapper popVal() { return valueQueue.removeLast(); }
        void pushOp(char ch) { opQueue.offerLast(ch); }
        char popOp() { return opQueue.removeLast(); }
        char peekOp() { return opQueue.peekLast(); }
        boolean isEmptyOp() { return opQueue.isEmpty(); }
        void clear() { valueQueue.clear(); opQueue.clear(); }
    }

    private Calculator calc;
    private Label resultLabel;
    private TextArea outputArea;
    private TextField inputField;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Unified Calculator");

        // Data Structure Selection
        ChoiceBox<String> dsChoiceBox = new ChoiceBox<>();
        dsChoiceBox.getItems().addAll("Array", "LinkedList", "Queue");
        dsChoiceBox.setValue("Array"); // Default selection

        // Input Field
        inputField = new TextField();
        inputField.setPromptText("Enter expression");

        // Result Label
        resultLabel = new Label("Result: ");

        // Output Area
        outputArea = new TextArea();
        outputArea.setEditable(false);

        // Buttons
        GridPane buttonGrid = new GridPane();
        buttonGrid.setAlignment(Pos.CENTER);
        buttonGrid.setHgap(10);
        buttonGrid.setVgap(10);
        buttonGrid.setPadding(new Insets(10, 10, 10, 10));

        String[] buttonLabels = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "0", ".", "=", "+",
                "(", ")", "%", "Backspace", "Clear" // Added Backspace
        };

        int row = 0, col = 0;
        for (String label : buttonLabels) {
            Button button = new Button(label);
            button.setPrefWidth(50);
            button.setOnAction(e -> handleButtonPress(label));
            buttonGrid.add(button, col, row);
            col++;
            if (col > 3) { // Changed to accommodate new buttons if needed, or adjust grid
                col = 0;
                row++;
            }
        }

        // Evaluate Button (already part of buttonGrid now with "=")
        // No need for a separate evaluate button. The "=" button handles it.

        // Layout
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10, 10, 10, 10));
        mainLayout.getChildren().addAll(
                new Label("Choose Data Structure:"),
                dsChoiceBox,
                inputField,
                buttonGrid,
                resultLabel,
                outputArea
        );

        // Data Structure Choice Listener
        dsChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "Array" -> calc = new ArrayCalculator();
                case "LinkedList" -> calc = new LinkedListCalculator();
                case "Queue" -> calc = new QueueCalculator();
            }
            outputArea.appendText("Switched to " + newVal + ".\n");
        });

        // Initial Calculator Setup
        calc = new ArrayCalculator(); // Default calculator
        outputArea.appendText("Using Array as default.\n");

        Scene scene = new Scene(mainLayout, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleButtonPress(String label) {
        switch (label) {
            case "=":
                evaluateExpression();
                break;
            case "Clear":
                inputField.clear();
                break;
            case "Backspace": // Handle backspace
                String currentText = inputField.getText();
                if (currentText.length() > 0) {
                    inputField.setText(currentText.substring(0, currentText.length() - 1));
                }
                break;
            default:
                inputField.appendText(label);
                break;
        }
    }

    private void evaluateExpression() {
        String input = inputField.getText().replaceAll("\\s+", "");

        if (!calc.ValidExpression(input)) {
            outputArea.appendText("Invalid expression. Please re-enter. Check for:\n");
            outputArea.appendText("- Only numbers, + - * / % . ( ) allowed\n");
            outputArea.appendText("- No alphabets or symbols\n");
            outputArea.appendText("- Balanced parentheses\n");
            outputArea.appendText("- No trailing or repeating operators\n");
            return;
        }

        try {
            NumberWrapper result = calc.evaluate(input);
            resultLabel.setText("Result: " + result);
        } catch (Exception e) {
            outputArea.appendText("Error: " + e.getMessage() + "\n");
            resultLabel.setText("Result: Error");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
