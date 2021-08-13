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

/**
 * An object that executes submitted {@link Runnable} tasks. 执行提交的Runnable任务的一个对象。
 * This interface provides a way of decoupling task submission from the
 * mechanics of how each task will be run, including details of thread use, scheduling, etc.
 * An {@code Executor} is normally used instead of explicitly creating threads.
 * For example, rather than invoking {@code new Thread(new(RunnableTask())).start()} for each of a set of tasks,
 * 这个接口提供了一种将任务提交与每个任务将如何运行分离的方法。例如,而不是调用thread.start的一组任务,
 * you might use:
 *
 * <pre>
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 * </pre>
 *
 * However, the {@code Executor} interface does not strictly require that execution be asynchronous.
 * In the simplest case, an executor can run the submitted task immediately in the caller's thread:
 * 然而,Executor接口并不严格要求执行是异步的。
 * 在最简单的情况下，执行器可以立即在调用者的线程中运行提交的任务:
 *
 *  <pre> {@code
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }}</pre>
 *
 * More typically, tasks are executed in some thread other than the caller's thread.
 * The executor below spawns a new thread for each task.
 * 更典型的是，任务是在调用者的线程之外的某个线程中执行的。下面的执行器为每个任务生成一个新线程。
 *
 *  <pre> {@code
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }}</pre>
 *
 * Many {@code Executor} implementations impose some sort of limitation on how and when tasks are scheduled.
 * 大多数执行器实现对任务的调度方式和时间施加了某种限制。
 * The executor below serializes the submission of tasks to a second executor, illustrating a composite executor.
 * 下面的执行器序列化将任务提交给第二个执行器，说明了一个复合执行器。
 *
 *  <pre> {@code
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }}</pre>
 *
 * The {@code Executor} implementations provided in this package implement {@link ExecutorService}, which is a more extensive interface.
 * 这个包中提供的{@code Executor}实现，实现了{@link ExecutorService}，这是一个更广泛的接口。
 * The {@link ThreadPoolExecutor} class provides an extensible thread pool implementation.
 * The {@link Executors} class provides convenient factory methods for these Executors.
 * ThreadPoolExecutor类提供一个可扩展的线程池实现。Executors类为这些Executors提供了方便的工厂方法。
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a {@code Runnable} object to an {@code Executor}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * its execution begins, perhaps in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Executor {

    /**
     * Executes the given command at some time in the future.
     * The command may execute in a new thread, in a pooled thread, or in the calling thread, at the discretion of the {@code Executor} implementation.<br/>
     * 在将来的某个时候执行给定的命令。命令可以在新线程中执行，也可以在池线程中执行，也可以在调用线程中执行，由{@code Executor}实现决定。
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    void execute(Runnable command);
}
