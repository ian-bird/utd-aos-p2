package utd.aos.p2.clock;

public interface Clock<C extends Clock<C>> {
    C receive(C c);
    C send();
    boolean covers(C c);
    Comparison compare(C c);
}
