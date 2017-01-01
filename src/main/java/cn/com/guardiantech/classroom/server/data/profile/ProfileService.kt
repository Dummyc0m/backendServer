package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.api.IProfileService
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.user.User
import io.vertx.core.logging.LoggerFactory
import java.io.File
import java.util.*

object ProfileService : AbstractDataService(){
    val logger = LoggerFactory.getLogger("ProfileService")
    val profilePluginFolder = File("./plugins/profile")
    val pluginList:MutableList<IProfileService> = ArrayList<IProfileService>()

    init {
        if (profilePluginFolder.exists()) {
            profilePluginFolder.mkdirs()
        }
    }

    override fun initialize() {
        logger.info("Initializing ProfileService...")
        logger.info("Loading ProfilePlugins...")
        logger.info("${profilePluginFolder.listFiles { file -> file.name.toLowerCase().endsWith(".jar")}.size} Jar(s) found in plugins/profile folder")

    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
        
    }

    fun fetchUserProfile (user: User) {
        dbClient.getConnection { con ->
            if (con.succeeded()) {

            }
        }
    }
}