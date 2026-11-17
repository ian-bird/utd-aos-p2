package utd.aos.p2.mutex;

public final class SendReleaseTo implements MutexData {
    public int where;

    public String toString() {
        return "{\"type\": \"SendReleaseTo\", \"where\": " + where + "}";
    }
    
    public SendReleaseTo(int where) {
        this.where = where;
    }
}
