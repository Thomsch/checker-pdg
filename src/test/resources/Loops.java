import java.util.List;

class Loops {

    // // Method with for loop
    // void basicLoop(int x) {
    //     int a = 0;
    //     for (int i = 0; i < x; x++) {
    //         a += x;
    //     }
    // }
    //
    // // Method with loops and conditional statements
    // public void printEvenNumbers(int limit) {
    //     for (int i = 0; i <= limit; i++) {
    //         if (i % 2 == 0) {
    //             System.out.println(i);
    //         }
    //     }
    // }
    //
    // // Method with while loop
    // public void countUpTo(int limit) {
    //     int i = 1;
    //     while (i <= limit) {
    //         i++;
    //     }
    // }

    // Method with an enhanced for loop.
    // public void printList(List<String> strings) {
    //     for (String string : strings) {
    //         System.out.println(string);
    //     }
    // }

    // Method with enhanced for loop
    // 'array' should hold all the iteration things
    public int sumArray(int[] array) {
        int sum = 0;
        for (int num : array) {
            sum += num;
        }
        return sum;
    }

    // // Method with varargs
    // public void printNumbers(int... numbers) {
    //     for (int number : numbers) {
    //         System.out.println(number);
    //     }
    // }
    //


}
