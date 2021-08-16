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
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.concurrent.Executor;

/**
 * A stage of a possibly asynchronous computation, that performs an action or computes a value when another CompletionStage completes.
 * 可以说是异步计算的一个阶段，当另一个CompletionStage完成时，执行一个操作或计算一个值。
 * 一个阶段在其计算终止时完成，但这可能反过来触发其他的相关阶段。该接口中定义的功能只采用少数基本形式，这些形式可以扩展为更大的方法集，以捕获一系列的使用风格。
 * A stage completes upon termination of its computation, but this may in turn trigger other dependent stages.
 * The functionality defined in this interface takes only a few basic forms, which expand out to
 * a larger set of methods to capture a range of usage styles: <ul>
 *
 * <li>The computation performed by a stage may be expressed as a Function, Consumer, or Runnable
 * (using methods with names including <em>apply</em>, <em>accept</em>, or <em>run</em>, respectively)
 * 一个阶段所执行的计算可表示为Function, Consumer, 或者是Runnable. 取决于它是否需要参数和/或产生结果。
 * depending on whether it requires arguments and/or produces results.
 * For example, {@code stage.thenApply(x -> square(x)).thenAccept(x ->
 * System.out.print(x)).thenRun(() -> System.out.println())}.
 * 从compose可以看到，另一种形式的应用的是阶段本身的功能，而不是它们的结果。
 * An additional form (<em>compose</em>) applies functions of stages themselves, rather than their results. </li>
 *
 * <li> One stage's execution may be triggered by completion of a single stage, or both of two stages, or either of two stages.
 * 一个阶段的执行可以由【单个阶段的完成】触发，也可以由【两个阶段都完成】触发，也可以由【两个阶段中的任何一个完成】触发。
 * Dependencies on a single stage are arranged using methods with prefix <em>then</em>.
 * Those triggered by completion of <em>both</em> of two stages may <em>combine</em> their results or effects,
 * using correspondingly named methods.
 * 依赖于一个单一的阶段安排使用前缀 then 的方法.那些由完成 both 的两个阶段可能结合他们的结果或效果,使用相应的命名方法。
 * 由这两个阶段中的任何一个触发的结果或效应不能保证哪个结果或效应用于相关阶段的计算。
 * Those triggered by <em>either</em> of two stages make no guarantees about which of the results or effects are used for the dependent stage's
 * computation.</li>
 *
 * <li> Dependencies among stages control the triggering of computations, but do not otherwise guarantee any particular ordering.
 * 阶段之间的不确定性控制着计算的触发，但不保证任何特定的顺序。
 * Additionally, execution of a new stage's computations may be arranged in any of three ways:
 * default execution, default asynchronous execution (using methods with suffix <em>async</em> that employ the stage's default asynchronous execution facility),
 * or custom (via a supplied {@link Executor}).
 * 此外，新阶段的计算可以以三种方式任意安排:
 * 1. 默认执行，2. 默认异步执行(使用后缀async的方法，它们使用阶段的默认异步执行配置)，3. 自定义(通过提供的{@link Executor})。
 * The execution properties of default and async modes are specified by CompletionStage implementations, not this interface.
 * Methods with explicit Executor arguments may have arbitrary execution properties, and might not even support concurrent execution,
 * but are arranged for processing in a way that accommodates asynchrony.
 * 默认模式和异步模式的执行属性是由CompletionStage实现指定的，而不是这个接口。
 * 带有显式Executor参数的方法可能有任意的执行属性，甚至可能不支持并发执行，但被安排用于处理，以适应异步。
 *
 * <li> Two method forms support processing whether the triggering stage completed normally or exceptionally:
 * Method {@link #whenComplete whenComplete} allows injection of an action regardless of outcome, otherwise preserving the outcome in its completion.
 * Method {@link #handle handle} additionally allows the stage to compute a replacement result that may enable further processing by other dependent stages.
 * 两种方法表单支持处理触发阶段是否正常完成:
 * 方法{@link #whenComplete whenComplete}允许在不考虑结果的情况下注入操作，否则在完成时保留结果。
 * 方法{@link #handle handle}还允许一个阶段计算一个替换结果，这个替换结果可能会让其他依赖阶段进行进一步的处理。
 * In all other cases, if a stage's computation terminates abruptly with an (unchecked) exception or error,
 * then all dependent stages requiring its completion complete exceptionally as well, with a {@link CompletionException} holding the exception as its cause.
 * 在所有其他情况下，如果一个阶段的计算突然终止了，因为(未检查的)异常或错误，
 * 那么所有需要它完成的依赖阶段也会异常完成，{@link CompletionException}将异常作为其原因。
 * If a stage is dependent on <em>both</em> of two stages, and both complete exceptionally,
 * then the CompletionException may correspond to either one of these exceptions.
 * 如果一个阶段依赖于两个阶段的，并且两个阶段都异常完成，那么CompletionException可能对应于这些异常中的任何一个。
 * If a stage is dependent on <em>either</em> of two others, and only one of them completes exceptionally,
 * no guarantees are made about whether the dependent stage completes normally or exceptionally.
 * 如果一个阶段依赖于其他两个中的一个，并且只有其中一个异常完成，不保证相关阶段是正常完成还是异常完成。
 * In the case of method {@code whenComplete}, when the supplied action itself encounters an exception,
 * then the stage exceptionally completes with this exception if not already completed exceptionally.
 * 在方法{@code whenComplete}的情况下，当提供的操作本身遇到异常时，那么这个阶段就异常地完成了，如果没有异常地完成的话。
 * </li>
 *
 * </ul>
 *
 * <p>All methods adhere to the above triggering, execution, and exceptional completion specifications
 * (which are not repeated in individual method specifications).
 * 所有方法都遵守上述触发、执行和异常完成规范(这些规范在单独的方法规范中不会重复)。
 * Additionally, while arguments used to pass a completion result
 * (that is, for parameters of type {@code T}) for methods accepting them may be null,
 * passing a null value for any other parameter will result in a {@link NullPointerException} being thrown.
 * 从理论上讲，虽然用于传递完成结果(即，对于类型为{@code T}的参数)的方法，接受它们的参数可能为null，但是传递任何其他参数的空值将导致抛出{@link NullPointerException}。
 *
 * <p>This interface does not define methods for initially creating, forcibly completing normally
 * or exceptionally, probing completion status or results, or awaiting completion of a stage.
 * 这个接口没有定义用于初始创建和强制正常完成的方法 或特殊情况下，探测完成状态或结果，或等待某一阶段的完成。
 * Implementations of CompletionStage may provide means of achieving such effects, as appropriate.
 * Method {@link #toCompletableFuture} enables interoperability among different implementations of this interface by providing a common conversion type.
 * CompletionStage的实现可以提供适当的实现这些效果的方法。
 * 方法{@link #toCompletableFuture}通过提供一个通用的转换类型来实现该接口的不同实现之间的互操作性。
 *
 * @author Doug Lea
 * @since 1.8
 */
