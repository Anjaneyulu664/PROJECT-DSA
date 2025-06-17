import java.util.*;

public class Unified_Calc {

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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Calculator calc;

        while (true) {
            calc = null;

            while (calc == null) {
                System.out.println("Choose Data Structure to perform :");
                System.out.println("1. Array");
                System.out.println("2. LinkedList");
                System.out.println("3. Queue");
                System.out.print("Enter choice: ");
                int choice = Integer.parseInt(sc.nextLine().trim());

                switch (choice) {
                    case 1 -> calc = new ArrayCalculator();
                    case 2 -> calc = new LinkedListCalculator();
                    case 3 -> calc = new QueueCalculator();
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }

            while (true) {
                System.out.print("Enter expression: ");
                String input = sc.nextLine().replaceAll("\\s+", "");

                if (!calc.ValidExpression(input)) {
                    System.out.println("Invalid expression. Please re-enter. Check for:");
                    System.out.println("- Only numbers, + - * / % . ( ) allowed");
                    System.out.println("- No alphabets or symbols");
                    System.out.println("- Balanced parentheses");
                    System.out.println("- No trailing or repeating operators");
                    continue;
                }

                try {
                    NumberWrapper result = calc.evaluate(input);
                    System.out.println("Final Result: " + result);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

                System.out.print("Evaluate another expression? (y/n): ");
                if (!sc.nextLine().trim().equalsIgnoreCase("y")) break;
            }

            System.out.print("Switch data structure? (y/n): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("y")) break;
        }

        System.out.println("Calculator closed.");
        sc.close();
    }
}
