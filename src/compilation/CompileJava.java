package compilation;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CompileJava {

    public static byte[] compile(String className, InputStream content) throws IOException {
        JavaCompiler compiler =
                ToolProvider.getSystemJavaCompiler();
        ClassFileManager manager = new ClassFileManager(
                compiler.getStandardFileManager(null, null, null));
        List<CharSequenceJavaFileObject> files = new ArrayList<>();
        files.add(new CharSequenceJavaFileObject(className, new String(content.readAllBytes(), StandardCharsets.UTF_8)));
        compiler.getTask(null, manager, null, null, null, files)
                .call();

        return manager.o.getBytes();
    }

    // These are some utility classes needed for the JavaCompiler
    // ----------------------------------------------------------
    static final class JavaFileObject
            extends SimpleJavaFileObject {
        final ByteArrayOutputStream os =
                new ByteArrayOutputStream();
        JavaFileObject(String name, JavaFileObject.Kind kind) {
            super(URI.create(
                    "string:///"
                            + name.replace('.', '/')
                            + kind.extension),
                    kind);
        }
        byte[] getBytes() {
            return os.toByteArray();
        }
        @Override
        public OutputStream openOutputStream() {
            return os;
        }
    }

    static final class ClassFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {
        JavaFileObject o;
        ClassFileManager(StandardJavaFileManager m) {
            super(m);
        }
        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location,
                String className,
                JavaFileObject.Kind kind,
                FileObject sibling
        ) {
            return o = new JavaFileObject(className, kind);
        }
    }

    static final class CharSequenceJavaFileObject
            extends SimpleJavaFileObject {
        final CharSequence content;
        public CharSequenceJavaFileObject(
                String className,
                CharSequence content
        ) {
            super(URI.create(
                    "string:///"
                            + className.replace('.', '/')
                            + JavaFileObject.Kind.SOURCE.extension),
                    JavaFileObject.Kind.SOURCE);
            this.content = content;
        }
        @Override
        public CharSequence getCharContent(
                boolean ignoreEncodingErrors
        ) {
            return content;
        }
    }
}