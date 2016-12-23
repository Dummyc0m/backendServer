/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.todolist.server.data.user

import cn.codetector.todolist.server.data.permission.Permission
import cn.codetector.todolist.server.data.permission.PermissionManager
import cn.codetector.todolist.server.data.permission.Role
import cn.codetector.util.Validator.SHA

class User(val username: String, var passwordHash: String, var role: Role) {
    fun hasPermission(permission: Permission): Boolean {
        return role.hasPermission(permission)
    }

    fun hasPermission(permission: String): Boolean {
        if (role.name.equals("administrator", ignoreCase = true)) return true
        return role.hasPermission(PermissionManager.getPermissionByName(permission))
    }

    fun authenticate(password: String): Boolean {
        return SHA.getSHA256String(password.toLowerCase()) == (this.passwordHash)
    }

    fun updatePassword(oldPass: String, newPass: String): Boolean{
        if (authenticate(oldPass) && newPass.length == 32){
            passwordHash = SHA.getSHA256String(newPass.toLowerCase())
            UserManager.markChange()
            return true
        }
        return false
    }
}