import java.util.List;
import java.util.Optional;

class AnonymousMethods {
    void anonymousMethod(Integer x) {
        Integer a = x;
        Optional<Integer> optX = Optional.of(x);
        optX.ifPresent(n -> System.out.println(n));
        Integer b = optX.get();
    }
}
