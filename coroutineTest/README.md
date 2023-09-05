## 协程是什么？ 可以干什么？

建议阅读： [kotlin协程硬核解读(1. 协程初体验)](https://openxu.blog.csdn.net/article/details/116016798)

1、 协程是对线程的封装，因此性能不会超过线程， 它主要的作用是让开发者能以同步（顺序）的方式 去写异步（回调）的代码

2、 协程可以简单理解为：包裹可执行代码块的Lambda表达式， 协程的执行可以简单理解为：Lambda（or Function）对象被Dispatch到不同的线程上执行（invoke）的过程

**同步的方式编写异步代码的例子：**

```
class SystemRemoteRepository{
	/**
	 * 1. 展示回调嵌套，回调地狱
	 */
	fun getArticleList(responseBack: (result: Pagination<Article>?) -> Unit) {
	        /**1. 获取文章树结构*/
	        val call:Call<ApiResult<MutableList<Tree>>> = RetrofitClient.apiService.getTree()
	        //同步（需要自己手动切换线程）
	        //val response : Response<ApiResult<MutableList<Tree>>> = call.execute()
	        //异步回调
	        call.enqueue(object : Callback<ApiResult<MutableList<Tree>>> {
	            override fun onFailure(call: Call<ApiResult<MutableList<Tree>>>, t: Throwable) {
	            }
	            override fun onResponse(call: Call<ApiResult<MutableList<Tree>>>, response: Response<ApiResult<MutableList<Tree>>>) 
	        })
	        log("函数执行结束")
	    }
}

class SystemViewModel : BaseViewModel(){
    private val remoteRepository : SystemRemoteRepository by lazy { SystemRemoteRepository() }
    val page = MutableLiveData<Pagination<Article>>()

    fun getArticleList() {
        viewModelScope.launch {  //主线程开启一个协程
        	// 网络请求：IO线程
            val tree : ApiResult<MutableList<Tree>> = RetrofitClient.apiService.getTreeByCoroutines()
            // 主线程
            val cid = tree?.data?.get(0)?.id
            if(cid!=null){
            	// 网络请求：IO线程
                val pageResult : ApiResult<Pagination<Article>> = RetrofitClient.apiService.getArticleListByCoroutines(0, cid)
                // 主线程
                page.value = pageResult.data!!
                log("Lambda的会被多次调用, 但是执行的节点完全不同")
            }
        }
    }
}

```



## 协程上下文

```
简单讲述协程上下文
```

协程上下文， 包含协程的各种关键信息， 如：协程名、调度器、Job等关键要素； 被设计为index-set : 带有set去重性质的数组。

推荐阅读  [Kotlin 协程 | CoroutineContext 为什么要设计成 indexed set？（一）](https://juejin.cn/post/6978613779252641799)  

描述协程上下文的代码：

```
public interface CoroutineContext {
    /**
     * An element of the [CoroutineContext]. An element of the coroutine context is a singleton context by itself.
     */
    public interface Element : CoroutineContext {
        /**
         * A key of this coroutine context element.
         */
        public val key: Key<*>
    }
}
```

形如 字母+数组的对象， 其中字母代表其Key的类型， 数字代表该类型的不同实例

A1 + B1 =>  A1 + B1

A1 + A2 + B1 = A2 + B1

...  当然其本身也存在数组内部顺序（ABC还是CBA....）【一般来说， 后加的元素是会覆盖之前的元素】， 这里就不细说了， 影响不大

---

接下来看一下典型的协会上下文元素， 可以自定义他们, 一般的代码表现形式为： 

```
// 指定当前协会的名称 可用于调试
public data class CoroutineName(
    /**
     * User-defined coroutine name.
     */
    val name: String
) : AbstractCoroutineContextElement(CoroutineName) {
    /**
     * Key for [CoroutineName] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<CoroutineName>
}
```

参照上述的例子， 可以自定义

```
public data class CoroutineAbelzhaoDebug(
    /**
     * User-defined coroutine name.
     */
    val name: String
) : AbstractCoroutineContextElement(CoroutineName) {
    /**
     * Key for [CoroutineName] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<CoroutineAbelzhaoDebug>
}
```

如何访问协程上下文：`coroutineContext[CoroutineName]`

```
private val scopeExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log("顶级作用域异常处理器捕获到${coroutineContext[CoroutineName] ?: '.'}异常: ${throwable.message}")
    }
```

再看一下具体的几个协程上下文元素

- Job (代表协程的《任务》这一概念， 其中涵盖了协程的运行状态、控制任务的执行和取消、父子关系的建立 .... ， 每启动一个协程（如 `launch`）， 都会创建一个新的Job替换原有上下文的Job， 并与原有上下文的Job建立父子关系，实现结构化并发、

- 此外调用API启动协程后， 会返回一个Job实例（或者说API只暴露Job对象供开发者操作， 屏蔽掉了其他的细节）

   

  ```
  public interface Job : CoroutineContext.Element {
      public companion object Key : CoroutineContext.Key<Job>
      public val isActive: Boolean
      public val isCompleted: Boolean
      public fun start(): Boolean
      public fun cancel(cause: CancellationException? = null)
  	xxx
  }
  
  val job: Job = lifecycleScope.launch {
       // do something
  }
  ```

- 拦截器   （`Dispatchers.IO ....`） 实现协程在线程上的调度

  ```
  public abstract class CoroutineDispatcher :
      AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
  
      /** @suppress */
      @ExperimentalStdlibApi
      public companion object Key : AbstractCoroutineContextKey<ContinuationInterceptor, CoroutineDispatcher>(
          ContinuationInterceptor,
          { it as? CoroutineDispatcher })
  }
  ```

  

- xxx

  

## 协程Job和结构化并发

```
讲述协程的父子关系, 跟进到TestLaunch01Activity 将上一节和本节的内容进行复述
```

Job (代表协程的任务， 包含协程运行状态、控制任务的执行和取消、父子关系的建立， 每启动一个协程（launch）， 都会创建一个新的Job替换上下文的Job， 并与上下文的Job建立父子关系，实现结构并发, 本身启动的协程， 返回的实例（或者说API暴露给我们的就是一个Job对象）

```
public interface Job : CoroutineContext.Element {
    /**
     * Key for [Job] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<Job>
    public val isActive: Boolean
    public val isCompleted: Boolean
    public val isCancelled: Boolean
    
    @InternalCoroutinesApi
    public fun getCancellationException(): CancellationException
    public fun start(): Boolean
    public fun cancel(cause: CancellationException? = null)
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Since 1.2.0, binary compatibility with versions <= 1.1.x")
    public fun cancel(cause: Throwable? = null): Boolean
    // 子Job列表
    public val children: Sequence<Job>
    // 其实现类中还有初始化 parent的方法
}

 val job = lifecycleScope.launch{
            	
 }
 job.cancel()
```



官方的结构化并发解释：https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency

> Coroutines follow a principle of **structured concurrency** which means that new coroutines can be only launched in a specific [CoroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html) which delimits the lifetime of the coroutine. The above example shows that [runBlocking](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html) establishes the corresponding scope and that is why the previous example waits until `World!` is printed after a second's delay and only then exits.
>
> In a real application, you will be launching a lot of coroutines. Structured concurrency ensures that they are not lost and do not leak. An outer scope cannot complete until all its children coroutines complete. Structured concurrency also ensures that any errors in the code are properly reported and are never lost.

协程遵循**结构化并发**原则，这意味着新的协程只能在特定的[CoroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html)中启动，该参数限定了协程的生存期。上面的示例显示 [runBlocking](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html) 建立了相应的范围，这就是为什么前面的示例在一秒钟的延迟后一直打印 “World！”，然后才退出。 在实际的应用程序中，您将启动许多协程。结构化并发可确保它们不会丢失且不会泄漏。外部作用域在其所有子项协程完成之前无法完成。结构化并发还可确保正确报告代码中的任何错误，并且永远不会丢失。



关于协程的阻塞问题（或者说代码执行顺序流）

- `launch` 是非阻塞的， 执行完毕之后立即返回， 不会对当前协程造成阻塞
- 同一协程内的代码（比如launch内部的代码）是按照顺序执行的， 执行到挂起函数（注意`launch`不是挂起函数），比如`delay()` 会挂起当前协程(造成执行流中断，协程所在的线程会放弃当前代码块执行，转去其他部分执行)
- 针对第一点千万需要注意的是， 虽然 launch是非阻塞的，但是如果协程和子协程在同一线程执行（如主线程）， 同一只能有一个协程“正在运行”， 即是 主线程卡死的情况仍然存在， 因此 执行耗时任务 必须要切换到非主线程去操作！ 



最后， 由Scope构建出来的带有结构化并发性质协程应该是这个样子的：

```
// 1、 launch是阻塞的 立即返回Job 
// 2、 job_1_3调用后 不会立即结束顶层协程 而是会等待所有子协程完成
fun testStructureConcurrency() {
        val job1_1 = lifecycleScope.launch {
            val job1_1_1 = launch {
            }
        }
        val job1_2 = lifecycleScope.launch {
            val job1_2_1 = launch {
            }
            val job1_2_2 = launch {
            }
        }
        val job1_3 = lifecycleScope.launch {
            val job1_3_1 = launch {
            }
        }
    }
```

![](https://zhaojunchen-1259455842.cos.ap-nanjing.myqcloud.com/img/20220719175634.png)



## 协程的异常处理

```
1、 讲述openxu中Java的异常处理（JVM的线程栈 ）
2、 讲述协程的父子关系及其异常的向上传播 
```

线程和协程的异常处理机制的根在哪里？    参考文献：https://blog.csdn.net/xmxkf/article/details/117200099

![](https://zhaojunchen-1259455842.cos.ap-nanjing.myqcloud.com/img/20220719180541.png)

![](https://zhaojunchen-1259455842.cos.ap-nanjing.myqcloud.com/img/20220719180627.png)



然后看下错误的异常处理：

解释下这里的内容， https://www.lukaslechner.com/why-exception-handling-with-kotlin-coroutines-is-so-hard-and-how-to-successfully-master-it/

异常处理的规则， 向上传播

然后看下典型的几个例子：https://blog.csdn.net/xmxkf/article/details/117200099

和我本地的测试代码 `TestExceptionActivity` 



## 在项目中的落地

- 打开联合收单， 看下里面常见的操作 
- 在 `onCreate` 使用 `lifecycleScope.launchWhenStart()` 完成 **一次性** 的 `start` 阶段的初始化
- 一般情况下， 直接使用`lifecycleScope`和`viewModelScope` 不需要自定义`Scope`, 自定义`Scope` 也最好使用 `SupervisorJob`
- 使用`async`同时进行多个接口的并发请求 
- `BottomShareDialog` 使用`withContext`的例子 (带异常处理)
- `retrofit`和`viewModel`自动取消的原理
- 解释下`ViewModelScope` 及其Retrofit自动取消网络请求的原理



## 个人总结（不一定完全正确）

1、 针对网络请求， 最好在retrofit层面或者网络框架的内部处理 `catch` 异常， 避免异常通过协程向上传播， 主要是写异常处理器也很麻烦、还容易忘记

```
lifecycleScope.launch(Dispatchers.IO){
            try {
                // NetWork request
                throw Exception("")
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
```

2、 针对 `async` 情形下的多个接口同时请求（存在一个接口挂掉、其他数据都失效的情况）， 可以适当使用 协程传播的特性， 在一个接口挂掉的时候， 提前结束其他的接口

```
lifecycleScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val d1 = async {
                // xxx
            }
            val d2 = async {
                // xxx
            }
            try {
                val v1 = d1.await()
                val v2 = d2.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
```

3、 关于封装SDK或者通用函数： 一般情况可以  `coroutineScope{}` 获取作用域， 挂起函数本身不能提作用域， 但是`coroutineScope`会将调用`suspend`函数的调用者作用域传递过来， 因此非常适合进行支持子协程的挂起函数封装， 此外 它具有非常好的 re-thrown的异常处理机制， 函数编写者不用关心函数协程层面的异常传播， 任何的异常都能被捕获并抛出到调用者

```
// coroutineScope 非常适合进行 async和多子协程的并发函数封装
    // 1 继承上级的作用域 下级可以直接开启协程
    // 2 拦截协程内部的异常 直接在外层 test 函数中抛出异常 re-thrown
    // 3 使用re-thrown 特性 保证函数的正常调用
    // 4 何种异常 都能捕获 非常TQL
    @Throws(Exception::class)
    private suspend fun test(): Int = coroutineScope {
        log(this.coroutineContext)
        val job1 = launch {
            delay(100)
            throw IllegalArgumentException("job1 launch throw exception")
        }
        val job3 = async {
            delay(1)
            throw IllegalArgumentException("job3 async throw exception")
        }
        launch {
            delay(100)
            throw IllegalArgumentException("job1 launch throw exception")
        }
        val job4 = async {
            delay(1)
            throw IllegalArgumentException("job4 async throw  exception")
        }

        // throw IllegalArgumentException("throw exception")
        return@coroutineScope 1
    }
```

 

4、 关于封装SDK或者通用函数： 封装的任务非常独立的时候， 可以使用 `supervisorScope{}` 替代 `coroutineScope` 获取作用域，它本身和`SupervisorJob` 有一样的特性， 但是他的异常会通过协程的机制进行传播， 因此 要么在抛异常的内部代码块提前处理异常， 要么 熟悉协程异常处理器， 避免崩溃！



5、  关于封装SDK或者通用函数： 一般情况 不需要在内部进行开启多个子协程、进行多次线程切换的情况， withContext是最好用的

```
// 充分说明 withContext 会将结果或者异常以普通的方式返回到外层
// 异常不会通过协程之间的父子关系去传播
// 在外层可以直接取拦截这个withContext的返回结(value or exception)
fun AppCompatActivity.testWithContext001() {
    var job1: Job? = null
    var job2: Job? = null
    var job3: Job? = null
    var job4: Job? = null
    var job5: Job? = null
    job1 = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
        log("job1捕获到异常: ${throwable.message}")
    }) {
        job2 = launch(CoroutineExceptionHandler { _, throwable ->
            log("job2捕获到异常: ${throwable.message}")
        }) {
            try {
                val result = doWorkBack(true)
            } catch (e: Exception) {
                log("try-catch捕获到异常: ${e.message}")
            }
        }
        delay(MAX)
    }
}

// testWithContext001
// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════
// ║Thread: main
// ╟───────────────────────────────────────────────────────────────────────────────────────────────────
// ║try-catch捕获到异常: withContext doWorkBack throw a exception
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
suspend fun doWorkBack(flag: Boolean): Int = withContext(Dispatchers.IO) {
    val a = 1
    delay(1000)
    val b = a * a
    if (flag) {
        throw IllegalArgumentException("withContext doWorkBack throw a exception")
    }
    delay(10)
    return@withContext b
}
```



6、 关于封装SDK或者通用函数:  如果说是需要对操作的线程进行函数封装`suspendCancellableCoroutine` 和 `suspendCoroutine` （后者不支持协程的取消， 不建议使用）， 正常的开发者， 一般不会在协程里面使用线程进行异步处理这中SB操作， 这种方式一般出现在 对原有的、 进行线程操作的三方框架的协程兼容上， 让其支持非阻塞挂起的特性， 比如：retrofit 对协程的支持。

```
suspend fun getUser(): User = suspendCancellableCoroutine { continuation ->
    Thread {
        Thread.sleep(2000)
        continuation.resume(User("Abelzhao", 23))
    }.start()
}

```

retrofit对协程的支持 

```
suspend fun <T : Any> Call<T>.await(): T {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
          val body = response.body()
          if (body == null) {
            val invocation = call.request().tag(Invocation::class.java)!!
            val method = invocation.method()
            val e = KotlinNullPointerException("Response from " +
                method.declaringClass.name +
                '.' +
                method.name +
                " was null but response body type was declared as non-null")
            continuation.resumeWithException(e)
          } else {
            continuation.resume(body)
          }
        } else {
          continuation.resumeWithException(HttpException(response))
        }
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        continuation.resumeWithException(t)
      }
    })
  }
}
```



## 参考文献

### 关于协程系列的内容:

**openXu 大佬系列:**

[kotlin协程硬核解读(1. 协程初体验)](https://openxu.blog.csdn.net/article/details/116016798) 

[kotlin协程硬核解读(2. 协程基础使用&源码浅析)](https://openxu.blog.csdn.net/article/details/116999821) 

[kotlin协程硬核解读(3. suspend挂起函数&挂起和恢复的实现原理)](https://openxu.blog.csdn.net/article/details/117000039) 

[kotlin协程硬核解读(4. 协程的创建和启动流程分析)](https://openxu.blog.csdn.net/article/details/117000126) 

[kotlin协程硬核解读(5. Java异常本质&协程异常传播取消和异常处理机制)](https://openxu.blog.csdn.net/article/details/117200099) 

[kotlin协程硬核解读(6. 协程调度器实现原理)](https://openxu.blog.csdn.net/article/details/117336178) 

### 协程上下文的硬核解读:

[Kotlin 协程 | CoroutineContext 为什么要设计成 indexed set？（一）]( https://juejin.cn/post/6978613779252641799)

### 关于异常捕获和传播

1、 https://www.lukaslechner.com/why-exception-handling-with-kotlin-coroutines-is-so-hard-and-how-to-successfully-master-it/ 

2、 https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c 

### 协程官方英文文档

1、 https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/coroutines-guide.md

### google官方协程规范文档

1、 https://developer.android.google.cn/kotlin/coroutines/coroutines-best-practices

2、 https://medium.com/androiddevelopers/coroutines-first-things-first-e6187bf3bb21

3、 https://medium.com/androiddevelopers/coroutines-on-android-part-ii-getting-started-3bff117176dd

4、 https://medium.com/androiddevelopers/coroutines-on-android-part-iii-real-work-2ba8a2ec2f45

### 腾讯云翻译几篇文章

[1、 [译] 关于 Kotlin Coroutines， 你可能会犯的 7 个错误](https://cloud.tencent.com/developer/article/1870416?from=article.detail.1605877) 



### 注意事项:

1、 协程开发时， 需要理清协程之间的父子关系