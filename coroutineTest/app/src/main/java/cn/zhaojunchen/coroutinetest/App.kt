package cn.zhaojunchen.coroutinetest

import android.app.Application
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/05/17
 *     desc   :
 * </pre>
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        buildXLogConfig()
    }

    /**
     * init XLog for debug log
     * */
    private fun buildXLogConfig() {
        val config = LogConfiguration.Builder()
            .logLevel(LogLevel.ALL)
            .enableThreadInfo()
            .enableBorder()
            .build()
        val androidPrinter: Printer = AndroidPrinter(true)
        XLog.init(config, androidPrinter)
    }
}