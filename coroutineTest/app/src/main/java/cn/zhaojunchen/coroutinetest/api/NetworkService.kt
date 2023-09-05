package cn.zhaojunchen.coroutinetest.api

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * <pre>
 *     author : junwuming
 *     e-mail : zjc986812982@163.com
 *     time   : 2022/07/25
 *     desc   :
 * </pre>
 */

interface NetworkService {

    @GET("/hotkey/json")
    suspend fun getHotkey(): Any
}
