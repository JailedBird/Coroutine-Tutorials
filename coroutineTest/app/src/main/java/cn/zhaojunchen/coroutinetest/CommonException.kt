package cn.zhaojunchen.coroutinetest

import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/17
 *     desc   :
 * </pre>
 */
val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    log("捕获到异常: ${throwable.message}")
}

fun getCoroutineException(tag: String) = CoroutineExceptionHandler { _, throwable ->
    log("$tag 捕获到异常: ${throwable.message}")
}