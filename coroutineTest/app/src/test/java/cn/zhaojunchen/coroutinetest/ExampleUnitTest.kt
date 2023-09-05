package cn.zhaojunchen.coroutinetest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
     fun demo() {
        runBlocking {
            /*val flow = listOf(1, 2, 3, 4, 5, 6).asFlow().flowOn(Dispatchers.IO).onEach {
                ("${Thread.currentThread().name} $it")
            }

            // flow.launchIn(GlobalScope)
            GlobalScope.launch {
                flow.collect {
                    print(it)
                }
            }.join()*/
            print("100")
        }

    }

}