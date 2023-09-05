package cn.zhaojunchen.coroutinetest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/*仅仅作为测试代码编写的空间 防止污染其他代码*/
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val startTime = System.currentTimeMillis()
        val job = lifecycleScope.launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0

            while (i < 5) {
                ensureActive()
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    log("Hello ${i++}")
                    nextPrintTime += 500L
                }
            }
        }
        job.invokeOnCompletion {
            log("结束 ${it}")
        }
        Thread.sleep(1000L)
        log("Cancel!")
        job.cancel()
        Thread.sleep(2)
        log(job.isCancelled)
        log(job.isCompleted)
        log("Done!")
    }

    // 协程的取消是一个协同操作
    /*
    * 当你的任务是一个耗时操作的时候 需要支持取消操作
    * 取消本身没得特殊的地方 它想表达的是 中途退出的过程
    * 协同处理无法突出 他就会一直执行等完毕 这个时候协程才会退出
    * 因此无法中途取消 取消操作除了状态改变 其他啥都无法改变
    *
    * 当Job抛出取消操作 上级会对这个操作视而不见 不做任何处理 也不会崩溃
    * 下面的例子即是 不做取消支持 Job会一直执行 知道彻底结束 协会状态cancel一直会维持
    * 最后彻底执行完毕之后 才会变为完成
    *
    * 支持协同操作的
    * 1、 调用如delay等库函数
    * 2、 调用yield
    * 3、 检查isActive 后续逻辑自己处理
    * 4、 ensureActive 检查isActive 并且抛取消异常
    * */
    fun testCancel001() {
        val startTime = System.currentTimeMillis()
        val job = lifecycleScope.launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0

            while (i < 5) {
                ensureActive()
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    log("Hello ${i++}")
                    nextPrintTime += 500L
                }
            }
        }
        job.invokeOnCompletion {
            log("结束 $it")
        }
        Thread.sleep(1000L)
        log("Cancel!")
        job.cancel()
        Thread.sleep(2)
        log(job.isCancelled)
        log(job.isCompleted)
        log("Done!")
    }


    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, TestActivity::class.java)
            context.startActivity(starter)
        }
    }
}