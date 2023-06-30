import java.util.List;

class Loops {

    // Simple loop
    void basicLoop(int x) {
        int a = 0;
        for (int i = 0; i < x; x++) {
            a += x;
        }
    }

    // Loop with a conditional statement
    public void printEvenNumbers(int limit) {
        for (int i = 0; i <= limit; i++) {
            if (i % 2 == 0) {
                System.out.println(i);
            }
        }
    }

    // While loop.
    public void countUpTo(int limit) {
        int i = 1;
        while (i <= limit) {
            i++;
        }
    }

    // Enhanced for loop with list.
    public void printList(List<String> strings) {
        for (String string : strings) {
            System.out.println(string);
        }
    }

    // Enhanced for loop with array
    public int sumArray(int[] array) {
        int sum = 0;
        for (int num : array) {
            sum += num;
        }
        return sum;
    }

    // Infinite loop
    public void infiniteLoop() {
        while (true) {
            System.out.println("Hello");
        }
    }

    // Method with varargs
    public void printNumbers(int... numbers) {
        for (int number : numbers) {
            System.out.println(number);
        }
    }

}
