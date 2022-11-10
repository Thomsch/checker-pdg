import org.checkerframework.checker.codechanges.qual.*;
import java.util.List;

// Test basic subtyping relationships for the Dataflow Checker.
class Test {

    float f = 3;

    void bar(int w, int z) {
        int omega = w + z;
    }

    int test(int x, int y, List<String> o) {
        int a = x;
        int b = y;
        List<String> oh = o;

        //bar(a, b); // Unsupported.
        // This is a comment
        if (a == 3 || a == 2) {
            if(b < 0) {
                boolean c = true && false;
            }

            a = y;
            y++;
        }

        b = x;
        a = y;
        return a + b;
    }
}
