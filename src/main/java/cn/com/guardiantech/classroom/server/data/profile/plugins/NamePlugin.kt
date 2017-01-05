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

    override fun generateDefault(vararg params: String): String {
        if (params.isNotEmpty())
            return JsonObject().put("name", params[0]).toString()
        throw IllegalArgumentException("One arg is required")
    }
}