/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.data.permission

import cn.com.guardiantech.classroom.server.data.AbstractDataService
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.util.*

/**
 * Created by codetector on 19/11/2016.
 */
object PermissionManager : AbstractDataService() {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val serverPermissions = PermissionMap()
    private val serverRoles = HashMap<String, Role>()

    override fun initialize() {
        logger.info("Permission Manager Initialized")
    }

    fun allPermissions(): List<Permission> {
        return ArrayList<Permission>(serverPermissions.permissions.values)
    }

    fun getPermissionByName(name: String): Permission {
        if (serverPermissions.permissions.contains(name)) {
            return serverPermissions.permissions.get(name)!!
        } else {
            throw IllegalArgumentException("No Permission named '$name' Found")
        }
    }

    fun registerPermission(name: String) {
        serverPermissions.addPermission(Permission(name))
        markChange()
        savePermissionTable { }
    }

    private fun getPermissionWithName(name: String): Permission {
        if (serverPermissions.permissions.contains(name)) {
            return serverPermissions.permissions.get(name)!!
        } else {
            val perm = Permission(name)
            logger.trace("Permission created on use : $perm")
            serverPermissions.addPermission(perm)
            markChange()
            return perm
        }
    }

    fun getRoleByName(name: String): Role {
        if (serverRoles.contains(name)) {
            return serverRoles.get(name)!!
        } else {
            throw IllegalArgumentException("No Role named '$name' Found")
        }
    }

    fun createRoleWithName(name: String): Boolean {
        if (!serverRoles.containsKey(name)) {
            markChange()
            return (serverRoles.put(name, Role(name)) != null)
        } else {
            return false
        }
    }

    fun savePermissionTable(action: () -> Unit) {
        assert(isInitialized())
        logger.trace("Saving Permission Table...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                val params = ArrayList<JsonArray>()
                serverPermissions.permissions.values.forEach { perm ->
                    params.add(JsonArray().add(perm.name).add(perm.description))
                }
                conn.result().batchWithParams("REPLACE INTO `${DatabaseConfiguration.db_prefix}_permission` (`name`, `description`) VALUES (? ,? )", params, {
                    handler ->
                    if (handler.succeeded()) {
                        var success = 0
                        var fail = 0
                        handler.result().forEachIndexed { i, result ->
                            if (result == 1) success++ else fail++
                        }
                        logger.trace("Permissions save complete, Success: $success, Fail: $fail")
                    }
                    action.invoke()
                })
            } else {
                logger.error("Failed to save permission table", conn.cause())
                action.invoke()
            }
        }
    }

    fun saveRolesTable(action: () -> Unit) {
        assert(isInitialized())
        logger.trace("Saving Roles Table...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                val roles = ArrayList<JsonArray>()
                serverRoles.values.forEach { role ->
                    roles.add(JsonArray().add(role.name).add(role.getPermissionJson().toString()))
                }
                conn.result().batchWithParams("INSERT INTO `${DatabaseConfiguration.db_prefix}_roles` (`name`,`permissions`) VALUES (?,?) ON DUPLICATE KEY UPDATE `permissions` = VALUES(`permissions`)", roles, {
                    handler ->
                    if (handler.succeeded()) {
                        var success = 0
                        var fail = 0
                        handler.result().forEachIndexed { i, result ->
                            if (result == 1) success++ else fail++
                        }
                        logger.trace("Roles save complete, Success: $success, Fail: $fail")
                    } else {
                        logger.error("Failed to save Role table", handler.cause())
                    }
                    action.invoke()
                })
            } else {
                logger.error("Failed to save Role table", conn.cause())
                action.invoke()
            }
        }
    }

    override fun saveToDatabase(action: () -> Unit) {
        savePermissionTable {
            saveRolesTable {
                action.invoke()
            }
        }
    }

    override fun loadFromDatabase(action: () -> Unit) {
        this.loadPermissionsFromDatabase {
            this.loadRolesFromDatabase {
                action.invoke()
            }
        }
    }

    fun loadPermissionsFromDatabase(action: () -> Unit) {
        assert(isInitialized())
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                logger.trace("Loading permissions from configuration...")
                conn.result().query("SELECT * FROM `${DatabaseConfiguration.db_prefix}_permission`", {
                    result ->
                    if (result.succeeded()) {
                        serverPermissions.clear()
                        result.result().rows.forEach {
                            row ->
                            val perm = Permission(row.getString("name"), row.getString("description"))
                            serverPermissions.addPermission(perm)
                        }
                        val permCount = serverPermissions.allPermissions().size
                        logger.trace("All ($permCount) Permission Loaded")
                    }
                    conn.result().close()
                    action.invoke()
                })
            }
        }
    }

    fun loadRolesFromDatabase(action: () -> Unit) {
        logger.trace("Loading serverRoles from configuration...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `${DatabaseConfiguration.db_prefix}_roles`", {
                    query ->
                    if (query.succeeded()) {
                        serverRoles.clear()
                        query.result().rows.forEach { roleR ->
                            var role = Role(roleR.getString("name"))
                            JsonObject(roleR.getString("permissions")).getJsonArray("permissions").forEach { permission ->
                                if (permission is String) {
                                    role.addPermission(getPermissionWithName(permission))
                                }
                            }
                            serverRoles.put(role.name, role)
                        }
                    }
                    val rolesCount = serverRoles.size
                    logger.trace("All ($rolesCount) Roles Loaded")
                    action.invoke()
                })
            } else {

            }
        }
    }

    override fun tick() {
        if (hasChanged()) {
            saveToDatabase()
        }
    }
}