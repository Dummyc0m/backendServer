package cn.com.guardiantech.classroom.server.data.user

import cn.codetector.util.Configuration.ConfigurationManager
import cn.codetector.util.Validator.SHA
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import cn.com.guardiantech.classroom.server.data.permission.Permission
import cn.com.guardiantech.classroom.server.data.permission.PermissionManager
import cn.com.guardiantech.classroom.server.data.profile.ProfileService
import com.sun.org.apache.xpath.internal.operations.Bool
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.util.*

object UserManager : AbstractDataService() {
    private val userConfiguration = ConfigurationManager.getConfiguration("user.config.json")
    private val defaultUserStatus = userConfiguration.getIntegerValue("defaultUserStatus",0)
    val allUsers: HashSet<User> = HashSet()
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun initialize() {
        logger.info("User Manager Initialized")
    }

    override fun saveToDatabase(action: () -> Unit) {
        dbClient.getConnection { conn ->
            logger.info("Saving users to configuration...")
            val users: MutableList<JsonArray> = ArrayList()
            this.allUsers.forEach { user ->
                users.add(JsonArray().add(user.id).add(user.email).add(user.passwordHash).add(user.accountStatus).add(user.mfa).add(user.role.name))
            }
            if (conn.succeeded()) {
                conn.result().batchWithParams("INSERT INTO `${DatabaseConfiguration.db_prefix}_auth` (`id`, `email`,`password`,`accountStatus`,`2fa`,`role`) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `password` = VALUES(`password`),`email` = VALUES(`email`),`accountStatus` = VALUES(`accountStatus`),`2fa` = VALUES(`2fa`), `role` = VALUES(`role`)", users, {
                    result ->
                    if (result.succeeded()) {
                        var success = 0
                        var fail = 0
                        result.result().forEach {
                            if (it > 0) success++ else fail++
                        }
                        logger.info("User save complete, Succeed:$success, Failed:$fail")
                    }
                    action.invoke()
                })
            } else {
                logger.warn("Failed to initialize configuration connection", conn.cause())
                action.invoke()
            }
        }
    }

    override fun loadFromDatabase(action: () -> Unit) {
        dbClient.getConnection { conn ->
            logger.info("Loading Users from Database...")
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `${DatabaseConfiguration.db_prefix}_auth`", { result ->
                    allUsers.clear()
                    result.result().rows.forEach { row ->
                        val user = User(row.getInteger("id"), row.getString("email"), row.getString("password"), row.getInteger("accountStatus"), row.getString("2fa"), PermissionManager.getRoleByName(row.getString("role")))
                        allUsers.add(user)
                    }
                    val userCount = allUsers.size
                    logger.info("User load complete, $userCount user(s) loaded")
                    action.invoke()
                })
            } else {
                logger.warn("Failed to initialize configuration connection", conn.cause())
                action.invoke()
            }
        }
    }

    fun hasUser(email: String): Boolean {
        return (allUsers.find { user ->
            user.email.equals(email, ignoreCase = true)
        }) != null
    }

    fun getUserByEmail(email: String): User {
        try {
            return (allUsers.find { user ->
                user.email.equals(email, ignoreCase = true)
            })!!
        } catch (e: Throwable) {
            throw IllegalArgumentException("requested User does not exist. Please check use hasUser(username) before requesting")
        }
    }

    fun registerUser(email: String, password: String, name: String, handler: (result: AsyncResult<Boolean>) -> Any) {
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().queryWithParams("SELECT count(*) as `count` FROM `${DatabaseConfiguration.db_prefix}_auth` WHERE `email` = ? ", JsonArray().add(email), { countCheck ->
                    if (countCheck.succeeded()) {
                        if (countCheck.result().results[0].getInteger(0) < 1) {
                            conn.result().queryWithParams("SELECT AUTO_INCREMENT as `value` FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", JsonArray(arrayListOf(DatabaseConfiguration.db_name,"${DatabaseConfiguration.db_prefix}_auth")), {
                                ai ->
                                if (ai.succeeded()) {
                                    val nextAiValue = ai.result().rows[0].getInteger("value")
                                    conn.result().updateWithParams("INSERT INTO `${DatabaseConfiguration.db_prefix}_auth` (`email`,`password`,`2fa`, accountStatus) VALUES (?,?,'',?)", JsonArray(arrayListOf(email, SHA.getSHA256String(password), defaultUserStatus)), { insert ->
                                        if (insert.succeeded()) {
                                            val thisUser = User(nextAiValue, email, SHA.getSHA256String(password), defaultUserStatus, "", PermissionManager.getRoleByName("user"))
                                            this.allUsers.add(thisUser)
                                            markChange()
                                            ProfileService.registerNewUser(thisUser, JsonObject().put("name", JsonObject().put("name", name)))
                                            handler.invoke(Future.succeededFuture(true))
                                            conn.result().close()
                                        } else {
                                            handler.invoke(Future.failedFuture(insert.cause()))
                                            conn.result().close()
                                        }
                                    })
                                } else {
                                    handler.invoke(Future.failedFuture(ai.cause()))
                                    conn.result().close()
                                }
                            })
                        } else {
                            conn.result().close()
                            handler.invoke(Future.succeededFuture(false))
                        }
                    } else {
                        conn.result().close ()
                        handler.invoke(Future.failedFuture(countCheck.cause()))
                    }
                })
            } else {
                handler.invoke(Future.failedFuture(conn.cause()))
            }
        }
    }

    fun  getUserById(id: Int): User {
        try {
            return (allUsers.find { user ->
                user.id == id
            })!!
        } catch (e: Throwable) {
            throw IllegalArgumentException("requested User does not exist. Please check use hasUser(username) before requesting")
        }
    }

}