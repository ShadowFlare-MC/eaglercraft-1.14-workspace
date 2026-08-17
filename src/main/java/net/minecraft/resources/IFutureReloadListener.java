package net.minecraft.resources;

import net.deltaclient.CompletableFuture;
import net.minecraft.profiler.IProfiler;

import java.util.concurrent.Executor;

public interface IFutureReloadListener {
    CompletableFuture<Void> reload(IFutureReloadListener.IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor);

    public interface IStage {
        <T> CompletableFuture<T> markCompleteAwaitingOthers(T backgroundResult);
    }
}
