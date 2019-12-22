package jasmin;

public class JASMParsingFailed extends Exception {
    public JASMParsingFailed(Exception e) {
        super(e);
    }

    public JASMParsingFailed(String msg) {
        super(msg);
    }
}
