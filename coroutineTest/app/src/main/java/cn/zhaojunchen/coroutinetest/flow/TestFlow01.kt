package cn.zhaojunchen.coroutinetest.flow

import cn.zhaojunchen.coroutinetest.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*


/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/17
 *     desc   : Test flow
 * </pre>
 */

// 测试协成的父子关系 这个例子证明直接使用:
suspend fun testFlow000() {
    flow {
        for (i in 1..3) {
            delay(100)
            emit(i)
        }
    }.flowOn(Dispatchers.Main)
        .map {
            it * it
        }.flowOn(Dispatchers.Main)
        .collect { res ->
            log(res)
        }

}

// 使用Flow进行任意线程的切换
// 1 当且仅当 collect开始收集的时候 才会进行流的触发和变化
// 2 flowOn可以实现任意线程的切换, flowOn作用的域仅为上面的那一行
suspend fun testFlow001() {
    simple().flowOn(Dispatchers.Main)
        .map {
            it * it
        }.flowOn(Dispatchers.Main)
        .collect { res ->
            log(res)
        }
}


fun simple(): Flow<Int> = flow {
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        /* log("flow $i ${Thread.currentThread().name}")*/
        emit(i)
    }
}







