import org.checkerframework.checker.codechanges.qual.*;

// Test basic subtyping relationships for the Dataflow Checker.
class Test {

    float f = 3;

    void test(int x, int y) {
        int a = x;
        int b = y;

        // This is a comment
        if (a == 3) {
            a = y;
            y++;
        }

        b = x;
        a = y;
    }
}