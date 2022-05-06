package com.c12e.fabric;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * Java agent to add jar to classpath at runtime
 */
public class JvmClasspathAgent {

    public static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation instrumentation) {
        JvmClasspathAgent.instrumentation = instrumentation;
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        JvmClasspathAgent.instrumentation = instrumentation;
    }

    public static void appendJarFile(JarFile file) throws IOException {
        if (instrumentation != null) {
            instrumentation.appendToSystemClassLoaderSearch(file);
        }
    }
}