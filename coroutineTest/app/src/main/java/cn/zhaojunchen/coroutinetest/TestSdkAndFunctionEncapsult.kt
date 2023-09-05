package cn.zhaojunchen.coroutinetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.zhaojunchen.coroutinetest.api.Repo
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/05
 *     desc   :
 * </pre>
 */
// 本节 主要讲述如何处理 利用协程提供的工具 进行三方工具的封装

suspend fun testCoroutineScope() = coroutineScope {
    /* val data = async(Dispatchers.IO) { // <- extension on current scope
     ... load some UI data for the Main thread ...
    }

    withContext(Dispatchers.Main) {
        doSomeWork()
        val result = data.await()
        display(result)
    }*/
}

/*Creates a CoroutineScope with SupervisorJob and calls the specified suspend block with this scope.、
 The provided scope inherits its coroutineContext from the outer scope, but overrides context's Job with
  SupervisorJob. A failure of a child does not cause this scope to fail and does not affect its other
   children, so a custom policy for handling failures of its children can be implemented. See SupervisorJob
   for details. A failure of the scope itself (exception thrown in the block or cancellation) fails the scope
   with all its children, but does not cancel parent job.*/
suspend fun testSuperCoroutineScope() = supervisorScope {

}

// 支持retrofit的取消
fun ViewModel.testRetrofit() {
    /**
     * [retrofit2.KotlinExtensions.await]
     * */
    viewModelScope.launch(Dispatchers.IO) {
        val res = try {
            Repo.networkService.getHotkey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log(res)
    }

}

/*
* 这个函数是用来 为函数提供手动切换线程的能力 当内部使用到线程等操作 就需要使用这个Continuation来手动的切换
* 将异常或者结果抛出到协程（其所在的线程）的续体continuation 从而让协程上下文收到这个数据 参考retrofit就是
* 使用的这个方案用于为协程部分封装非协程代码调用和返回
* Notes: 除非是为三方SDK(已经使用线程进行相关开发)添加协程的兼容（就像retrofit） 否咋一般不会再协程的开发中直接
* 是用Java Thread进行相关的操作的 不然还要这个协程干嘛
* */

suspend fun testSuspendCancellableCoroutine() = suspendCancellableCoroutine<Int> {
    val a = 1
    val b = 2
    // 这里没有真正的异步
    it.resume(a + b)
}


suspend fun testSuspendCancellableCoroutine1() = suspendCancellableCoroutine<Long> {
    val a = 2
    val b = 12
    // 这里没有真正的异步
    thread {
        try {
            val value = doSpendWork(a, b, it)
            it.resume(value)
        } catch (e: Exception) {
            it.resumeWithException(e)
        }
    }
}

// 模拟耗时计算
// 关于支持取消 可以看这里:
// https://developer.android.google.cn/kotlin/coroutines/coroutines-best-practices#coroutine-cancellable
fun doSpendWork(a: Int, b: Int, continuation: CancellableContinuation<Long>): Long {
    var count = 1000000
    var i = 0
    while (count-- > 0 && continuation.isActive) {
        i += 1
    }
    // 处于非活跃的续体  支持取消的响应 看你是直接抛出异常 还是直接返回咋地
    if (!continuation.isActive) {
        throw CancellationException("cancel!")
    }
    return System.currentTimeMillis()
}


// 原理同上 但是不支持协程的取消 不建议在项目中使用
suspend fun testSuspendCoroutine() = suspendCoroutine<Int> {
    val a = 1
    val b = 2
    it.resume(a + b)
}