public interface CompletionStage<T> {

    /**
     * Returns a new CompletionStage that, when this stage completes normally,
     * is executed with this stage's result as the argument to the supplied function.
     * 返回一个新的CompletionStage，当此阶段正常完成时，该CompletionStage将以此阶段的结果作为所提供函数的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     * 关于涉及异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn the function to use to compute the value of the returned CompletionStage
     * 用于计算返回的CompletionStage的值的函数
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> thenApply(Function<? super T,? extends U> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes normally,
     * is executed using this stage's default asynchronous execution facility,
     * with this stage's result as the argument to the supplied function.
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的默认异步执行工具执行，并将此阶段的结果作为所提供函数的参数。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     * 关于涉及异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn the function to use to compute the value of the returned CompletionStage
     * 用于计算返回的CompletionStage的值的函数
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> thenApplyAsync
        (Function<? super T,? extends U> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes normally,
     * is executed using the supplied Executor, with this stage's result as the argument to the supplied function.
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用所提供的Executor执行，并将此阶段的结果作为所提供函数的参数。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     * 关于涉及异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn the function to use to compute the value of the returned CompletionStage
     * 用于计算返回的CompletionStage的值的函数，用于异步执行的执行器
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> thenApplyAsync
        (Function<? super T,? extends U> fn,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this stage completes normally,
     * is executed with this stage's result as the argument to the supplied action.
     * 返回一个新的CompletionStage，当这个stage正常完成时，将此阶段的结果作为所提供操作的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     * 关于涉及异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action the action to perform before completing the returned CompletionStage
     * 在完成返回的CompletionStage之前执行的操作。
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenAccept(Consumer<? super T> action);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, is executed using this stage's default asynchronous
     * execution facility, with this stage's result as the argument to
     * the supplied action.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, is executed using the supplied Executor, with this
     * stage's result as the argument to the supplied action.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action,
                                                 Executor executor);
    /**
     * Returns a new CompletionStage that, when this stage completes normally, executes the given action.
     * 返回一个新的CompletionStage，当这个stage正常完成时，执行给定的操作。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenRun(Runnable action);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, executes the given action using this stage's default
     * asynchronous execution facility.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenRunAsync(Runnable action);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, executes the given action using the supplied Executor.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<Void> thenRunAsync(Runnable action,
                                              Executor executor);

    /**
     * Returns a new CompletionStage that, when this and the other given stage both complete normally,
     * is executed with the two results as arguments to the supplied function.
     * 返回一个新的CompletionStage，当这个和另一个给定的stage都正常完成时，将这两个结果作为所提供函数的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of the returned CompletionStage
     *
     * @param <U> the type of the other CompletionStage's result
     * @param <V> the function's return type
     * @return the new CompletionStage
     */
    public <U,V> CompletionStage<V> thenCombine
        (CompletionStage<? extends U> other,
         BiFunction<? super T,? super U,? extends V> fn);

    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, is executed using this stage's
     * default asynchronous execution facility, with the two results
     * as arguments to the supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of
     * the returned CompletionStage
     * @param <U> the type of the other CompletionStage's result
     * @param <V> the function's return type
     * @return the new CompletionStage
     */
    public <U,V> CompletionStage<V> thenCombineAsync
        (CompletionStage<? extends U> other,
         BiFunction<? super T,? super U,? extends V> fn);

    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, is executed using the supplied
     * executor, with the two results as arguments to the supplied
     * function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of
     * the returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the type of the other CompletionStage's result
     * @param <V> the function's return type
     * @return the new CompletionStage
     */
    public <U,V> CompletionStage<V> thenCombineAsync
        (CompletionStage<? extends U> other,
         BiFunction<? super T,? super U,? extends V> fn,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this and the other given stage both complete normally,
     * is executed with the two results as arguments to the supplied action.
     * 返回一个新的CompletionStage，当这个和另一个给定的stage都正常完成时，将这两个结果作为所提供操作的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param <U> the type of the other CompletionStage's result
     * @return the new CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBoth
        (CompletionStage<? extends U> other,
         BiConsumer<? super T, ? super U> action);

    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, is executed using this stage's
     * default asynchronous execution facility, with the two results
     * as arguments to the supplied action.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param <U> the type of the other CompletionStage's result
     * @return the new CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBothAsync
        (CompletionStage<? extends U> other,
         BiConsumer<? super T, ? super U> action);

    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, is executed using the supplied
     * executor, with the two results as arguments to the supplied
     * function.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the type of the other CompletionStage's result
     * @return the new CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBothAsync
        (CompletionStage<? extends U> other,
         BiConsumer<? super T, ? super U> action,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this and the other given stage both complete normally, executes the given action.
     * 返回一个新的CompletionStage，当这个和另一个给定的stage都正常完成时，执行给定的动作。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the returned CompletionStage
     *
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other,
                                              Runnable action);
    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, executes the given action using
     * this stage's default asynchronous execution facility.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other,
                                                   Runnable action);

