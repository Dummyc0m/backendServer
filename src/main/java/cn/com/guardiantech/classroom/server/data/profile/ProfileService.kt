package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.api.IProfileService
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.user.User
import io.vertx.core.logging.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*

object ProfileService : AbstractDataService(){
    val logger = LoggerFactory.getLogger("ProfileService")
    val profilePluginFolder = File("./plugins/profile")
    val plugins:MutableList<IProfileService> = ArrayList<IProfileService>()

    init {
        if (profilePluginFolder.exists()) {
            profilePluginFolder.mkdirs()
        }
    }

    override fun initialize() {
        logger.info("Initializing ProfileService...")
        logger.info("Loading ProfilePlugins...")
        val files = profilePluginFolder.listFiles { file -> file.name.toLowerCase().endsWith(".jar")}
        logger.info("${files.size} Jar(s) found in plugins/profile folder")
        val urls :Array<URL> =  Array<URL>(files.size, {
            index ->
            files[index].toURI().toURL()
        })
        val classLoader = URLClassLoader(urls, this.javaClass.classLoader)
        val pluginLoader: ServiceLoader<IProfileService> = ServiceLoader.load(IProfileService::class.java, classLoader)
        pluginLoader.iterator().forEach { plugin ->

            plugins.add(plugin)
        }
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