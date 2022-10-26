import org.checkerframework.checker.codechanges.qual.*;

// Test basic subtyping relationships for the Dataflow Checker.
class Test {
    void test(int x, int y) {
        int a = x;
        int b = y;

        if (a == 3) {
            a = y;
            y++;
        }

        b = x;
        a = y;
    }
}