    /**
     * Returns a new CompletionStage that, when this and the other
     * given stage complete normally, executes the given action using
     * the supplied executor.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other,
                                                   Runnable action,
                                                   Executor executor);
    /**
     * Returns a new CompletionStage that, when either this or the other given stage complete normally,
     * is executed with the corresponding result as argument to the supplied function.
     * 返回一个新的CompletionStage，当这个或另一个给定的stage正常完成时，将相应的结果作为所提供函数的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of
     * the returned CompletionStage
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> applyToEither
        (CompletionStage<? extends T> other,
         Function<? super T, U> fn);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, is executed using this
     * stage's default asynchronous execution facility, with the
     * corresponding result as argument to the supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of
     * the returned CompletionStage
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> applyToEitherAsync
        (CompletionStage<? extends T> other,
         Function<? super T, U> fn);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, is executed using the
     * supplied executor, with the corresponding result as argument to
     * the supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param fn the function to use to compute the value of
     * the returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> applyToEitherAsync
        (CompletionStage<? extends T> other,
         Function<? super T, U> fn,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when either this or the other given stage complete normally,
     * is executed with the corresponding result as argument to the supplied action.
     * 返回一个新的CompletionStage，当这个或另一个给定的stage正常完成时，将相应的结果作为所提供操作的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> acceptEither
        (CompletionStage<? extends T> other,
         Consumer<? super T> action);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, is executed using this
     * stage's default asynchronous execution facility, with the
     * corresponding result as argument to the supplied action.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> acceptEitherAsync
        (CompletionStage<? extends T> other,
         Consumer<? super T> action);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, is executed using the
     * supplied executor, with the corresponding result as argument to
     * the supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<Void> acceptEitherAsync
        (CompletionStage<? extends T> other,
         Consumer<? super T> action,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when either this or the other given stage complete normally, executes the given action.
     * 返回一个新的CompletionStage，当这个或另一个给定的stage正常完成时，执行给定的操作。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other,
                                                Runnable action);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, executes the given action
     * using this stage's default asynchronous execution facility.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterEitherAsync
        (CompletionStage<?> other,
         Runnable action);

    /**
     * Returns a new CompletionStage that, when either this or the
     * other given stage complete normally, executes the given action
     * using the supplied executor.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param other the other CompletionStage
     * @param action the action to perform before completing the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<Void> runAfterEitherAsync
        (CompletionStage<?> other,
         Runnable action,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this stage completes normally,
     * is executed with this stage as the argument to the supplied function.
     * 返回一个新的CompletionStage，当此阶段正常完成时，此阶段将作为所提供函数的参数执行。
     *
     * See the {@link CompletionStage} documentation for rules covering exceptional completion.
     *
     *
     * @param fn the function returning a new CompletionStage
     * @param <U> the type of the returned CompletionStage's result
     * @return the CompletionStage
     */
    public <U> CompletionStage<U> thenCompose
        (Function<? super T, ? extends CompletionStage<U>> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, is executed using this stage's default asynchronous
     * execution facility, with this stage as the argument to the
     * supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param fn the function returning a new CompletionStage
     * @param <U> the type of the returned CompletionStage's result
     * @return the CompletionStage
     */
    public <U> CompletionStage<U> thenComposeAsync
        (Function<? super T, ? extends CompletionStage<U>> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * normally, is executed using the supplied Executor, with this
     * stage's result as the argument to the supplied function.
     *
     * See the {@link CompletionStage} documentation for rules
     * covering exceptional completion.
     *
     * @param fn the function returning a new CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the type of the returned CompletionStage's result
     * @return the CompletionStage
     */
    public <U> CompletionStage<U> thenComposeAsync
        (Function<? super T, ? extends CompletionStage<U>> fn,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this stage completes exceptionally,
     * is executed with this stage's exception as the argument to the supplied function.
     * 返回一个新的CompletionStage，当这个stage异常完成时，将此阶段的异常作为所提供函数的参数执行。
     * Otherwise, if this stage completes normally, then the returned stage also completes normally with the same value.
     * 否则，如果此阶段正常完成，则返回的阶段也以相同的值正常完成。
     *
     * @param fn the function to use to compute the value of the
     * returned CompletionStage if this CompletionStage completed
     * exceptionally
     * @return the new CompletionStage
     */
    public CompletionStage<T> exceptionally
        (Function<Throwable, ? extends T> fn);

    /**
     * Returns a new CompletionStage with the same result or exception as this stage, that executes the given action when this stage completes.
     * 返回一个新的CompletionStage，它具有与此阶段相同的结果或异常，在此阶段完成时执行给定的操作。
     *
     * <p>When this stage is complete, the given action is invoked with the result (or {@code null} if none) and the exception (or {@code null} if none) of this stage as arguments.
     * 当此阶段完成时，给定的操作被调用，并将此阶段的结果(可能为null)和异常(可能为null)作为参数。
     * The returned stage is completed when the action returns.
     * If the supplied action itself encounters an exception,
     * then the returned stage exceptionally completes with this exception unless this stage also completed exceptionally.
     * 当操作返回时，返回阶段就完成了。
     *
     * @param action the action to perform
     * @return the new CompletionStage
     */
    public CompletionStage<T> whenComplete
        (BiConsumer<? super T, ? super Throwable> action);

    /**
     * Returns a new CompletionStage with the same result or exception as
     * this stage, that executes the given action using this stage's
     * default asynchronous execution facility when this stage completes.
     *
     * <p>When this stage is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this stage as arguments.  The returned stage is completed
     * when the action returns.  If the supplied action itself encounters an
     * exception, then the returned stage exceptionally completes with this
     * exception unless this stage also completed exceptionally.
     *
     * @param action the action to perform
     * @return the new CompletionStage
     */
    public CompletionStage<T> whenCompleteAsync
        (BiConsumer<? super T, ? super Throwable> action);

    /**
     * Returns a new CompletionStage with the same result or exception as
     * this stage, that executes the given action using the supplied
     * Executor when this stage completes.
     *
     * <p>When this stage is complete, the given action is invoked with the
     * result (or {@code null} if none) and the exception (or {@code null}
     * if none) of this stage as arguments.  The returned stage is completed
     * when the action returns.  If the supplied action itself encounters an
     * exception, then the returned stage exceptionally completes with this
     * exception unless this stage also completed exceptionally.
     *
     * @param action the action to perform
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletionStage
     */
    public CompletionStage<T> whenCompleteAsync
        (BiConsumer<? super T, ? super Throwable> action,
         Executor executor);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * either normally or exceptionally, is executed with this stage's
     * result and exception as arguments to the supplied function.
     *
     * <p>When this stage is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this stage as arguments, and the
     * function's result is used to complete the returned stage.
     *
     * @param fn the function to use to compute the value of the
     * returned CompletionStage
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> handle
        (BiFunction<? super T, Throwable, ? extends U> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * either normally or exceptionally, is executed using this stage's
     * default asynchronous execution facility, with this stage's
     * result and exception as arguments to the supplied function.
     *
     * <p>When this stage is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this stage as arguments, and the
     * function's result is used to complete the returned stage.
     *
     * @param fn the function to use to compute the value of the
     * returned CompletionStage
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> handleAsync
        (BiFunction<? super T, Throwable, ? extends U> fn);

    /**
     * Returns a new CompletionStage that, when this stage completes
     * either normally or exceptionally, is executed using the
     * supplied executor, with this stage's result and exception as
     * arguments to the supplied function.
     *
     * <p>When this stage is complete, the given function is invoked
     * with the result (or {@code null} if none) and the exception (or
     * {@code null} if none) of this stage as arguments, and the
     * function's result is used to complete the returned stage.
     *
     * @param fn the function to use to compute the value of the
     * returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    public <U> CompletionStage<U> handleAsync
        (BiFunction<? super T, Throwable, ? extends U> fn,
         Executor executor);

    /**
     * Returns a {@link CompletableFuture} maintaining the same
     * completion properties as this stage. If this stage is already a
     * CompletableFuture, this method may return this stage itself.
     * Otherwise, invocation of this method may be equivalent in
     * effect to {@code thenApply(x -> x)}, but returning an instance
     * of type {@code CompletableFuture}. A CompletionStage
     * implementation that does not choose to interoperate with others
     * may throw {@code UnsupportedOperationException}.
     *
     * @return the CompletableFuture
     * @throws UnsupportedOperationException if this implementation
     * does not interoperate with CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture();

}
