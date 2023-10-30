package cn.zhaojunchen.coroutinetest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/17
 *     desc   :
 * </pre>
 */

class TestLaunch01Activity : AppCompatActivity() {
    companion object {
        const val MAX = Long.MAX_VALUE

        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, TestLaunch01Activity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_launch01)
        // testLaunch001()
        // testLaunch002()
        testLaunch000()
    }
    // 子协程执行无线任务时候 是不会执行退出的 父协程会等待子协程执行完毕 除非自己cancel
    private fun testLaunch000() {
        // 外层协程启动子协程 执行无限delay
        lifecycleScope.launch{
            log("测试父子协程")
            launch {
                while (true) {
                    delay(1000)
                    log("父子协程嵌套")
                }
            }

            lifecycleScope.launch {
                while (true) {
                    delay(1000)
                    log("同级别协程嵌套")
                }
            }
            log("父子协程Block结束，观测两种协程是否停止！")
            // cancel()
        }
    }

    // 测试协程的父子关系
    /* 0、 Scope提供的这个东西叫做 称之为 顶层作用域， 基于此的launch称之为顶层协程 他们之间形成了父子关系
       不过看起来有点特殊 是 域 和 协程 之间的一个 父子关系 ， 协程中开启子协程 也是父子关系 他们是 协程-协程的父子关系
       参考图片: https://zhaojunchen-1259455842.cos.ap-nanjing.myqcloud.com/img/20220719175634.png

    *  1、 协程内部 通过launch直接启动的才叫 其子协程 他们呈现父子关系
    *  2、 协程内部通过顶级作用域启动的协程 他们之间不具有父子关系 也强烈不建议这么写
    * */

    // 测试结果 干掉Job1 Job2仍然执行 Job3挂掉  根本原因就是在于子协程的问题
    private fun testLaunch001() {
        log("顶层作用域 ${lifecycleScope.coroutineContext}")

        val job1 = lifecycleScope.launch {
            log("顶层协程job1 ${this.coroutineContext}")
            val job2 = lifecycleScope.launch {
                log("顶层协程job2 ${this.coroutineContext}")
                while (true) {
                    delay(1000)
                    log("job2活着")
                }
            }
            // job1的直接子协程 受到job1的生命周期相关限制
            val job3 = launch {
                log("job1子协程job3 ${this.coroutineContext}")
                while (true) {
                    delay(1000)
                    log("job3活着")
                }
            }

            while (true) {
                delay(1000)
                log("job1活着")
            }
        }
        // 5S 之后 主动干掉job1
        lifecycleScope.launch {
            delay(5000)
            job1.cancel()
            log("4000 later, cancel job1")
        }
    }

    // 本次的测试函数主要是用来测试他们上下文之间的继承关系, 特别是针对 异常传播时候的SupervisorJob和JobSupport关系
    private fun testLaunch002() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        job1 = lifecycleScope.launch {
            log("job1上下文: ${this.coroutineContext}")
            job2 = lifecycleScope.launch(Dispatchers.Default) {
                log("job2上下文: ${this.coroutineContext}")
            }
            job3 = launch(CoroutineName("Job3")) {
                log("job3上下文: ${this.coroutineContext}")
                val job5 = launch(CoroutineName("Job5")) {
                    log("job5上下文: ${this.coroutineContext}")
                }
            }
            job4 = launch(CoroutineName("Job3")) {
                log("job4上下文: ${this.coroutineContext}")
                val job6 = launch(CoroutineName("Job5")) {
                    log("job6上下文: ${this.coroutineContext}")
                }
            }
            val job7 = launch(Dispatchers.IO) {
                log("job7上下文: ${this.coroutineContext}")
                /*while (true){
                    delay(1000)
                    log("job7 is alive")
                }*/
            }
        }
        // 这个是 协程完成时 进行的回调注册 当且仅当 所有子协程完成 当前协程才会完成
        // 作为测试 你在job7添加死循环 就会发现这个回调永远不会执行
        job1.invokeOnCompletion {
            log("job1注册的完成回调生效")
        }
    }

    // 看一个不切换线程导致的 协程阻塞问题
    fun testLaunch003() {
        var job1: Job? = null
        var job2: Job? = null

        /*
        * launch 会立即返回协程的job 但是
        * 问题在于 此处启动的协程 应该是（和具体的调度时机有关系， 这块代码还没仔细深入）立即调度
        * 默认就是主线程 因此主线程被卡主 从而导致 testLaunch3后续的主线程代码无法执行
        * after job1 init 应该也不会输出
        * 因此：协程内部执行耗时代码 请记得切换线程到IO等线程*/
        job1 = lifecycleScope.launch {
            var count = 0L
            while (true) {
                // 耗时任务
                count++
            }
        }
        log("after job1 init")
    }

    // 测试 Async
    fun testAsync001() {
        // d1 d2 强关联
        lifecycleScope.launch {
            // 立即返回 d1 Deferred 立即在IO中调度执行
            val d1 = async(Dispatchers.IO + CoroutineName("D1")) {
                delay(1000)
                log("${this.coroutineContext}")
                System.currentTimeMillis()
            }
            // 立即返回 d2 Deferred 立即在IO中调度执行
            val d2 = async(Dispatchers.IO + CoroutineName("D2")) {
                delay(1002)
                log(" ${this.coroutineContext}")
                System.currentTimeMillis()
            }
            // 阻塞当前协程 直接 d1结束
            val v1 = d1.await()
            val v2 = d2.await()
            log(v1 + v2)
        }
    }


    // 测试协成 顶层作用域和顶层协程的异常拦截  此处主要针对lifecycleScope(viewModelScope和lifecycleScope的顶层作用域相同规则同样适用)
    fun testLaunch004() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        var job5: Job? = null
        // 顶层coroutine  [来自SupervisorJob的顶层lifecycleScope]
        // 注意 job1 如果不注册拦截器则则会直接崩溃(这个异常也是在这里处理的)
        job1 = lifecycleScope.launch(coroutineExceptionHandler) {
            job4 = launch {
                delay(MAX)
            }
            delay(1000) // 确保后续协程开始执行
            throw IllegalArgumentException("job1 主动抛出异常")
        }

        job2 = lifecycleScope.launch {
            delay(100000)
        }
        job3 = lifecycleScope.launch {
            delay(100000)
        }

        lifecycleScope.launch {
            // wait
            delay(2000)
            log("job1 lives? ${job1.isActive} ")
            log("job2 lives? ${job2.isActive} ")
            log("job3 lives? ${job3.isActive} ")
            log("job4 lives? ${job4?.isActive} ")
        }

    }
    // testLaunch004 充分说明lifecycleScope启动的顶层协程会拦截到异常 上述的job1如果未注册异常拦截器 则会直接崩溃
    // 针对 lifecycleScope的顶层协程不会将异常上抛, 也不会干扰到其他的兄弟顶层协程  可以认为这个异常被job1 消费了
    //║捕获到异常: job1 主动抛出异常
    //║job1 lives? false
    //║job2 lives? true
    //║job3 lives? true
    //║job4 lives? false

    // 测试协成 顶层作用域和顶层协程的异常拦截  此处主要针对lifecycleScope(viewModelScope和lifecycleScope的顶层作用域相同规则同样适用)
    fun testLaunch005() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        var job5: Job? = null
        // 顶层coroutine  [来自SupervisorJob的顶层lifecycleScope]
        // 注意 job1 如果不注册拦截器则则会直接崩溃(这个异常也是在这里处理的)
        job1 = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            log("job1捕获到异常: ${throwable.message}")
        }) {
            job4 = launch(CoroutineExceptionHandler { _, throwable ->
                log("job4捕获到异常: ${throwable.message}")
            }) {
                delay(1000)
                throw IllegalArgumentException("job4 主动抛出异常")
            }
            delay(MAX)

        }

        job2 = lifecycleScope.launch {
            delay(MAX)
        }
        job3 = lifecycleScope.launch {
            delay(MAX)
        }

        lifecycleScope.launch {
            // wait
            delay(2000)
            log("job1 lives? ${job1.isActive} ")
            log("job2 lives? ${job2.isActive} ")
            log("job3 lives? ${job3.isActive} ")
            log("job4 lives? ${job4?.isActive} ")
        }

    }
    // testLaunch005 充分说明lifecycleScope启动的顶层协程会拦截到异常 顶层协程的子协程是不能捕获到异常的，他需要通过树的关系传递这个异常 知道lifecycleScope的顶层协程 上述的job1如果未注册异常拦截器 则会直接崩溃
    // 针对 lifecycleScope的顶层协程不会将异常上抛, 也不会干扰到其他的兄弟顶层协程  可以认为这个异常被job1 消费了  其他的和testLaunch004是一致的
    //║job1捕获到异常: job4 主动抛出异常
    //║job1 lives? false
    //║job2 lives? true
    //║job3 lives? true
    //║job4 lives? false


    // 如何修复5中的问题，即是让job4能自动捕获到异常
    fun testLaunch006() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        var job5: Job? = null
        // 顶层coroutine  [来自SupervisorJob的顶层lifecycleScope]
        // 注意 job1 如果不注册拦截器则则会直接崩溃(这个异常也是在这里处理的)
        job1 = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            log("job1捕获到异常: ${throwable.message}")
        }) {
            // 模拟 lifecycleScope的顶层作用域实现 (不建议使用launch(SupervisorJob()) 这样非常迷惑)
            supervisorScope {
                job4 = launch(CoroutineExceptionHandler { _, throwable ->
                    log("job4捕获到异常: ${throwable.message}")
                }) {
                    delay(1000)
                    throw IllegalArgumentException("job4 主动抛出异常")
                }
            }
            delay(MAX)
        }

        job2 = lifecycleScope.launch {
            delay(MAX)
        }
        job3 = lifecycleScope.launch {
            delay(MAX)
        }

        lifecycleScope.launch {
            // wait
            delay(2000)
            log("job1 lives? ${job1.isActive} ")
            log("job2 lives? ${job2.isActive} ")
            log("job3 lives? ${job3.isActive} ")
            log("job4 lives? ${job4?.isActive} ")
        }

    }

    // testLaunch006 充分说明lifecycleScope启动的顶层协程会拦截到异常 顶层协程的子协程是不能捕获到异常的，他需要通过树的关系传递这个异常 知道lifecycleScope的顶层协程 上述的job1如果未注册异常拦截器 则会直接崩溃
    // 针对 lifecycleScope的顶层协程不会将异常上抛, 也不会干扰到其他的兄弟顶层协程  可以认为这个异常被job1 消费了  其他的和testLaunch004是一致的
    //║job4捕获到异常: job4 主动抛出异常
    //║job1 lives? false
    //║job2 lives? true
    //║job3 lives? true
    //║job4 lives? false


    // 如何修复5中的问题，即是让job4能自动捕获到异常
    fun testLaunch007() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        var job5: Job? = null
        // 顶层coroutine  [来自SupervisorJob的顶层lifecycleScope]
        // 注意 job1 如果不注册拦截器则则会直接崩溃(这个异常也是在这里处理的)
        job1 = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            log("job1捕获到异常: ${throwable.message}")
        }) {
            // 模拟 lifecycleScope的顶层作用域实现 (不建议使用launch(SupervisorJob()) 这样非常迷惑)
            supervisorScope {
                job4 = launch(CoroutineExceptionHandler { _, throwable ->
                    log("job4捕获到异常: ${throwable.message}")
                }) {
                    delay(1000)
                    throw IllegalArgumentException("job4 主动抛出异常")
                }
            }
            delay(MAX)
        }

        job2 = lifecycleScope.launch {
            delay(MAX)
        }
        job3 = lifecycleScope.launch {
            delay(MAX)
        }

        lifecycleScope.launch {
            // wait
            delay(2000)
            log("job1 lives? ${job1.isActive} ")
            log("job2 lives? ${job2.isActive} ")
            log("job3 lives? ${job3.isActive} ")
            log("job4 lives? ${job4?.isActive} ")
        }

    }

    // testLaunch007 充分说明supervisorScope这个“顶层作用域”启动的顶层协程job4相当于一个顶层协程，效果和lifecycleScope的顶层协程类似
    // 注意这里他实际阻止了job4的异常向外层传播 可以看到job1还是处在活跃状态的
    //║job4捕获到异常: job4 主动抛出异常
    //║job1 lives? true
    //║job2 lives? true
    //║job3 lives? true
    //║job4 lives? false


    // 使用supervisorScope模拟 lifecycleScope 继续套娃
    fun testLaunch008() {
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null
        var job4: Job? = null
        var job5: Job? = null
        var job6: Job? = null
        job1 = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            log("job1捕获到异常: ${throwable.message}")
        }) {
            // 模拟 lifecycleScope的顶层作用域实现 (不建议使用launch(SupervisorJob()) 这样非常迷惑)
            supervisorScope {
                job4 = launch(CoroutineExceptionHandler { _, throwable ->
                    log("job4捕获到异常: ${throwable.message}")
                }) {
                    job5 = launch(CoroutineExceptionHandler { _, throwable ->
                        log("job5捕获到异常: ${throwable.message}")
                    }) {
                        delay(1000)
                        throw IllegalArgumentException("job5 主动抛出异常")
                    }
                    job6 = launch {
                        delay(MAX)
                    }
                    delay(MAX)
                }
            }
            delay(MAX)
        }

        job2 = lifecycleScope.launch {
            delay(MAX)
        }
        job3 = lifecycleScope.launch {
            delay(MAX)
        }

        lifecycleScope.launch {
            // wait
            delay(2000)
            log("job1 lives? ${job1.isActive} ")
            log("job2 lives? ${job2.isActive} ")
            log("job3 lives? ${job3.isActive} ")
            log("job4 lives? ${job4?.isActive} ")
            log("job5 lives? ${job5?.isActive} ")
            log("job6 lives? ${job6?.isActive} ")
        }

    }
    // ║job4捕获到异常: job5 主动抛出异常
    // ║job1 lives? true
    // ║job2 lives? true
    // ║job3 lives? true
    // ║job4 lives? false
    // ║job5 lives? false
    // ║job6 lives? false

    // 直接的launch是如何实现继承上级的上下文
    // public fun CoroutineScope.launch(
    //     context: CoroutineContext = EmptyCoroutineContext,
    //     start: CoroutineStart = CoroutineStart.DEFAULT,
    //     block: suspend CoroutineScope.() -> Unit
    // ): Job {
    // 思考1 为啥 block 是 CoroutineScope.()-> Unit
    // 答案： 最终Block会被包装为 StandaloneCoroutine 的协程 -> CoroutineContext
    // 因此 这个block中的this 直接可以获取上级（而非顶级的lifecycleScope）的上下文 从而可以实现
    // 一级一级的继承 下面的job3则是直接继承了顶级作用 导致无法继承Name上下文 他不是一个合法的父子协程！

    // ═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ║Thread: main
    // ╟───────────────────────────────────────────────────────────────────────────────────────────────────
    // ║lifeCycleScope is: [SupervisorJobImpl{Active}@597e889, Dispatchers.Main.immediate]
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ║Thread: DefaultDispatcher-worker-1
    // ╟───────────────────────────────────────────────────────────────────────────────────────────────────
    // ║top launch scope is: [CoroutineName(Fuck you), StandaloneCoroutine{Active}@5152cbc, Dispatchers.Default]
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ║Thread: DefaultDispatcher-worker-2
    // ╟───────────────────────────────────────────────────────────────────────────────────────────────────
    // ║sub1-1 launch scope is: [CoroutineName(Fuck you), StandaloneCoroutine{Active}@d6c879a, Dispatchers.IO]
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ║Thread: DefaultDispatcher-worker-2
    // ╟───────────────────────────────────────────────────────────────────────────────────────────────────
    // ║sub1-2 launch scope is: [CoroutineName(Fuck you), StandaloneCoroutine{Active}@3a2d8cb, Dispatchers.Default]
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════════════
    // ║Thread: main
    // ╟───────────────────────────────────────────────────────────────────────────────────────────────────
    // ║lifecycle-top-child launch scope is: [StandaloneCoroutine{Active}@c339643, Dispatchers.Main.immediate]
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════════════
    fun testLaunchImplement() {
        log("lifeCycleScope is: $lifecycleScope")
        log("lifeCycleScope is: ${lifecycleScope.coroutineContext}")
        lifecycleScope.launch(Dispatchers.Default + CoroutineName("Fuck you")) {
            log("top launch scope is: $this")
            log("top launch scope is: ${this.coroutineContext}")
            val job1 = launch(Dispatchers.IO) {
                log("sub1-1 launch scope is: $this")
                log("sub1-1 launch scope is: ${this.coroutineContext}")
            }
            val job2 = launch {
                log("sub1-2 launch scope is: ${this.coroutineContext}")
            }
            val job3 = lifecycleScope.launch {
                log("lifecycle-top-child launch scope is: ${this.coroutineContext}")
            }
        }
    }
}