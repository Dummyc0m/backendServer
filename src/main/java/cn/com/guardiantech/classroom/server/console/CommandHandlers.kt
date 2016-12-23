/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.console

import cn.com.guardiantech.classroom.server.Main
import cn.com.guardiantech.classroom.server.console.consoleManager.Command
import cn.com.guardiantech.classroom.server.data.DataService
import cn.com.guardiantech.classroom.server.data.permission.PermissionManager
import cn.com.guardiantech.classroom.server.data.user.UserHash
import cn.com.guardiantech.classroom.server.webService.WebService

/**
 * Created by codetector on 20/11/2016.
 */
object CommandHandlers {
    @Command(command = "web")
    fun webServiceCommandHandler(args: Array<String>): Boolean {
        if (args.size == 2) {
            when (args[1]) {
                "status" -> {
                    println("Web Service is currently " + if (WebService.isServiceRunning) "Running" else "Stopped")
                    return true
                }
                "stop" -> {
                    WebService.shutdown()
                    return true
                }
                "start" -> {
                    WebService.initService(Main.sharedVertx, Main.sharedJDBCClient)
                    return true
                }
            }
        }
        println("Available actions: (status)")
        return false
    }

    @Command(command = "server")
    fun serverCommandHandler(args: Array<String>): Boolean {
        if (args.size == 2) {
            when (args[1]) {
                "stop" -> {
                    Main.stopService()
                    return true
                }
                "save" -> {
                    Main.save()
                    return true
                }
            }
        }
        println("Available actions: (status)")
        return false
    }

    @Command(command = "db")
    fun dbCommandHandler(args: Array<String>): Boolean {
        if (args.size > 1) {
            when (args[1]) {
                "save" -> {
                    DataService.save()
                    return true
                }
                "reload" -> {
                    DataService.reload()
                    return true
                }
            }
        }
        return false
    }

    @Command(command = "user")
    fun userCommandHandler(args: Array<String>): Boolean {
        if (args.size > 1) {
            when (args[1]) {
                "count" -> {
                    val cnt = UserHash.totalUserCache()
                    println("Current Logged in: $cnt")
                    return true
                }
                "save" -> {
                    UserHash.save()
                    return true
                }
                "reload" -> {
                    UserHash.removeTimedOutUsers(UserHash.DEFAULT_TIMEOUT)
                    println("Finished removing timed out users")
                    return true
                }
                "clear" -> {
                    UserHash.clearCache()
                    return true
                }
            }
        }
        return false
    }

    @Command(command = "permission")
    fun permissionCommandHandler(args: Array<String>): Boolean {
        if (args.size > 1) {
            when (args[1]) {
                "list" -> {
                    PermissionManager.allPermissions().forEach(::println)
                    return true
                }
                "add" -> {
                    if (args.size > 2) {
                        PermissionManager.registerPermission(args[2])
                        println("Permission ${args[2]} added!")
                        return true
                    }
                }
            }
        }
        return false
    }
}