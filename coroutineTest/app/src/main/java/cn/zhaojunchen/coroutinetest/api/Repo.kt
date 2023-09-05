package cn.zhaojunchen.coroutinetest.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/25
 *     desc   :
 * </pre>
 */
// https://openxu.blog.csdn.net/article/details/115636089
object Repo {
    private const val BASE_URL: String = "https://www.wanandroid.com"

    private val okHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val networkService: NetworkService = retrofit.create(NetworkService::class.java)

}