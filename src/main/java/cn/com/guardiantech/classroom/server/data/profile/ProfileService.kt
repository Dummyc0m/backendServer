package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.api.IProfileService
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.user.User
import cn.com.guardiantech.classroom.server.data.user.UserManager
import com.google.common.base.Strings
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

object ProfileService : AbstractDataService() {
    val logger = LoggerFactory.getLogger("ProfileService")
    val profilePluginFolder = File("./plugins/profile")
    val plugins: MutableMap<String, IProfileService> = HashMap<String, IProfileService>()

    val dbCache: MutableMap<User, UserProfile> = HashMap<User, UserProfile>()

    init {
        if (!profilePluginFolder.exists()) {
            profilePluginFolder.mkdirs()
        }
        logger.info("NOT LOADING!!!")//TODO
    }

    override fun initialize() {
        logger.info("Initializing ProfileService...")
        logger.info("Root Service name ${this.javaClass.`package`.name}")
        logger.info("Loading ProfilePlugins...")
        val files = profilePluginFolder.listFiles { file -> file.name.toLowerCase().endsWith(".jar") }
        logger.info("${files.size} Jar(s) found in plugins/profile folder")
        //Jar (Java Service) Loader
        val urls: Array<URL> = Array<URL>(files.size, {
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

    private fun loadProfile(data: JsonObject) {
        val userProfile = UserProfile()
        plugins.values.forEach { p ->
            var pluginData = Strings.nullToEmpty(data.getString(p.getServiceName()))
            val validData  = p.isDataValid(pluginData)
            if (!validData){
                pluginData = p.upgradeData(pluginData)
            }
            userProfile.set(p, pluginData, !validData)
        }
        dbCache.put(UserManager.getUserById(data.getInteger("uid")), userProfile)
    }

    private fun loadData (action: () -> Unit) {
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `${dbprefix}_user_profile`", {result ->
                    if (result.succeeded()){
                        result.result().rows.forEach { row ->
                            loadProfile(row)
                        }
                        action.invoke()
                    }
                    conn.result().close()
                })
            }
        }
    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
        logger.info("Initializing plugins and Loading data...")
        logger.info("Verifying db and adding columns if needed")
        //ALTER TABLE `classroom_user_profile` ADD `test` TEXT NULL AFTER `phone`;
        val allColumns: MutableList<String> = ArrayList<String>()
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().query("SHOW COLUMNS FROM `${dbprefix}_user_profile`", { query ->
                    query.result().rows.forEach { row ->
                        allColumns.add(row.getString("Field"))
                    }
                    conn.result().close()
                    //Checking columns against plugins
                    val newColumns: MutableList<String> = ArrayList<String>()
                    plugins.keys.forEach { plugin ->
                        if (!allColumns.contains(plugin)) {
                            newColumns.add(plugin)
                        }
                    }

                    if (newColumns.isNotEmpty()) {
                        dbClient.getConnection { conn ->
                            if (conn.succeeded()) {
                                val connection = conn.result()
                                connection.setAutoCommit(false, {
                                    newColumns.forEach { column ->
                                        connection.update("ALTER TABLE `${dbprefix}_user_profile` ADD `${column}` TEXT CHARACTER SET utf8 COLLATE utf8_bin NULL", {

                                        })
                                    }
                                    connection.commit {
                                        connection.setAutoCommit(true, {
                                            connection.close()
                                            loadData(action)
                                        })
                                    }
                                })
                            }
                        }
                    } else {
                        loadData(action)
                    }
                })
            }
        }
    }

    fun hasPlugin(name: String): Boolean {
        return plugins.containsKey(name)
    }

    fun getPlugin(name: String): IProfileService {
        if (plugins.containsKey(name)) {
            return plugins[name]!!
        }
        throw IllegalArgumentException("Plugin Not found")
    }

    fun fetchUserProfile(user: User, profile: String, callback: (AsyncResult<JsonObject>) -> Unit) {
        if (hasPlugin(profile)) {
            val plugin = getPlugin(profile)
            dbClient.getConnection { con ->
                if (con.succeeded()) {
                    con.result().queryWithParams("SELECT `${profile}` FROM `${dbprefix}_user_profile` WHERE `uid` = ?", JsonArray().add(user.id), { query ->
                        if (query.succeeded()) {
                            if (query.result().numRows > 0) {
                                val dataOfUser = query.result().results[0].getString(0)
                                if (!plugin.isDataValid(dataOfUser)) {

                                }
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
}