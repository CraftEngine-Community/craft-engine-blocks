package cn.gtemc.craftengine.scheduler.impl;

import cn.gtemc.craftengine.CraftEngineBlocks;
import cn.gtemc.craftengine.scheduler.RegionExecutor;
import cn.gtemc.craftengine.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class BukkitExecutor implements RegionExecutor<World> {
    private final CraftEngineBlocks plugin;

    public BukkitExecutor(CraftEngineBlocks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable runnable, World world, int x, int z) {
        execute(runnable);
    }

    @Override
    public void runDelayed(Runnable runnable, World world, int x, int z) {
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public SchedulerTask runAsyncRepeating(Runnable runnable, long delay, long period) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, runnable, delay, period));
    }

    @Override
    public SchedulerTask runAsyncLater(Runnable runnable, long delay) {
        return new BukkitTask(Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, delay));
    }

    @Override
    public SchedulerTask runLater(Runnable runnable, long delay, World world, int x, int z) {
        if (delay <= 0) {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
                return new DummyTask();
            } else {
                return new BukkitTask(Bukkit.getScheduler().runTask(this.plugin, runnable));
            }
        }
        return new BukkitTask(Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delay));
    }

    @Override
    public SchedulerTask runRepeating(Runnable runnable, long delay, long period, World world, int x, int z) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, delay, period));
    }

    @Override
    public void execute(@NotNull Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }
}
