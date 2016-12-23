package cn.codetector.todolist.server.data.user

import cn.codetector.todolist.server.data.AbstractDataService
import cn.codetector.todolist.server.data.configuration.DatabaseConfiguration
import cn.codetector.todolist.server.data.permission.PermissionManager
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory
import java.util.*

object UserManager : AbstractDataService() {
    val allUsers: MutableMap<String, User> = HashMap()
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun initialize() {
        logger.info("User Manager Initialized")
    }

    override fun saveToDatabase(action: () -> Unit) {
        dbClient!!.getConnection { conn ->
            logger.trace("Saving users to configuration...")
            val users: MutableList<JsonArray> = ArrayList()
            this.allUsers.values.forEach { user ->
                users.add(JsonArray().add(user.username).add(user.passwordHash).add(user.role.name))
            }
            if (conn.succeeded()) {
                conn.result().batchWithParams("INSERT INTO `${DatabaseConfiguration.db_prefix}_auth` (`username`,`password`,`role`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `role` = VALUES(`role`)", users, {
                    result ->
                    if (result.succeeded()) {
                        var success = 0
                        var fail = 0
                        result.result().forEach {
                            if (it > 0) success++ else fail++
                        }
                        logger.trace("User save complete, Succeed:$success, Failed:$fail")
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
        assert(isInitialized())
        dbClient!!.getConnection { conn ->
            logger.trace("Loading Users from Database...")
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `${DatabaseConfiguration.db_prefix}_auth`", { result ->
                    allUsers.clear()
                    result.result().rows.forEach { row ->
                        val user = User(row.getString("username"), row.getString("password"), PermissionManager.getRoleByName(row.getString("role")))
                        allUsers.put(user.username, user)
                    }
                    val userCount = allUsers.size
                    logger.trace("User load complete, $userCount user(s) loaded")
                    action.invoke()
                })
            } else {
                logger.warn("Failed to initialize configuration connection", conn.cause())
                action.invoke()
            }
        }
    }

    fun hasUser(username: String): Boolean {
        return allUsers.containsKey(username)
    }

    fun getUserByUsername(username: String): User {
        try {
            return allUsers.get(username)!!
        } catch (e: Throwable) {
            throw IllegalArgumentException("requested User does not exist. Please check use hasUser(username) before requesting")
        }
    }

}