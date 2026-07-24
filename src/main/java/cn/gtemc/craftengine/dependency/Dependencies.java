package cn.gtemc.craftengine.dependency;

import cn.gtemc.craftengine.dependency.relocation.Relocation;

import java.util.Collections;
import java.util.List;

public final class Dependencies {

    public static final Dependency ASM = new Dependency(
            "asm",
            "org.ow2.asm",
            "asm",
            Collections.emptyList()
    );

    public static final Dependency ASM_COMMONS = new Dependency(
            "asm-commons",
            "org.ow2.asm",
            "asm-commons",
            Collections.emptyList()
    );

    public static final Dependency JAR_RELOCATOR = new Dependency(
            "jar-relocator",
            "me.lucko",
            "jar-relocator",
            Collections.emptyList()
    );

    public static final Dependency BYTE_BUDDY = new Dependency(
            "byte-buddy",
            "net{}bytebuddy",
            "byte-buddy",
            List.of(Relocation.of("bytebuddy", "net{}bytebuddy"))
    );

    public static final Dependency GSON = new Dependency(
            "gson",
            "com.google.code.gson",
            "gson",
            Collections.emptyList()
    );
}
