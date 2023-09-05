package cn.zhaojunchen.coroutinetest.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/12
 *     desc   :
 * </pre>
 */
class FlowTestViewModel : ViewModel() {
    private val flowSimple = flow<Int> {
        (1..10).forEach { it ->
            delay(100)
            emit(it)
        }
    }.onEach {
        /*public fun <T> Flow<T>.onEach(action: suspend (T) -> Unit): Flow<T> = transform { value ->
            action(value)
            return@transform emit(value)
        }*/
        // 非终端操作符 额外处理数据 在简单的情况下可以直接在里面写入逻辑
        println(it * 100)
    }

    fun start1() {
        flowSimple.launchIn(viewModelScope)
    }


}