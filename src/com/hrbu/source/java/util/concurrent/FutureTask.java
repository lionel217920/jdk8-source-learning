/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation. 可取消的异步计算。
 * This class provides a base implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and retrieve the result of the computation.
 * 该类提供了Future的基础实现，并提供了开始和取消计算的方法，查询计算是否完成，检索计算的结果。
 * The result can only be retrieved when the computation has completed; 只有在计算完成后才能检索结果；
 * the {@code get} methods will block if the computation has not yet completed. {@code get}方法在计算没有完成前会一直阻塞。
 * Once the computation has completed, the computation cannot be restarted or cancelled
 * (unless the computation is invoked using {@link #runAndReset}).
 * 一旦计算结果完成，这个计算就不能被重新开始或者取消（除非使用#runAndReset方法调用计算）。
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or {@link Runnable} object.
 * Because {@code FutureTask} implements{@code Runnable}, a {@code FutureTask} can be submitted to an {@link Executor} for execution.
 * FutureTask 可用于包装 Callable 和 Runnable 类。
 * 因为实现了Runnalbe接口，FutureTask 可以提交到 Executor执行。
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating customized task classes.
 * 除了可以作为一个单独的类，还提供了受保护的功能？？？，在创建自定义任务的时候可能很有用。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this class that relied on AbstractQueuedSynchronizer,
     * mainly to avoid surprising users about retaining interrupt status during cancellation races.
     * 修订说明：不同于以前版本的是，以前版本依赖于AQS，主要是为了避免在取消竞争时保留中断状态这种意外发生。
     * Sync control in the current design relies on a "state" field updated via CAS to track completion,
     * along with a simple Treiber stack to hold waiting threads.
     * 当前版本设计的同步控制依赖于，使用CAS更新的state状态来跟踪完成情况。
     *
     * Style note: As usual, we bypass overhead of using AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     * 样式说明：通常情况下，我们绕过使用原子更新，取而替代的是使用Unsafe内联函数。
     */

    /**
     * The run state of this task, initially NEW. 此任务的运行状态，最初的状态是NEW。
     * The run state transitions to a terminal state only in methods set, setException, and cancel.
     * 运行状态仅在set、setException 和 cancel方法中转变到终端状态。
     * During completion, state may take on transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a cancel(true)).
     * 在完成过程中，状态可能具有完成（当结果被设置）或者中断（正在执行的满足取消）的瞬时值。 ？？？？
     * Transitions from these intermediate to final states use cheaper ordered/lazy writes because values are unique and cannot be further modified.
     * 从这些中间状态过渡到最终状态使用更便宜的有序或者懒惰的写入，因为值是独一无二的，无法进一步修改。？？？？
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /** The underlying callable; nulled out after running */
    private Callable<V> callable;
    /** The result to return or exception to throw from get() */
    private Object outcome; // non-volatile, protected by state reads/writes  这里为什么不适用volatile是因为happen before原则。
    /** The thread running the callable; CASed during run() */
    private volatile Thread runner; // 当前执行的线程，通过CAS设置
    /** Treiber stack of waiting threads 等待的线程都在这里 */
    private volatile WaitNode waiters;

    /**
     * Returns result or throws exception for completed task.
     * 返回结果，对于已完成（注释好像不对吧，大于已取消的状态吗？？）的任务会抛出异常。
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) // 等于完成状态就返回
            return (V)x;
        if (s >= CANCELLED) // 取消或者中断状态会抛出异常
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the given {@code Callable}.
     * 构造方法：创建一个任务，该任务在运行后将执行给定的 Callable方法。
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the given {@code Runnable},
     * and arrange that {@code get} will return the given result on successful completion.
     * 创建一个FutureTask: 运行时执行给定的Runnable，get方法会将在成功完成后返回给定的结果。
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {    // in case call to interrupt throws exception
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null) // 给执行的线程发出中断信号，告诉它应该中断了。
                        t.interrupt(); // 如果线程处于被阻塞状态，会立即退出阻塞状态并抛出InterruptedException，仅此而已。
                } finally { // 如果线程处于正常活动状态，会设置该线程的中断标识为true，仅此而已。
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED); // final state
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }

    /**
     * Protected method invoked when this task transitions to state {@code isDone} (whether normally or via cancellation).
     * 当任务过渡状态时（无论是正常完成还是取消），调用这个protected方法。
     * The default implementation does nothing.
     * Subclasses may override this method to invoke completion callbacks or perform bookkeeping.
     * 默认的实现什么也没做。子类可以重写这个方法调用完成的回调或者执行记录。
     * Note that you can query status inside the implementation of this method to determine whether this task has been cancelled.
     * 注意点：您可以在本方法的实施中查询状态，以确定此任务是否已取消。
     */
    protected void done() { }

    /**
     * Sets the result of this future to the given value unless this future has already been set or has been cancelled.
     * 将此Future的结果设置为给定值，除非此Future已经确定或已取消。
     *
     * <p>This method is invoked internally by the {@link #run} method upon successful completion of the computation.
     * 这个方法由内部的run方法在计算成功后调用。
     *
     * @param v the value
     */
    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v; // 判断状态后才会将结果设置，如果任务取消了这里不会执行。
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException} with the given throwable as its cause,
     * unless this future has already been set or has been cancelled.
     * 使这个Future设置一个异常结果，使用指定的throwable作为原因，
     *
     * <p>This method is invoked internally by the {@link #run} method upon failure of the computation.
     * 这个方法由内部的run方法在计算成功后调用。
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }

    public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset, // 通过CAS设置当前执行的线程
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) { //callable不为空，状态为new才执行
                V result;
                boolean ran; // 局部变量，记录执行是否成功
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex); // 失败时将异常放到outcome中
                }
                if (ran)
                    set(result); // 执行成功将结果放到outcome中
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber stack.
     * 简单的链表列表节点，用来记录在一个程序堆栈中等待的线程。
     * See other classes such as Phaser and SynchronousQueue for more detailed explanation.
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and nulls out callable.
     * 移除并标志所有等待的线程，调用done方法，将callable设置为空。
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t); // 恢复暂停的线程
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     * 等待完成 或 中断 或 超时。
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            if (Thread.interrupted()) { // 判断当前线程是否被中断
                removeWaiter(q); // 如果当前线程被中断，移除线程
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETING) {
                if (q != null)
                    q.thread = null;
                return s;
            }
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();
            else if (q == null)
                q = new WaitNode();
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q); // 超时，移除等待的线程
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            else
                LockSupport.park(this); // 暂停当前线程
        }
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid accumulating garbage.
     * 尝试取消链接超时或者终端的等待节点，已避免积累垃圾。
     * Internal nodes are simply unspliced without CAS since it is harmless if they are traversed anyway by releasers.
     * 内部节点在没有CAS的情况下简单地不进行拼接，因为如果释放程序无论如何都要遍历这些节点，那么这些节点是无害的。
     * To avoid effects of unsplicing from already removed nodes, the list is retraversed in case of an apparent race.
     * 为了避免从已删除的节点中取消剪接的影响，在出现明显的竞争时，将重新遍历列表。
     * This is slow when there are a lot of nodes, but we don't expect lists to be long enough to outweigh higher-overhead schemes.
     * 当有很多节点时，速度会很慢，但我们不希望列表的长度超过更高开销的计划。
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try { // 对象的字段在主存中的偏移位置
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
