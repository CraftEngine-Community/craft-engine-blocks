package cn.gtemc.craftengine.classpath.impl;

import cn.gtemc.craftengine.classpath.ClassPathAppender;
import cn.gtemc.craftengine.classpath.URLClassLoaderAccess;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class ReflectionClassPathAppender implements ClassPathAppender {
    private final URLClassLoaderAccess classLoaderAccess;

    public ReflectionClassPathAppender(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            this.classLoaderAccess = URLClassLoaderAccess.create(urlClassLoader);
        } else {
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
        }
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            this.classLoaderAccess.addURL(file.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
