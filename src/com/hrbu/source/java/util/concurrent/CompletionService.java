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
 * A service that decouples the production of new asynchronous tasks from the consumption of the results of completed tasks.
 * 将新的异步任务的生产与已完成任务的结果的消费分离开来。生产者与消费者分离开的意思？
 * Producers {@code submit} tasks for execution. Consumers {@code take} completed tasks and process their results in the order they complete.
 * 生产者使用{@code submit}方法任务执行。消费者使用{@code take}方法获取已完成的任务，并按照完成的顺序处理它们的结果。
 * A {@code CompletionService} can for example be used to manage asynchronous I/O,
 * in which tasks that perform reads are submitted in one part of a program or system,
 * and then acted upon in a different part of the program when the reads complete,
 * possibly in a different order than they were requested.
 * 例如，{@code CompletionService}可以用来管理异步I/O，其中执行读操作的任务在程序或系统的一个部分提交，然后在程序的另一个部分执行读操作，可能和要求的顺序不一样。
 *
 * <p>Typically, a {@code CompletionService} relies on a separate {@link Executor} to actually execute the tasks,
 * in which case the {@code CompletionService} only manages an internal completion queue.
 * 通常，{@code CompletionService}依赖于一个独立的{@link Executor}来实际执行任务，在这种情况下，{@code CompletionService}只管理一个内部的完成队列。
 * The {@link ExecutorCompletionService} class provides an implementation of this approach.
 * {@link ExecutorCompletionService}类提供了这种方法的实现。
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a task to a {@code CompletionService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions taken by that task, which in turn <i>happen-before</i>
 * actions following a successful return from the corresponding {@code take()}.
 */
public interface CompletionService<V> {
    /**
     * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
     * Upon completion, this task may be taken or polled.
     * 提交一个返回值的任务以供执行，并返回一个Future，表示任务的挂起结果。完成后，可以执行take或poll。
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<V> submit(Callable<V> task);

    /**
     * Submits a Runnable task for execution and returns a Future representing that task.
     * Upon completion, this task may be taken or polled.
     *
     *
     * @param task the task to submit
     * @param result the result to return upon successful completion
     * @return a Future representing pending completion of the task,
     *         and whose {@code get()} method will return the given
     *         result value upon completion
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<V> submit(Runnable task, V result);

    /**
     * Retrieves and removes the Future representing the next completed task, waiting if none are yet present.
     * 检索并删除表示下一个已完成任务的Future，如果没有则等待。
     *
     * @return the Future representing the next completed task
     * @throws InterruptedException if interrupted while waiting
     */
    Future<V> take() throws InterruptedException;

    /**
     * Retrieves and removes the Future representing the next completed task, or {@code null} if none are present.
     * 检索并删除代表下一个已完成任务的Future，如果没有，则返回{@code null}。
     *
     * @return the Future representing the next completed task, or
     *         {@code null} if none are present
     */
    Future<V> poll();

    /**
     * Retrieves and removes the Future representing the next completed task,
     * waiting if necessary up to the specified wait time if none are yet present.
     * 检索并删除表示下一个已完成任务的Future，如有必要，可等待至指定的等待时间，如果没有出现。
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the Future representing the next completed task or
     *         {@code null} if the specified waiting time elapses
     *         before one is present
     * @throws InterruptedException if interrupted while waiting
     */
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
