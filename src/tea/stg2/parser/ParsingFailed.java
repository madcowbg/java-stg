package tea.stg2.parser;

public class ParsingFailed extends Exception {
    public ParsingFailed(String msg) {
        super(msg);
    }

    public ParsingFailed(Exception e) {
        super(e);
    }
}
