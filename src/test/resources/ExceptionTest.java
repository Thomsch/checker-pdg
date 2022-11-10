import java.util.List;

class Test {

    void exceptionTest(int x) {
        int a = x;

        try {
            int b = a / x;
        } catch (Exception e) {
            int c = x;
        }
    }
}
