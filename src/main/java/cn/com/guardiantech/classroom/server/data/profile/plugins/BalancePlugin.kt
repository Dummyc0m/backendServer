package cn.com.guardiantech.classroom.server.data.profile.plugins

import cn.com.guardiantech.classroom.server.api.IProfileService
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger

/**
 * Created by Codetector on 2016/12/31.
 */
class BalancePlugin : IProfileService{
    override fun getServiceName(): String {
        return "balance"
    }

    override fun isDataValid(data: String): Boolean {
        return data.startsWith("{") && data.endsWith("}")
    }
}