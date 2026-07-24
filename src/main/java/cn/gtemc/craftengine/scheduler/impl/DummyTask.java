package cn.gtemc.craftengine.scheduler.impl;

import cn.gtemc.craftengine.scheduler.SchedulerTask;

public final class DummyTask implements SchedulerTask {

    @Override
    public void cancel() {
    }

    @Override
    public boolean cancelled() {
        return true;
    }
}
