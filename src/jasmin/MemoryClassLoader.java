package jasmin;

import java.util.Map;

public class MemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    public MemoryClassLoader(Map<String, byte[]> classes) {
        this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }
}
