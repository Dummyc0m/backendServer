package cn.com.guardiantech.classroom.server.data.avatar

import cn.codetector.util.Configuration.ConfigurationManager
import cn.com.guardiantech.classroom.server.Main
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import cn.com.guardiantech.classroom.server.data.user.User
import com.google.common.primitives.Longs
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.logging.LoggerFactory
import org.jetbrains.annotations.Mutable
import java.io.File
import java.util.*

/**
 * Created by Codetector on 2016/12/23.
 */
object AvatarManager : AbstractDataService() {
    private val logger = LoggerFactory.getLogger("AvatarManager")
    private val httpClient = Main.sharedVertx.createHttpClient(
            HttpClientOptions().setKeepAlive(false)
                    .setDefaultHost("www.gravatar.com")
                    .setDefaultPort(443)
                    .setSsl(true)
    );

    private val avatarConfiguration = ConfigurationManager.getConfiguration("avatar.config.json")
    private val avatarDirectory = File("./${avatarConfiguration.getStringValue("avatarDir", "avatar")}/")

    private val localFileList : MutableList<String> = ArrayList<String>()
    private val userAvatarMatch : MutableMap<Int, String> = HashMap<Int, String>()

    override fun initialize() {
        logger.info("Initializing Avatar Manager")
    }


    override fun saveToDatabase(action: () -> Unit) {
    }

    override fun loadFromDatabase(action: () -> Unit) {
        if (!avatarDirectory.exists()) {
            logger.info("Avatar directory created : ${avatarDirectory.name}")
            avatarDirectory.mkdir()
        }
        dbClient.getConnection { con ->
            if (con.succeeded()){
                con.result().query("SELECT * FROM `${DatabaseConfiguration.db_prefix}_avatar`", { q ->
                    if (q.succeeded()){
                        userAvatarMatch.clear()
                        q.result().rows.forEach { row ->
                            userAvatarMatch.put(row.getInteger("uesr"), row.getString("fileName"))
                        }
                    }
                    con.result().close()
                })
            }
        }
    }

    fun getAvatarForUser(user: User, handler: (AsyncResult<Buffer>) -> Any) {
        dbClient.getConnection { con ->
            if (con.succeeded()) {

            } else {
                handler.invoke(Future.failedFuture(con.cause()))
            }
        }
    }

}