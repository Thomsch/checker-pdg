import java.util.List;
import java.util.Optional;

class AnonymousClass {

    void anonymousClass() {
        int a = 0;
        Runnable r = new Runnable() {
            public void run() {
                int b = a;
            }
        };
        r.run();
    }
}
