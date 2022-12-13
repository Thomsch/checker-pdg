import java.util.List;
import java.util.Optional;

class DataFlowTests {

    void join() {
        int a = 0;

        if (a == 0) {
            int b = a;
        } else {
            int c = a;
        }

        int d = a;
    }
}
