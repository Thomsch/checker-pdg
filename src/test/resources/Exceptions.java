import java.util.List;
import java.util.Optional;

class Exceptions {

    void oneCatch(int x) {
        int a = x;
        try {
            int b = a / x;
        } catch (ArithmeticException e) {
            int c = x;
        }
    }

    void justThrow() {
        throw new RuntimeException("Just throw");
    }

    void multipleCatches(int x) {
        int a = x;
        try {
            int b = a / x;
            List l = null;
            l.size();
        } catch (ArithmeticException e) {
            int c = 1;
        } catch (NullPointerException e) {
            int c = 2;
        } finally {
            int d = 4;
        }
    }


    void uncaughtException() {
        try {
            List l = null;
            l.size();
        } catch (NullPointerException e) {
            int a = 5 / 0; // ArithmeticException
        } finally {
           int d = 0;
        }
        int e = 0; // Only if no exception is thrown in the catch
    }

    void cascadingException() {
        try {
            List l = null;
            l.size();
        } catch (NullPointerException e) {
            try {
                int a = 5 / 0;
            } catch (Exception e2) {
                int b = 3;
            } finally {
                int a = 2;
            }
        } finally {
            int a = 3;
        }
    }
}
