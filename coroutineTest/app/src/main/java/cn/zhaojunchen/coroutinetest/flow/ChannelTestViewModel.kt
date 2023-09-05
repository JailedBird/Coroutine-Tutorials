package cn.zhaojunchen.coroutinetest.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.zhaojunchen.coroutinetest.log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/08
 *     desc   :
 * </pre>
 */
class ChannelTestViewModel : ViewModel() {

    // ShareFlow的emit默认会等待所有的处理过程 搞完 才会进行下一次emit
    // https://blog.csdn.net/weixin_44235109/article/details/121594988
    fun testShareFlowIsSuspend() {
        var value = 0
        val sharedFlow = MutableSharedFlow<Int>()
        /*viewModelScope.launch {

        }*/
    }

    private lateinit var channel: Channel<Int>


    private val _sh: MutableSharedFlow<String> = MutableSharedFlow<String>()
    val sh: SharedFlow<String>
        get() = _sh

    private val _st: MutableStateFlow<String> = MutableStateFlow("")
    val st: StateFlow<String>
        get() = _st


    fun test1() {
        viewModelScope.launch {
            // StateFlow
            st.collect {

            }

            _st.emit("1")

            // SharedFlow

            sh.replayCache


            st.collect {

            }
        }
    }

}