package cn.com.guardiantech.classroom.server.data.profile.plugins

import cn.com.guardiantech.classroom.server.api.IProfileService
import io.vertx.core.json.JsonObject

class PhonePlugin: IProfileService {
    override fun getServiceName(): String {
        return "phone"
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
        return JsonObject().put("version", 1).put("countrycode", "+86").put("phone", oldData).toString()
    }

    override fun generateDefault(vararg params: String): String {
        return JsonObject().put("version", 1).put("countrycode", ""). put("phone", "").toString()
    }
}