package teaselib.core.ai;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

class ComputeService extends NamedExecutorService.SameThread {

    final ComputeContext context;

    ComputeService() {
        super(TeaseLibAI.class.getSimpleName());
        ComputeContext newContext;
        try {
            newContext = submit(ComputeContext::newInstance).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            newContext = ComputeContext.None;
        } catch (ExecutionException e) {
            throw asRuntimeException(ExceptionUtil.reduce(e));
        }
        context = newContext;
    }

    @Override
    public void shutdown() {
        submit(context::close);
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

}