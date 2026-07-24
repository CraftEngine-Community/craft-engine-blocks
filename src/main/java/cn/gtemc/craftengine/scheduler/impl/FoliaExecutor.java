package cn.gtemc.craftengine.scheduler.impl;

import cn.gtemc.craftengine.CraftEngineBlocks;
import cn.gtemc.craftengine.scheduler.RegionExecutor;
import cn.gtemc.craftengine.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class FoliaExecutor implements RegionExecutor<World> {
    private final CraftEngineBlocks plugin;

    public FoliaExecutor(CraftEngineBlocks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable runnable, World world, int x, int z) {
        Bukkit.getRegionScheduler().execute(this.plugin, world, x, z, runnable);
    }

    @Override
    public void run(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin, runnable);
    }

    @Override
    public void runDelayed(Runnable runnable, World world, int x, int z) {
        run(runnable, world, x, z);
    }

    @Override
    public void runDelayed(Runnable runnable) {
        run(runnable);
    }

    @Override
    public SchedulerTask runAsyncRepeating(Runnable runnable, long delay, long period) {
        return runRepeating(runnable, delay, period);
    }

    @Override
    public SchedulerTask runAsyncLater(Runnable runnable, long delay) {
        return runLater(runnable, delay);
    }

    @Override
    public SchedulerTask runLater(Runnable runnable, long delay) {
        if (delay <= 0) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().run(this.plugin, scheduledTask -> runnable.run()));
        } else {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, scheduledTask -> runnable.run(), delay));
        }
    }

    @Override
    public SchedulerTask runLater(Runnable runnable, long delay, World world, int x, int z) {
        if (delay <= 0) {
            return new FoliaTask(Bukkit.getRegionScheduler().run(this.plugin, world, x, z, scheduledTask -> runnable.run()));
        } else {
            return new FoliaTask(Bukkit.getRegionScheduler().runDelayed(this.plugin, world, x, z, scheduledTask -> runnable.run(), delay));
        }
    }

    @Override
    public SchedulerTask runRepeating(Runnable runnable, long delay, long period, World world, int x, int z) {
        return new FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, world, x, z, scheduledTask -> runnable.run(), delay, period));
    }

    @Override
    public SchedulerTask runRepeating(Runnable runnable, long delay, long period) {
        return new FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin, scheduledTask -> runnable.run(), delay, period));
    }

    @Override
    public void execute(@NotNull Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin, runnable);
    }
}
