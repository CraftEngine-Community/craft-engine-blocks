package cn.gtemc.craftengine;

import cn.gtemc.craftengine.block.BlockBehaviors;
import cn.gtemc.craftengine.classpath.ClassPathAppender;
import cn.gtemc.craftengine.classpath.impl.ReflectionClassPathAppender;
import cn.gtemc.craftengine.dependency.Dependencies;
import cn.gtemc.craftengine.dependency.DependencyManager;
import cn.gtemc.craftengine.dependency.DependencyManagerImpl;
import cn.gtemc.craftengine.entity.seat.SeatManager;
import cn.gtemc.craftengine.injector.PlaceBlockBlockPlaceContextGenerator;
import cn.gtemc.craftengine.item.ItemManager;
import cn.gtemc.craftengine.item.ItemProcessors;
import cn.gtemc.craftengine.item.behavior.ItemBehaviors;
import cn.gtemc.craftengine.item.settings.ItemSettings;
import cn.gtemc.craftengine.plugin.context.event.EventFunctions;
import cn.gtemc.craftengine.scheduler.JavaScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class CraftEngineBlocks extends JavaPlugin {
    private static CraftEngineBlocks instance;
    private JavaScheduler scheduler;
    private ClassPathAppender classPathAppender;
    private DependencyManager dependencyManager;
    private SeatManager seatManager;
    private ItemManager itemManager;

    @Override
    public void onLoad() {
        instance = this;
        initPlugin();
    }

    @Override
    public void onEnable() {
        this.seatManager.reload();
        this.itemManager.reload();
        this.scheduler.sync().runDelayed(() -> {
            this.seatManager.delayedInit();
            this.itemManager.delayedInit();
        });
    }

    @Override
    public void onDisable() {
        if (this.seatManager != null) this.seatManager.disable();
        if (this.itemManager != null) this.itemManager.disable();
        if (this.scheduler != null) this.scheduler.shutdownScheduler();
        if (this.scheduler != null) this.scheduler.shutdownExecutor();
        if (this.dependencyManager != null) this.dependencyManager.close();
    }

    private void initPlugin() {
        this.scheduler = new JavaScheduler(this);
        this.classPathAppender = new ReflectionClassPathAppender(this.getClass().getClassLoader());
        initDependencyManager();
        PlaceBlockBlockPlaceContextGenerator.init();
        BlockBehaviors.register();
        ItemBehaviors.register();
        EventFunctions.register();
        ItemSettings.register();
        ItemProcessors.register();
        this.seatManager = new SeatManager(this);
        this.itemManager = new ItemManager(this);
    }

    private void initDependencyManager() {
        this.dependencyManager = new DependencyManagerImpl(this);
        this.dependencyManager.loadDependencies(Set.of(
                Dependencies.BYTE_BUDDY,
                Dependencies.GSON
        ));
    }

    public JavaScheduler scheduler() {
        return this.scheduler;
    }

    public ClassPathAppender classPathAppender() {
        return this.classPathAppender;
    }

    public DependencyManager dependencyManager() {
        return this.dependencyManager;
    }

    public static CraftEngineBlocks instance() {
        return instance;
    }
}
