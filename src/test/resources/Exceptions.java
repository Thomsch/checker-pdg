import java.util.List;
import java.util.Optional;

class Exceptions {

    void exceptionTest(int x) {
        int a = x;

        try {
            int b = a / x;
        } catch (Exception e) {
            int c = x;
        }
    }

    // void justThrow() {
    //     throw new RuntimeException("Just throw");
    // }
}
