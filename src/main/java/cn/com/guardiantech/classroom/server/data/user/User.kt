/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.data.user

import cn.com.guardiantech.classroom.server.data.permission.Permission
import cn.com.guardiantech.classroom.server.data.permission.PermissionManager
import cn.com.guardiantech.classroom.server.data.permission.Role
import cn.codetector.util.Validator.SHA
import cn.com.guardiantech.classroom.server.data.user.UserManager.markChange

class User(val id:Int, var email: String, var passwordHash: String, var accountStatus: Int, var twoFa: String, var role: Role) {
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

    fun updatePassword(oldPass: String, newPass: String): Boolean {
        if (authenticate(oldPass) && newPass.length == 32) {
            passwordHash = SHA.getSHA256String(newPass.toLowerCase())
            markChange()
            return true
        }
        return false
    }

    fun updateEmail(newEmail: String) {
        this.email = newEmail
        markChange()
    }

    fun update2fa(twoFa: String){
        this.twoFa = twoFa
        markChange()
    }
}