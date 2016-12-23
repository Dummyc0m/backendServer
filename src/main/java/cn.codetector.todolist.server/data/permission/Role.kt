package cn.codetector.todolist.server.data.permission

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class Role(val name: String) {
    val permissions: PermissionMap = PermissionMap()

    fun addPermission(permission: Permission) {
        permissions.addPermission(permission)
    }

    fun hasPermission(permission: Permission): Boolean {
        return permissions.hasPermission(permission)
    }

    @Deprecated(message = "Please get permission Object from PermissionManager", replaceWith = ReplaceWith("hasPermission(PermissionManager.getPermissionByName(permission))"), level = DeprecationLevel.HIDDEN)
    fun hasPermission(permission: String): Boolean {
        return hasPermission(PermissionManager.getPermissionByName(permission))
    }

    fun allPermissions(): List<Permission> {
        return permissions.allPermissions()
    }

    fun getPermissionJson(): JsonObject {
        val perms = JsonArray()
        permissions.permissions.values.forEach {
            p ->
            perms.add(p.name)
        }
        return JsonObject().put("permissions", perms)
    }
}