package cn.com.guardiantech.classroom.server.api

import io.vertx.core.json.JsonObject

interface IProfileService {
    fun getServiceName() :String
    open fun isDataValid(data: String): Boolean {
        return true
    }
    open fun getDefaultData(): String {
        return JsonObject().toString()
    }
    open fun parseData(data: String): JsonObject {
        return JsonObject(data)
    }
    open fun encodeData(data: JsonObject): String {
        return data.toString()
    }
}