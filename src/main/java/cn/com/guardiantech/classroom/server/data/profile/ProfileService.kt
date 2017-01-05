package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.api.IProfileService
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.user.User
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import org.reflections.Reflections
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*

object ProfileService : AbstractDataService(){
    val logger = LoggerFactory.getLogger("ProfileService")
    val profilePluginFolder = File("./plugins/profile")
    val plugins:MutableMap<String, IProfileService> = HashMap<String, IProfileService>()

    init {
        if (!profilePluginFolder.exists()) {
            profilePluginFolder.mkdirs()
        }
    }

    override fun initialize() {
        logger.info("Initializing ProfileService...")
        logger.info("Root Service name ${this.javaClass.`package`.name}")
        logger.info("Loading ProfilePlugins...")
        val files = profilePluginFolder.listFiles { file -> file.name.toLowerCase().endsWith(".jar")}
        logger.info("${files.size} Jar(s) found in plugins/profile folder")
        //Jar (Java Service) Loader
        val urls :Array<URL> =  Array<URL>(files.size, {
            index ->
            files[index].toURI().toURL()
        })
        val classLoader = URLClassLoader(urls, this.javaClass.classLoader)
        val pluginLoader: ServiceLoader<IProfileService> = ServiceLoader.load(IProfileService::class.java, classLoader)
        pluginLoader.iterator().forEach { plugin ->
            plugins.put(plugin.getServiceName(), plugin)
        }
        logger.info("Loading Internal ProfilePlugins...")
        //Self-contained packages (Reflection)
        val reflection = Reflections("${this.javaClass.`package`.name}.plugins")
        val clazz = reflection.getSubTypesOf(IProfileService::class.java)
        logger.info("${clazz.size} Internal ProfilePlugin(s) found")
        clazz.forEach { plugin ->
            val loadingPlugin = plugin.newInstance()
            logger.info("PluginService ${loadingPlugin.getServiceName()} Loaded.")
            plugins.put(loadingPlugin.getServiceName(), loadingPlugin)
        }

        logger.info("ProfilePlugin Loading complete, ${plugins.size} Plugins Loaded")
    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
        
    }

    fun getPlugin(name: String): IProfileService {
        if (plugins.containsKey(name)){
            return plugins[name]!!
        }
        throw IllegalArgumentException("Plugin Not found")
    }

    fun fetchUserProfile (user: User, profile: String, callback: (AsyncResult<JsonObject>) -> Unit) {
        dbClient.getConnection { con ->
            if (con.succeeded()) {
                con.result().queryWithParams("SELECT `${profile}` FROM `${dbprefix}_user_profile` WHERE `uid` = ?", JsonArray().add(user.id), {query ->
                    if (query.succeeded()) {
                        if (query.result().numRows > 0) {
                            callback.invoke(Future.succeededFuture(getPlugin(profile).parseData(query.result().results[0].getString(0))))
                        } else {
                            callback.invoke(Future.failedFuture("User DNE"))
                        }
                    } else {
                        callback.invoke(Future.failedFuture(query.cause()))
                    }
                    con.result().close()
                })
            } else {
                // [] are great!!!!
                callback.invoke(Future.failedFuture(con.cause()))
            }
        }
    }
}