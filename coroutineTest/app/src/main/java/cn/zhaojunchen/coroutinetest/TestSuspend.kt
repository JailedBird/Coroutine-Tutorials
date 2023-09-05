package cn.zhaojunchen.coroutinetest

import cn.zhaojunchen.coroutinetest.model.User
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/18
 *     desc   :
 * </pre>
 */
// 测试协程的字节码底层实现

/*
suspend fun getUser(): User {
    println("假的挂起函数${Thread.currentThread()}")
    // Thread.sleep(1000)
    delay(100)
    delay(200)
    delay(300)
    delay(400)
    delay(500)
    return User("openXu", 23)
}*/
// 请反编译查看 main函数启动的协程的源代码 大概就只知道为啥协程不会导致阻塞!
fun main() {
    GlobalScope.launch {
        //--------------------初始状态0----------------------
        println("状态0")
        delay(1000)         //挂起点1
        //--------------------状态1----------------------
        println("状态1")
        val user = getUser()  //挂起点2
        //--------------------状态2----------------------
        println("状态2 $user")
        delay(1000)          //挂起点3
        //--------------------状态3----------------------

        println("状态3 $user")
        delay(1000)
        println("状态4")  //挂起点4
    }
    Thread.sleep(5000)
}

// 直接使用 suspendCancellableCoroutine
suspend fun getUser(): User = suspendCancellableCoroutine { continuation ->
    Thread {
        Thread.sleep(2000)
        continuation.resume(User("Abelzhao", 23))
    }.start()
}

// 经典案例  几个关键点
// 1、 协程多次调用invokeSuspend内部的函数 通过修改label状态机 执行不同的调度逻辑
// 2、 下面的例子中 每次进入invokeSuspend都会先执行label33 然后每次label变化都会
// 先检查异常相关是否发生
// 3、 不同的break label层级 实现了不同代码block的挂起和执行
// 4、 更多的数据结构和隐藏的调用时机待挖掘
// 5、 新发现 new Function2 构成了 continuation 其中的invokeSuspend定义只是在 BaseContinuationImpl中调用函数 val outcome = invokeSuspend(param)
// 我甚至怀疑这个Function2本身就是 Continuation的马甲(kotlin自动生成的代码看不完全) 因此知道的不是很全面
// openXu的解释是: 创建的这个函数对象是个匿名对象，这个匿名类的类型是SuspendKt$main$1 extends SuspendLambda (ContinuationImpl->BaseContinuationImpl, 包含invokeSuspend函数的实现) implements Function2
// 因此匿名对象本身就是continuation 查看delay函数 DelayKt.delay(1000L, this) 可以看到这continuation实际上是被下发到delay中从而实现resume挂起和恢复


