package cn.com.guardiantech.classroom.server.data.security.authlog

import cn.codetector.util.Configuration.ConfigurationManager
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import cn.com.guardiantech.classroom.server.data.user.User
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

object AuthLogService : AbstractDataService() {
    val logger = LoggerFactory.getLogger("AuthLogService")
    val config = ConfigurationManager.getConfiguration("loggingService.config.json")
    var isAuthLoggingEnabled = config.getBooleanValue("authLogging", true)
    override fun initialize() {
        logger.info("Initializing AuthLog Service")
        logger.info("Auth Logging status: ${isAuthLoggingEnabled}")
    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
    }

    fun logUserActivity(user: User, ip: String, activityType: UserActivityLogType, ua: String = "") {
        if (isAuthLoggingEnabled) {
            dbClient.getConnection { conn ->
                if (conn.succeeded()) {
                    conn.result().updateWithParams("INSERT INTO `${DatabaseConfiguration.db_prefix}_authlog` (`type`, `ip`, `uid`, `ua`) VALUES (?, ?, ?, ?)", JsonArray().add(activityType.ordinal).add(ip).add(user.id).add(ua), { query ->
                        if (query.failed()) {
                            logger.warn("Failed to log user ${activityType.name} activity ", query.cause())
                        }
                        conn.result().close()
                    })
                }
            }
        }
    }

    fun fetchUserActivity(user: User, activityType: UserActivityLogType, handler: (AsyncResult<JsonObject>) -> Any) {
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().queryWithParams("SELECT `ip`, `type`, UNIX_TIMESTAMP(`date`) as `time` FROM `${dbprefix}_authlog` WHERE `uid` = ? AND `type` = ? ORDER BY `date` DESC LIMIT 10",
                        JsonArray(arrayListOf(user.id, activityType.ordinal)),
                        {
                            query ->
                            if (query.succeeded()) {
                                handler.invoke(Future.succeededFuture(JsonObject().put("count",query.result().numRows).put("log",query.result().rows)))
                            } else {
                                handler.invoke(Future.failedFuture(query.cause()))
                            }
                            conn.result().close()
                        })
            } else {
                handler.invoke(Future.failedFuture(conn.cause()))
            }
        }
    }
}