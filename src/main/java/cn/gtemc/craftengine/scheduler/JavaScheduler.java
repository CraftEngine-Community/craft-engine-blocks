package cn.gtemc.craftengine.scheduler;

import cn.gtemc.craftengine.CraftEngineBlocks;
import cn.gtemc.craftengine.scheduler.impl.AsyncTask;
import cn.gtemc.craftengine.scheduler.impl.BukkitExecutor;
import cn.gtemc.craftengine.scheduler.impl.FoliaExecutor;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.World;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class JavaScheduler {
    private static final int PARALLELISM = 16;
    private final CraftEngineBlocks plugin;
    private final ScheduledThreadPoolExecutor scheduler;
    private final ForkJoinPool worker;
    private final RegionExecutor<World> sync;

    public JavaScheduler(CraftEngineBlocks plugin) {
        this.plugin = plugin;
        this.scheduler = new ScheduledThreadPoolExecutor(4, r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("craft-engine-blocks-extensions-scheduler");
            return thread;
        });
        this.scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.worker = new ForkJoinPool(PARALLELISM, new WorkerThreadFactory(), new ExceptionHandler(), false);
        if (VersionHelper.hasFoliaPatch) {
            this.sync = new FoliaExecutor(plugin);
        } else {
            this.sync = new BukkitExecutor(plugin);
        }
    }

    public Executor async() {
        return this.worker;
    }

    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = this.scheduler.schedule(() -> this.worker.execute(task), delay, unit);
        return new AsyncTask(future);
    }

    public SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit) {
        ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(() -> this.worker.execute(task), delay, interval, unit);
        return new AsyncTask(future);
    }

    public RegionExecutor<World> sync() {
        return this.sync;
    }

    public void shutdownScheduler() {
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                this.plugin.getLogger().severe("Timed out waiting for the CraftEngine scheduler to terminate");
                reportRunningTasks(thread -> thread.getName().equals("craft-engine-blocks-extensions-scheduler"));
            }
        } catch (InterruptedException e) {
            this.plugin.getLogger().log(Level.WARNING, "Thread is interrupted", e);
        }
    }

    public void shutdownExecutor() {
        this.worker.shutdown();
        try {
            if (!this.worker.awaitTermination(1, TimeUnit.MINUTES)) {
                this.plugin.getLogger().severe("Timed out waiting for the CraftEngine worker thread pool to terminate");
                reportRunningTasks(thread -> thread.getName().startsWith("craft-engine-blocks-extensions-worker-"));
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.WARNING, "Thread is interrupted", e);
        }
    }

    private void reportRunningTasks(Predicate<Thread> predicate) {
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (predicate.test(thread)) {
                this.plugin.getLogger().warning("Thread " + thread.getName() + " is blocked, and may be the reason for the slow shutdown!\n" +
                        Arrays.stream(stack).map(el -> "  " + el).collect(Collectors.joining("\n"))
                );
            }
        });
    }

    private static final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setDaemon(true);
            thread.setName("craft-engine-blocks-extensions-worker-" + COUNT.getAndIncrement());
            return thread;
        }
    }

    private final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            JavaScheduler.this.plugin.getLogger().log(Level.WARNING, "Thread " + t.getName() + " threw an uncaught exception", e);
        }
    }
}