// 新发现: 参照openXu的协程第四篇 可以看到 new Function生成的对象其实本身并不是真的Function2 而是SuspendLambda 传递的上下文是一个null 因此需要调用 invoke和create重新创续体
// 他立即调用invokeSuspend(Unit) 直接出发续体内容的执行
/*public final class TestSuspendKt {
   public static final void main() {
      BuildersKt.launch$default((CoroutineScope)GlobalScope.INSTANCE, (CoroutineContext)null, (CoroutineStart)null, (Function2)(new Function2((Continuation)null) {
         Object L$0;
         int label;

         @Nullable
         public final Object invokeSuspend(@NotNull Object $result) {
            String var3;
            label42: {
               User user;
               Object var4;
               label34: {
                  Object var10000;
                  label33: {
                     var4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                     String var5;
                     switch(this.label) {
                     case 0:
                        ResultKt.throwOnFailure($result);
                        var5 = "状态0";
                        System.out.println(var5);
                        this.label = 1;
                        if (DelayKt.delay(1000L, this) == var4) {
                           return var4;
                        }
                        break;
                     case 1:
                        ResultKt.throwOnFailure($result);
                        break;
                     case 2:
                        ResultKt.throwOnFailure($result);
                        var10000 = $result;
                        break label33;
                     case 3:
                        user = (User)this.L$0;
                        ResultKt.throwOnFailure($result);
                        break label34;
                     case 4:
                        ResultKt.throwOnFailure($result);
                        break label42;
                     default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                     }

                     var5 = "状态1";
                     System.out.println(var5);
                     this.label = 2;
                     var10000 = TestSuspendKt.getUser(this);
                     if (var10000 == var4) {
                        return var4;
                     }
                  }

                  user = (User)var10000;
                  var3 = "状态2 " + user;
                  System.out.println(var3);
                  this.L$0 = user;
                  this.label = 3;
                  if (DelayKt.delay(1000L, this) == var4) {
                     return var4;
                  }
               }

               var3 = "状态3 " + user;
               System.out.println(var3);
               this.L$0 = null;
               this.label = 4;
               if (DelayKt.delay(1000L, this) == var4) {
                  return var4;
               }
            }

            var3 = "状态4";
            System.out.println(var3);
            return Unit.INSTANCE;
         }

         @NotNull
         public final Continuation create(@Nullable Object value, @NotNull Continuation completion) {
            Intrinsics.checkNotNullParameter(completion, "completion");
            Function2 var3 = new <anonymous constructor>(completion);
            return var3;
         }

         public final Object invoke(Object var1, Object var2) {
            return ((<undefinedtype>)this.create(var1, (Continuation)var2)).invokeSuspend(Unit.INSTANCE);
         }
      }), 3, (Object)null);
      Thread.sleep(5000L);
   }

   // $FF: synthetic method
   public static void main(String[] var0) {
      main();
   }

   @Nullable
   public static final Object getUser(@NotNull Continuation $completion) {
      int $i$f$suspendCancellableCoroutine = false;
      int var3 = false;
      CancellableContinuationImpl cancellable$iv = new CancellableContinuationImpl(IntrinsicsKt.intercepted($completion), 1);
      cancellable$iv.initCancellability();
      CancellableContinuation continuation = (CancellableContinuation)cancellable$iv;
      int var6 = false;
      (new Thread((Runnable)(new TestSuspendKt$getUser$2$1(continuation)))).start();
      Object var10000 = cancellable$iv.getResult();
      if (var10000 == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
         DebugProbesKt.probeCoroutineSuspended($completion);
      }

      return var10000;
   }
}
*/

// 新发现 为什么coroutine里面的异常都会被捕捉到?
// 查看下面的函数 : runSafely直接将block中的未捕获的异常直接捕获 并且抛出到 续体中
/* internal fun <R, T> (suspend (R) -> T).startCoroutineCancellable(
    receiver: R, completion: Continuation<T>,
    onCancellation: ((cause: Throwable) -> Unit)? = null
) =
    runSafely(completion) {
        createCoroutineUnintercepted(receiver, completion).intercepted().resumeCancellableWith(Result.success(Unit), onCancellation)
    }

/**
 * Similar to [startCoroutineCancellable], but for already created coroutine.
 * [fatalCompletion] is used only when interception machinery throws an exception
 */
internal fun Continuation<Unit>.startCoroutineCancellable(fatalCompletion: Continuation<*>) =
    runSafely(fatalCompletion) {
        intercepted().resumeCancellableWith(Result.success(Unit))
    }

/**
 * Runs given block and completes completion with its exception if it occurs.
 * Rationale: [startCoroutineCancellable] is invoked when we are about to run coroutine asynchronously in its own dispatcher.
 * Thus if dispatcher throws an exception during coroutine start, coroutine never completes, so we should treat dispatcher exception
 * as its cause and resume completion.
 */
private inline fun runSafely(completion: Continuation<*>, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        completion.resumeWith(Result.failure(e))
    }
}
*/


// 有一段核心代码 IDE看不到:https://github.com/JetBrains/kotlin/blob/92d200e093c693b3c06e53a39e0b0973b84c7ec5/libraries/stdlib/jvm/src/kotlin/coroutines/intrinsics/IntrinsicsJvm.kt
// 如何调用和构建SuspendLambda 并且调用invoke实现对应的函数
/*@SinceKotlin("1.3")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
    completion: Continuation<T>
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(probeCompletion)
    else
        createCoroutineFromSuspendFunction(probeCompletion) {
            (this as Function1<Continuation<T>, Any?>).invoke(it)
        }
}
*/