package compiler;

import org.mdkt.compiler.CompiledCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adapted from {@link org.mdkt.compiler.DynamicClassLoader}.
 * Added functionality to remove classes from loader.
 */
public class DynamicClassLoader extends org.mdkt.compiler.DynamicClassLoader {
    private Map<String, CompiledCode> customCompiledCode = new HashMap();
    private Set<String> removedClasses = new HashSet<>();

    public DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addCode(CompiledCode cc) {
        this.customCompiledCode.put(cc.getName(), cc);
        this.removedClasses.remove(cc.getName());
    }

    public void removeClass(String className) {
        customCompiledCode.remove(className);
        removedClasses.add(className);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(removedClasses.contains(name)) {
            throw new ClassNotFoundException("Class \"" + name + "\" was removed from classloader");
        }
        return super.loadClass(name, resolve);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if(removedClasses.contains(name)) {
            throw new ClassNotFoundException("Class \"" + name + "\" was removed from classloader");
        }
        CompiledCode cc = (CompiledCode)this.customCompiledCode.get(name);
        if (cc == null) {
            return super.findClass(name);
        } else {
            byte[] byteCode = cc.getByteCode();
            return this.defineClass(name, byteCode, 0, byteCode.length);
        }
    }
}