import java.util.Scanner;

public class ArrCalculator5  {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of values: ");
        int n = sc.nextInt();
        if (n <= 0) {
            System.out.println("Invalid input! n must be > 0.");
            sc.close();
            return;
        }

        double[] arr = new double[n];
        System.out.println("Enter " + n + " numbers:");
        for (int i = 0; i < n; i++) {
            System.out.print("  [" + (i + 1) + "]: ");
            arr[i] = sc.nextDouble();
        }

        double sum = 0, prod = 1;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (double v : arr) {
            sum += v;
            prod *= v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        double avg = sum / n;

        System.out.println("\nResults:");
        System.out.println("  Sum     = " + sum);
        System.out.println("  Average = " + avg);
        System.out.println("  Minimum = " + min);
        System.out.println("  Maximum = " + max);
        System.out.println("  Product = " + prod);

        sc.close();
    }
}
