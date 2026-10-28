package utd.aos.p2.chan;

import java.util.Optional;
import java.util.function.Consumer;

public interface Chan<T> {
    Optional<T> pull();

    void push(T t);

    void registerCallback(Consumer<T> cb);
}
