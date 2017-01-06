package cn.com.guardiantech.classroom.server.data.profile.plugins

import cn.com.guardiantech.classroom.server.api.IProfileService
import io.vertx.core.json.JsonObject

/**
 * Created by Codetector on 2017/1/4.
 */
class NamePlugin: IProfileService{
    override fun getServiceName(): String {
        return "name"
    }

    override fun isDataValid(data: String): Boolean {
        if (data.startsWith("{") && data.endsWith("}")) {
            val json = JsonObject(data)
            return json.containsKey("version") && json.getInteger("version") == 1
        } else {
            return false
        }
    }

    override fun upgradeData(oldData: String): String {
        return JsonObject().put("version", 1).put("name", oldData).toString()
    }

    override fun generateDefault(vararg params: String): String {
        if (params.isNotEmpty())
            return JsonObject().put("name", params[0]).toString()
        throw IllegalArgumentException("One arg is required")
    }
}