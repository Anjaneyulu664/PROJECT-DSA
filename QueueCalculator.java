import java.util.*;

public class QueueCalculator {
    public static int preced(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }
    public static double applyOp(double a, double b, char op) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            default:  return 0;
        }
    }
    public static boolean isBalanced(String expr) {
        int open = 0, close = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') open++;
            else if (c == ')') close++;
        }
        return open == close;
    }
    public static String fixParentheses(String expr, Scanner sc) {
        int open=0, close=0;
        for (char c : expr.toCharArray()) {
            if (c=='(') open++; else if (c==')') close++;
        }
        if (open>close) {
            System.out.print("Missing ')'. Insert at index: ");
            int pos = sc.nextInt(); sc.nextLine();
            expr = expr.substring(0,pos) + ")" + expr.substring(pos);
        } else if (close>open) {
            System.out.print("Missing '('. Insert at index: ");
            int pos = sc.nextInt(); sc.nextLine();
            expr = expr.substring(0,pos) + "(" + expr.substring(pos);
        }
        return expr;
    }

    public static void addToQ(LinkedList<Queue<Double>> list, double num, int cap) {
        if (list.isEmpty() || list.getLast().size() >= cap)
            list.add(new LinkedList<>());
        list.getLast().add(num);
    }
    public static void extractNums(String expr, LinkedList<Queue<Double>> inQs, int cap) {
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                double num = 0;
                while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                    num = num * 10 + (expr.charAt(i) - '0');
                    i++;
                }
                i--;
                addToQ(inQs, num, cap);
            }
        }
    }
    public static void distribute(LinkedList<Queue<Double>> inQs,
                                  LinkedList<Queue<Double>> evenQs,
                                  LinkedList<Queue<Double>> oddQs,
                                  int cap) {
        for (Queue<Double> q : inQs) {
            for (double num : q) {
                if ((int)num % 2 == 0)
                    addToQ(evenQs, num, cap);
                else
                    addToQ(oddQs, num, cap);
            }
        }
    }
    public static double evaluate(String expr) {
        LinkedList<Double> vals = new LinkedList<>();
        LinkedList<Character> ops = new LinkedList<>();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == ' ') continue;
            if (Character.isDigit(c)) {
                double num = 0;
                while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                    num = num * 10 + (expr.charAt(i) - '0');
                    i++;
                }
                i--;
                vals.add(num);
            } else if (c == '(') {
                ops.add(c);
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.getLast() != '(') {
                    double b = vals.removeLast();
                    double a = vals.removeLast();
                    char op = ops.removeLast();
                    vals.add(applyOp(a, b, op));
                }
                if (!ops.isEmpty()) ops.removeLast();
            } else {
                while (!ops.isEmpty() && preced(ops.getLast()) >= preced(c)) {
                    double b = vals.removeLast();
                    double a = vals.removeLast();
                    char op = ops.removeLast();
                    vals.add(applyOp(a, b, op));
                }
                ops.add(c);
            }
        }
        while (!ops.isEmpty()) {
            double b = vals.removeLast();
            double a = vals.removeLast();
            char op = ops.removeLast();
            vals.add(applyOp(a, b, op));
        }
        return vals.removeLast();
    }
    public static void printQueues(String label, LinkedList<Queue<Double>> qs) {
        System.out.println(label + ":");
        int i = 1;
        for (Queue<Double> q : qs) {
            System.out.println("  Q" + (i++) + " => " + q);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("📦 Welcome to QueueCalculator!");

        System.out.print("Enter capacity for input queue: ");
        int inputCap = sc.nextInt();
        System.out.print("Enter capacity for even/odd queues: ");
        int eoCap = sc.nextInt();
        sc.nextLine();

        outer:
        while (true) {
            System.out.print("\nEnter expression: ");
            String expr = sc.nextLine();

            if (!isBalanced(expr)) {
                expr = fixParentheses(expr, sc);
                System.out.println("Fixed: " + expr);
            }

            LinkedList<Queue<Double>> inQs = new LinkedList<>();
            extractNums(expr, inQs, inputCap);
            LinkedList<Queue<Double>> evenQs = new LinkedList<>();
            LinkedList<Queue<Double>> oddQs = new LinkedList<>();
            distribute(inQs, evenQs, oddQs, eoCap);
            double result = evaluate(expr);

            System.out.println("\n✅ Result = " + result);

            boolean inMenu = true;
            while (inMenu) {
                System.out.println("\nMenu:");
                System.out.println("1. Show Input Queues");
                System.out.println("2. Show Even Number Queues");
                System.out.println("3. Show Odd Number Queues");
                System.out.println("4. Show All");
                System.out.println("5. Display Result Again");
                System.out.println("0. New Expression / Exit");
                System.out.print("Choice: ");
                int choice = sc.nextInt(); sc.nextLine();

                switch (choice) {
                    case 0:
                        inMenu = false;
                        break;
                    case 1:
                        printQueues("Input Queues", inQs);
                        break;
                    case 2:
                        printQueues("Even Queues", evenQs);
                        break;
                    case 3:
                        printQueues("Odd Queues", oddQs);
                        break;
                    case 4:
                        printQueues("Input Queues", inQs);
                        printQueues("Even Queues", evenQs);
                        printQueues("Odd Queues", oddQs);
                        break;
                    case 5:
                        System.out.println("Result = " + result);
                        break;
                    default:
                        System.out.println("Invalid - try again.");
                }
            }

            System.out.print("\nAnother expression? (yes/no): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("yes")) {
                System.out.println("👋 Goodbye!");
                break outer;
            }
        }

        sc.close();
    }
}
