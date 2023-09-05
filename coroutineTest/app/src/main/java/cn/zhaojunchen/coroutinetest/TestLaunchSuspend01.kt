package cn.zhaojunchen.coroutinetest


import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/17
 *     desc   :
 * </pre>
 */

/*Launch函数注释中明确说明Launch是一个非阻塞函数
但是如果不存在线程切换的情况下 可能就会变为一个 “阻塞函数”了 根本原因是一个线程同时只能执行一处代码
下面的例子中 使用 lifecycleScope的主线程launch(不切换线程) 在协程中执行一个 耗时函数 Thread.sleep()
由于默认是立即执行代码 回导致协程所在做主线程立即执行 耗时任务 从而卡主主线程，无法返回主线程 导致“阻塞”
 解决方案 使用Dispatcher切换一次线程 此时立即启动的耗时任务会在其他线程中执行 保证当前线程的不卡*/
fun AppCompatActivity.testLaunchSuspend01() {
    val start = System.currentTimeMillis()

    val job1 = lifecycleScope.launch {
        // 模拟耗时任务
        Thread.sleep(4000)
    }

    val job2 = lifecycleScope.launch {
        Thread.sleep(4000)
    }

    val job3 = lifecycleScope.launch(Dispatchers.IO) {
        Thread.sleep(4000)
    }
    // 类比job1 如果使用挂起 函数执行 由于当前子协程直接挂起 同样不会卡主线线程
    val job4 = lifecycleScope.launch {
        delay(4000)
    }

    log("Ending this test, spend times: ${System.currentTimeMillis() - start}")

}
// 1、 这个例子中输出：
// Ending this test, spend times: 27
// End outer block
// 可以看到 27Ms的耗时 说明 End输出早于  Thread.sleep(8000) 的执行 说明launch的执行 是非阻塞的
// 但是屏幕会出现8s的卡顿说明 Thread.sleep(8000) 在launch中执行的时候确实卡主主线程
// 最终 最外层launch完全占用了线程 导致  "End outer block" 输出最后
// 结论 ：1、 主线程launch不能执行耗时任务 2、 launch是非阻塞的 但是同一个线程执行多个协程 会造成 其中某一个协程
// 暂时不执行 类似于 阻塞的launch 这点需要特别注意 具体项目 可能需要看源代码才能知道具体的调度

fun AppCompatActivity.testLaunchSuspend02() {
    val start = System.currentTimeMillis()
    lifecycleScope.launch {
        var i = 10000
        var t = 0
        while (i-- > 0) {
            t++
        }
        val job1 = launch {
            // 模拟耗时任务
            Thread.sleep(8000)
        }
        /*val job2 = launch(Dispatchers.IO) {
            Thread.sleep(4000)
        }*/
        // 类比job1 如果使用挂起 函数执行 由于当前子协程直接挂起 同样不会卡主线线程
        /*val job3 =launch {
            delay(4000)
        }*/
        i = 10000
        t = 0
        while (i-- > 0) {
            t++
        }

        log("Ending this test, spend times: ${System.currentTimeMillis() - start}")
    }
    log("End outer block")
}


