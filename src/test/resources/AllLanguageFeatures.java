import java.util.ArrayList;
import java.util.List;

public abstract class AllLanguageFeatures {

    // Class variable
    private static int classVariable = 10;

    // Instance variable
    private int instanceVariable = 20;

    // Constructor
    public AllLanguageFeatures() {
        // Constructor body
    }

    // Method with parameters and return type
    public int addNumbers(int num1, int num2) {
        return num1 + num2;
    }

    // Method with no parameters and return type
    public void printHello() {
        System.out.println("Hello!");
    }

    // Method with exception handling
    public void readFile(String fileName) {
        try {
            // Code to read file
        } catch (Exception e) {
            // Exception handling
        }
    }

    // Method with loops and conditional statements
    public void printEvenNumbers(int limit) {
        for (int i = 0; i <= limit; i++) {
            if (i % 2 == 0) {
                System.out.println(i);
            }
        }
    }

    // Method with generics
    public <T> void printList(List<T> list) {
        for (T item : list) {
            System.out.println(item);
        }
    }

    // Method with varargs
    public void printNumbers(int... numbers) {
        for (int number : numbers) {
            System.out.println(number);
        }
    }

    // Method with lambda expression
    public List<Integer> filterEvenNumbers(List<Integer> numbers) {
        List<Integer> evenNumbers = new ArrayList<>();
        numbers.forEach(num -> {
            if (num % 2 == 0) {
                evenNumbers.add(num);
            }
        });
        return evenNumbers;
    }

    // Method with enhanced for loop
    public int sumArray(int[] array) {
        int sum = 0;
        for (int num : array) {
            sum += num;
        }
        return sum;
    }

    // Method with enum
    public enum DaysOfWeek {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }

    // Method with inner class
    public class InnerClass {
        // Inner class body
    }

    // Method with private class
    private static class PrivateClass {
        // Private class body
    }

    // Method with static block
    static {
        // Static block code
    }

    // Method with synchronized keyword
    public synchronized void synchronizedMethod() {
        // Synchronized method body
    }

    // Method with final keyword
    public final void finalMethod() {
        // Final method body
    }

    // Method with abstract keyword
    public abstract void abstractMethod();

    // Method with native keyword
    public native void nativeMethod();

    // Method with strictfp keyword
    public strictfp void strictfpMethod() {
        // strictfp method body
    }

    // Method with default keyword (interface default method)
    public interface DefaultMethodInterface {
        default void defaultMethod() {
            // Default method body
        }
    }

    // Method using a local class
    public void processNumbers(List<Integer> numbers) {
        class NumberProcessor {
            public void process(List<Integer> nums) {
                for (int num : nums) {
                    System.out.println("Processed number: " + num);
                }
            }
        }

        NumberProcessor processor = new NumberProcessor();
        processor.process(numbers);
    }

    // Method with while loop
    public void countUpTo(int limit) {
        int i = 1;
        while (i <= limit) {
            System.out.println(i);
            i++;
        }
    }

    // Method with switch case
    public String getDayOfWeek(int day) {
        String dayOfWeek;
        switch (day) {
            case 1:
                dayOfWeek = "Sunday";
                break;
            case 2:
                dayOfWeek = "Monday";
                break;
            case 3:
                dayOfWeek = "Tuesday";
                break;
            case 4:
                dayOfWeek = "Wednesday";
                break;
            case 5:
                dayOfWeek = "Thursday";
                break;
            case 6:
                dayOfWeek = "Friday";
                break;
            case 7:
                dayOfWeek = "Saturday";
                break;
            default:
                dayOfWeek = "Invalid day";
        }
        return dayOfWeek;
    }
}
