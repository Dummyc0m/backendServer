package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.api.IProfileService
import cn.com.guardiantech.classroom.server.data.user.User
import java.util.*

/**
 * Created by Codetector on 2017/1/5.
 */
class UserProfile() {
    val pluginData: MutableMap<String, String> = HashMap<String, String>()
    private var changed = false

    constructor(data: Map<String, String>): this(){
        pluginData.putAll(data)
    }

    private fun markChange () {
        this.changed = true
        ProfileService.markChange()
    }

    fun hasChange (): Boolean{
        return this.changed
    }

    fun get(plugin: IProfileService): String {
        val dataFromMap = pluginData[plugin.getServiceName()]
        if (dataFromMap != null) {
            if (plugin.isDataValid(dataFromMap)) {
                return dataFromMap
            } else {
                set(plugin, plugin.generateDefault())
            }
        }
        return plugin.generateDefault()
    }

    fun set(plugin: IProfileService, data: String, triggerUpdate: Boolean = true) {
        pluginData.put(plugin.getServiceName(), data)
        if (triggerUpdate)
            markChange()
    }
}