package cn.zhaojunchen.coroutinetest

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/18
 *     desc   :
 * </pre>
 */

// 用于测试函数反编译相关的内容
class T : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
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
    }

}

class DecClassContent {
    //定义一个User实体类
    data class User(val name: String)

    //定义一个函数模拟耗时获取User对象
    suspend fun getUser(): User {
        println("假的挂起函数${Thread.currentThread()}")
        Thread.sleep(1000)
        return User("openXu")
    }

    suspend fun test() = withContext(Dispatchers.IO) {
        return@withContext 1
    }

    suspend fun testSplitFunction() = coroutineScope {
        val d1: Deferred<Int?> = async(Dispatchers.IO) {
            try {
                // do d1
                delay(1000)
                100
            } catch (e: Exception) {
                null
            }
        }
        val d2 = async(Dispatchers.Default) {
            try {
                // do d1
                delay(1000)
                100
            } catch (e: Exception) {
                null
            }
        }
        val s1 = d1.await()
        val s2 = d2.await()
        return@coroutineScope if (s1 != null && s2 != null) {
            s1 * s2
        } else {
            -1
        }
    }
}

fun main() {
    GlobalScope.launch(Dispatchers.IO) {
        println(this)
        val job1 = launch(Dispatchers.IO) {
            println(this)
            println("job1 using launch launch")
        }
        val job2 = this.launch {
            println(this)
            println("job2 using this launch")
        }
    }


}