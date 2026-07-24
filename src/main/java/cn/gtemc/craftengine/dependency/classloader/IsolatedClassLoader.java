package cn.gtemc.craftengine.dependency.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public final class IsolatedClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }
}
