package cn.zhaojunchen.coroutinetest

import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext


/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/31
 *     desc   :
 * </pre>
 */
/**
 * CoroutineExceptionHandlerImplKt.class using jvm ServiceLoader to load our own CoroutineExceptionHandler,
 * from CoroutineExceptionHandlerImplKt 's handleCoroutineExceptionImpl(), Global coroutine exception can not prevent
 * your app crash, please see AndroidExceptionPreHandler's file
 * */
class GlobalCoroutineExceptionHandler : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        ToastUtils.showShort("捕获到全局异常: ${exception.message}")
    }

}