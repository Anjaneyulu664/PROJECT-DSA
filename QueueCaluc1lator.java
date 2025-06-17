import java.util.*;

public class QueueCaluc1lator{

    public static int preced(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }

    public static double applyn(double a, double b, char op) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            default: return 0;
        }
    }

    public static boolean isParenthesesBalanced(String expression) {
        int openCount = 0, closeCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }
        return openCount == closeCount;
    }

    public static String fixParentheses(String expression, Scanner sc) {
        int openCount = 0, closeCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }

        if (openCount > closeCount) {
            System.out.println("Missing closing parenthesis. Enter position to insert ')': ");
            int position = sc.nextInt();
            sc.nextLine();
            expression = expression.substring(0, position) + ")" + expression.substring(position);
        } else if (closeCount > openCount) {
            System.out.println("Missing opening parenthesis. Enter position to insert '(': ");
            int position = sc.nextInt();
            sc.nextLine();
            expression = expression.substring(0, position) + "(" + expression.substring(position);
        }
        return expression;
    }

    public static void addToQueueList(LinkedList<Queue<Double>> queueList, double number, int capacity) {
        if (queueList.isEmpty() || queueList.getLast().size() >= capacity) {
            queueList.add(new LinkedList<>());
        }
        queueList.getLast().add(number);
    }

    
    public static void extractToInputQueueList(String expression, LinkedList<Queue<Double>> inputQueues, int inputCapacity) {
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isDigit(c)) {
                double num = 0;
                while (i < expression.length() && Character.isDigit(expression.charAt(i))) {
                    num = num * 10 + (expression.charAt(i) - '0');
                    i++;
                }
                i--;
                addToQueueList(inputQueues, num, inputCapacity);
            }
        }
    }

    
    public static void distributeToEvenOddQueues(LinkedList<Queue<Double>> inputQueues,
                                                 LinkedList<Queue<Double>> evenQueues,
                                                 LinkedList<Queue<Double>> oddQueues,
                                                 int capacity) {
        for (Queue<Double> inputQueue : inputQueues) {
            for (double num : inputQueue) {
                if ((int) num % 2 == 0)
                    addToQueueList(evenQueues, num, capacity);
                else
                    addToQueueList(oddQueues, num, capacity);
            }
        }
    }

    public static double evaluateExpression(String expression) {
        LinkedList<Double> values = new LinkedList<>();
        LinkedList<Character> operators = new LinkedList<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == ' ') continue;

            if (Character.isDigit(c)) {
                double num = 0;
                while (i < expression.length() && Character.isDigit(expression.charAt(i))) {
                    num = num * 10 + (expression.charAt(i) - '0');
                    i++;
                }
                i--;
                values.add(num);
            } else if (c == '(') {
                operators.add(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.getLast() != '(') {
                    double b = values.removeLast();
                    double a = values.removeLast();
                    char op = operators.removeLast();
                    values.add(applyn(a, b, op));
                }
                if (!operators.isEmpty()) operators.removeLast(); // remove '('
            } else {
                while (!operators.isEmpty() && preced(operators.getLast()) >= preced(c)) {
                    double b = values.removeLast();
                    double a = values.removeLast();
                    char op = operators.removeLast();
                    values.add(applyn(a, b, op));
                }
                operators.add(c);
            }
        }

        while (!operators.isEmpty()) {
            double b = values.removeLast();
            double a = values.removeLast();
            char op = operators.removeLast();
            values.add(applyn(a, b, op));
        }

        return values.removeLast();
    }

    public static void printQueueList(String label, LinkedList<Queue<Double>> queueList) {
        System.out.println(label + ":");
        int i = 1;
        for (Queue<Double> q : queueList) {
            System.out.println("  Queue " + i++ + " => " + q);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("📦 Welcome to the Capacity-Based Queue Calculator!");

        System.out.print("Enter capacity for input queue: ");
        int inputCapacity = sc.nextInt();
        System.out.print("Enter capacity for even/odd queues: ");
        int eoCapacity = sc.nextInt();
        sc.nextLine(); 

        while (true) {
            System.out.print("\nEnter a mathematical expression: ");
            String expression = sc.nextLine();

            if (!isParenthesesBalanced(expression)) {
                expression = fixParentheses(expression, sc);
                System.out.println("Fixed expression: " + expression);
            }

            LinkedList<Queue<Double>> inputQueues = new LinkedList<>();
            extractToInputQueueList(expression, inputQueues, inputCapacity);

            System.out.println("Input Queues:");
            printQueueList("Input Queues", inputQueues);
            LinkedList<Queue<Double>> evenQueues = new LinkedList<>();
            LinkedList<Queue<Double>> oddQueues = new LinkedList<>();
            distributeToEvenOddQueues(inputQueues, evenQueues, oddQueues, eoCapacity);

            double result = evaluateExpression(expression);

            System.out.println("\n✅ Result = " + result);

            printQueueList("Even Number Queues", evenQueues);
            printQueueList("Odd Number Queues", oddQueues);
            System.out.println("\n📊 Queue Summary:");
            System.out.println("Input Queues Created: " + inputQueues.size());
            System.out.println("Even Queues Created: " + evenQueues.size());
            System.out.println("Odd Queues Created: " + oddQueues.size());

            System.out.print("\nDo you want to try again? (yes/no): ");
            String answer = sc.nextLine().trim().toLowerCase();
            if (!answer.equals("yes")) {
                System.out.println("👋 Goodbye!");
                break;
            }
        }

        sc.close();
    }
}