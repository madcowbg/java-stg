package compilation;

public class SourceBuilder {
    protected final StringBuilder source = new StringBuilder();

    public byte[] byteArray() {
        return source.toString().getBytes();
    }
}
