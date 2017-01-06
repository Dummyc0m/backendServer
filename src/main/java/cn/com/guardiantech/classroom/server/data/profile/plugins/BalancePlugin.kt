package cn.com.guardiantech.classroom.server.data.profile.plugins

import cn.com.guardiantech.classroom.server.api.IProfileService
import com.google.common.primitives.Doubles
import com.google.common.primitives.Longs
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger

/**
 * Created by Codetector on 2016/12/31.
 */
class BalancePlugin : IProfileService {
    override fun getServiceName(): String {
        return "balance"
    }

    override fun isDataValid(data: String): Boolean {
        return (
                if (data.startsWith("{") && data.endsWith("}")) {
                    val json = JsonObject(data)
                    json.containsKey("version") && json.getInteger("version") == 1
                } else {
                    false
                }
                )
    }

    override fun generateDefault(vararg params: String): String {
        var balance = 0.0
        return JsonObject().put("version", 1).put("balance", balance).toString()
    }
}