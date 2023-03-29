package compiler;

import org.mdkt.compiler.CompiledCode;

import java.util.*;

/**
 * Adapted from {@link org.mdkt.compiler.DynamicClassLoader}.
 * Added functionality to remove classes from loader.
 */
public class DynamicClassLoader extends ClassLoader {
    private final Map<String, CompiledCode> customCompiledCode = new HashMap<>();
    private Set<String> removedClasses = new HashSet<>();

    public DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addCode(CompiledCode cc) {
        this.customCompiledCode.put(cc.getName(), cc);
        this.removedClasses.remove(cc.getName());
    }

    public void removeFiles(Collection<String> classNames) {
        classNames.forEach(this.customCompiledCode::remove);
        this.removedClasses = new HashSet<>(classNames);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (removedClasses.contains(name)) {
            throw new ClassNotFoundException("Class \"" + name + "\" was removed from classloader");
        }
        return super.loadClass(name, resolve);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (removedClasses.contains(name)) {
            throw new ClassNotFoundException("Class \"" + name + "\" was removed from classloader");
        }
        CompiledCode cc = this.customCompiledCode.get(name);
        if (cc == null) {
            return super.findClass(name);
        } else {
            byte[] byteCode = cc.getByteCode();
            return this.defineClass(name, byteCode, 0, byteCode.length);
        }
    }
}