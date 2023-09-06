package cn.zhaojunchen.coroutinetest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlin.concurrent.thread

// [Why exception handling with Kotlin Coroutines is so hard and how to successfully master it!]
// (https://www.lukaslechner.com/why-exception-handling-with-kotlin-coroutines-is-so-hard-and-how-to-successfully-master-it/)
// 测试协程的异常处理
class TestExceptionActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, TestExceptionActivity::class.java)
            context.startActivity(starter)
        }
    }
    // 将异常处理器绑定在顶级作用域或者顶级协程都是可以的 同时存在时使用顶级协程作用域上的处理器
    // 考虑到一般作用域是写死的（如lifecycleScope） 因此一般异常处理器的位子一般在顶层协程上

    // Job初始化的顶层作用域 ：处理异常的时候 异常会传播到本身、其他孩子也会GG
    // SupervisorJob初始化的顶层作用域：则不会干死自己和其他的兄弟 可以认为

    // 将这个例子不停地变式:
    // 1、 测试有无异常处理器 及其 在顶层作用域上、顶层协程上的 影响
    // 2、 测试 SupervisorJob 和 Job

    // 顶层作用域的异常处理器
    private val scopeExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log("顶级作用域异常处理器捕获到${coroutineContext[CoroutineName] ?: '.'}异常: ${throwable.message}")
    }

    // 顶层协程(作用域启动的直接子协程)的异常处理器
    private val topLaunchExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, throwable ->
            log("顶级协程异常处理器捕获到${coroutineContext[CoroutineName] ?: '.'}异常: ${throwable.message}")
        }

    private val normalExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log("普通协程异常处理器捕获到${coroutineContext[CoroutineName] ?: '.'}异常: ${throwable.message}")
    }

    private val _lifecycleScope get() = lifecycleScope

    // 使用自定义Scope的意义在于 测试Job和SupervisorJob对协程的影响
    // Job会将顶层的协程传递到顶层Scope 【如果有Top Job存在异常处理器，则还是由Top Job处理】
    // Top Scope会导致其他任务取消 这就是最大的影响
    // 以及他们的异常处理器解决方案
    private val scope =
        // CoroutineScope(Dispatchers.Main.immediate + scopeExceptionHandler +/*SupervisorJob*/Job())
        CoroutineScope(Dispatchers.Main.immediate + scopeExceptionHandler + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_exception)
        lifecycleScope.launch {
            test001()
        }
    }

    // 异常时归属于线程的 不同线程是不能捕获的
    private fun test000() {
        try {
            thread {
                throw Exception("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // try catch无法 从外部捕获协程的异常 原理和test000 类似
    // 人家压根不在一个线程上玩 捕获了个寂寞
    private fun test000_1() {
        try {
            lifecycleScope.launch {
                throw java.lang.IllegalArgumentException("顶级协程抛出异常")
            }
        } catch (e: Exception) {
            log("try-catch 捕获到异常 ${e.message}")
        }
    }

    // 测试异常处理器的异常传播 切换Scope的Job配置和 各种异常处理器
    private fun test001() {
        scope.launch(CoroutineName("job1") + topLaunchExceptionHandler) {
            launch(CoroutineName("job1-1") + normalExceptionHandler) {
                delay(2000)
                throw RuntimeException("inner exception")
            }
            launch(CoroutineName("job1-2")) {
                while (true) {
                    delay(1000)
                    log("111")
                }
            }
        }
        // 当使用Super的时候 兄弟协程还是会存在的 当使用Job的时候 兄弟直接GG
        scope.launch(CoroutineName("job2")) {
            while (true) {
                delay(1000)
                log("兄弟协程还是活着的")
            }
        }
        // 测试Job的情况被干掉的时候 是否还能启动协程
        lifecycleScope.launch {
            delay(3000)
            lifecycleScope.launch{
                log("Scope is alive? " + scope.isActive)
            }
            delay(6000)
            log("Scope is alive? " + scope.isActive)
            scope.launch {
                log("Scope 启动新的协程")
            }
        }

    }
    /*总结： 上述例子的复杂性充分告诉我们
    * 1、 在你不是很了解的情况下 不要去自动以Scope尽量直接使用ViewModelScope和LifecycleScope
    * 2、 针对ViewModelScope等固定的（无法直接需改这个Scope） Scope 处理协程异常需要直接写在顶层
    * 协程
    * 3、 需要分清楚 SupervisorJob和Job的区别， 他们对父级和兄弟的异常处理干扰
    * 4、 搞不清楚就看看参考文献*/

    // 测试WithContext等的异常处理集合
    private fun testWrapper() {
        testCoroutineScopeWrapper1()
        testCoroutineScopeWrapper2()
        testWithContextWrapper1()
        testWithContextWrapper2()
    }


    private fun testCoroutineScopeWrapper1() {
        lifecycleScope.launch {
            try {
                testCoroutineScope()
            } catch (e: Exception) {
                log("try-catch ${e.message}")
            }
        }

    }

    private fun testCoroutineScopeWrapper2() {
        lifecycleScope.launch(topLaunchExceptionHandler) {
            testCoroutineScope()
        }

    }

    // coroutineScope 非常适合进行 async和多子协程的并发函数封装(这种适合死一个就都死的情况)
    // 1 继承上级的作用域 下级可以直接开启协程
    // 2 拦截协程内部的异常 直接在外层 test 函数中抛出异常 re-thrown
    // 3 使用re-thrown 特性保证函数的正常调用
    // 4 何种异常 都能捕获 非常TQL
    @Throws(Exception::class)
    private suspend fun testCoroutineScope(): Int = coroutineScope {
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
            // delay(100)
            throw IllegalArgumentException("job1 launch throw exception")
        }
        val job4 = async {
            delay(1)
            throw IllegalArgumentException("job4 async throw  exception")
        }

        // throw IllegalArgumentException("throw exception")
        return@coroutineScope 1
    }

    // 注意 加上异常处理器也是没用的 特性几乎和coroutineScope一模一样 异常向上抛出
    private fun testWithContextWrapper1() {
        lifecycleScope.launch {
            try {
                testWithContext()
            } catch (e: Exception) {
                log("try-catch ${e.message}")
            }
        }
    }

    private fun testWithContextWrapper2() {
        lifecycleScope.launch(topLaunchExceptionHandler) {
            testWithContext()
        }
    }

    @Throws(Exception::class)
    private suspend fun testWithContext(): Int =
        withContext(Dispatchers.IO + normalExceptionHandler) {
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
                // delay(100)
                throw IllegalArgumentException("job1 launch throw exception")
            }
            val job4 = async {
                delay(1)
                throw IllegalArgumentException("job4 async throw  exception")
            }

            // throw IllegalArgumentException("throw exception")
            return@withContext 1
        }

    /*
    * 和coroutineScope不同， supervisorScope会继承调用者的上下文（从而可以获取协程作用域开启子协程）
    * 但是本质上是具有和SupervisorJob一样的特性 异常传播和coroutineScope 也完全不同, 适用于SupervisorJob的场景就适合
    * supervisorScope, 适合Job的并发场景就选择 coroutineScope
    * 关于异常处理: 前者是re-thrown 后者是通过协程向上传播 传播到自身的 supervisorScope 这一级就到顶了
    * 如果没得异常处理器就直接崩溃 （当然 顶层协程域的异常处理器可以从调用者哪里继承下来 这就不会出问题）
    * 否则请在顶层协程(相对于supervisorScope)那里添加上异常处理器！
    * 核心: supervisorScope内部的协程 同比 当做顶层协程作用域 和 顶层协程看待 你就不会出错了
    * 异常处理器也是可以继承的!! 最外层给你搞一个异常处理器 supervisorScope就稳了
    * */
    @Throws(Exception::class)
    private suspend fun testSupervisorScope(): Int = supervisorScope {
        log(this.coroutineContext)
        // topLaunchExceptionHandler作为job1的顶级协程的异常处理器直接处理异常
        launch(topLaunchExceptionHandler + CoroutineName("job1")) {
            delay(100)
            throw IllegalArgumentException("job1 launch throw exception")
        }
        // 顶层作用域和协程本身都没得异常处理器 直接蹦
        launch(CoroutineName("job2")) {
            delay(100)
            throw IllegalArgumentException("job2 launch throw exception")
        }
        // throw IllegalArgumentException("throw exception")
        return@supervisorScope 1
    }


    // 针对 async的特殊处理
    /* 1、 async会存在抛出2次异常的情况下 */
    private fun testAsync1() {
        // 异常会直接
        lifecycleScope.launch(topLaunchExceptionHandler) {
            // 协程内部异常 完全符合协程向上传播的规律
            // 不加异常处理器的时候 直接崩掉 加上之后 d1.await 再次抛出异常 需要再次处理
            // 注意异常处理加错位置了， 加载这里没得效果
            val d1 = async(Dispatchers.IO + CoroutineName("d1")) {
                delay(1000)
                throw java.lang.IllegalArgumentException("主动抛出异常")
            }
            // d1.await貌似会抛出异常 但是同样可以被顶层的异常处理器解决 ?
            // or d1 根本就不会执行到 因为异常导致整个协程GG了
            // 是否可以尝试 不去为await添加额外的try-catch
            d1.await()
        }
    }

    // Async 作为顶层协程的时候 协程内部内不会抛异常 而是转移到await 那边抛出
    private fun testAsync2() {
        val d1 = lifecycleScope.async(Dispatchers.IO) {
            // 不会导致异常 而是通过d1.await的时候 抛出
            throw java.lang.IllegalArgumentException("顶层async主动抛出异常")
        }
        lifecycleScope.launch {
            delay(1000)
            // 为顶层协程添加处理器 也可以
            try {
                d1.await()
            } catch (e: Exception) {
                log("try-catch 捕获到 $e")
            }
        }

    }

    // 注意： lifecycleScope.async一般 不会用来作为顶层协程 他的返回值正常情况下在非协程作用域是拿不到的
    // 之所以有此已将 是因为 当他配合supervisorScope的时候 内部的直接子async启动的协会曾默认就是类似顶层协程
    // 的产物
    // 下面的例子 只要在 await捕获异常 就不需要在supervisorScope或者async设置异常处理器
    // Output: try-catch java.lang.IllegalArgumentException: d1 async throw exception
    private suspend fun testAsync3(): Int? = supervisorScope<Int?> {
        val d1 = async(Dispatchers.IO + CoroutineName("d1")) {
            delay(100)
            throw IllegalArgumentException("d1 async throw exception")
            1
        }

        val d2 = async(Dispatchers.IO + CoroutineName("d2")) {
            delay(100)
            2
        }

        val res: Int? = try {
            val v1 = d1.await()
            val v2 = d2.await()
            v1 + v2
        } catch (e: Exception) {
            log("try-catch $e")
            null
        }
        return@supervisorScope res
    }

    // 注意 自定义Scope的时候 记得注销作用域 避免造成泄露
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }


}