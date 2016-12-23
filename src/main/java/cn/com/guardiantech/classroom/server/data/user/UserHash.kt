/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.data.user

import cn.com.guardiantech.classroom.server.Main
import com.google.common.io.Files
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.*

object UserHash {
    val DEFAULT_TIMEOUT: Long = Main.globalConfig.getLongValue("userTimeout", 1000 * 60 * 30)
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val allUsers: MutableMap<String, WebUser> = HashMap()

    private val targetCacheFile = File("./session.cache")
    private var changed = false

    fun save() {
        logger.trace("Saving login Cache...")
        removeTimedOutUsers(DEFAULT_TIMEOUT)
        val writer = PrintWriter(FileWriter(targetCacheFile))
        val dataArray = JsonArray()
        allUsers.forEach { entry ->
            dataArray.add(JsonObject().put("key", entry.key).put("user", entry.value.user.username).put("lastActive", entry.value.lastActive))
        }
        writer.println(JsonObject().put("cache", dataArray).toString())
        writer.close()
        logger.trace("Login Cache saved!")
        changed = false
    }

    fun loadCache() {
        logger.trace("Initializing UserHash configuration")
        if (targetCacheFile.exists() && targetCacheFile.isFile) {
            val dataString = Files.toString(targetCacheFile, Charset.forName("utf-8"))
            if (dataString.isNotBlank()) {
                val cachedData = JsonObject(dataString).getJsonArray("cache")
                cachedData.forEach { item ->
                    if ((item as JsonObject).getString("user").isNotBlank() && UserManager.hasUser(item.getString("user"))) {
                        allUsers.put(item .getString("key"), WebUser(UserManager.getUserByUsername(item.getString("user")), item.getLong("lastActive")))
                    }
                }
                logger.trace("All (${allUsers.size}) user cache loaded")
                removeTimedOutUsers(DEFAULT_TIMEOUT)
            }
        }
    }

    fun totalUserCache(): Int {
        return allUsers.size
    }

    fun createWebUser(user: User): String {
        var uniqueId = UUID.randomUUID().toString()
        while (allUsers.containsKey(uniqueId)) {
            uniqueId = UUID.randomUUID().toString()
        }
        allUsers.put(uniqueId, WebUser(user))
        changed = true
        return uniqueId
    }

    fun isAuthKeyValid(key: String): Boolean {
        return allUsers.containsKey(key)
    }

    fun getUserByAuthKey(key: String): WebUser {
        return allUsers.get(key)!!
    }

    fun removeTimedOutUsers(valveValue: Long) {
        var count = 0
        val it = allUsers.iterator()
        while (it.hasNext()) {
            if (!it.next().value.lastActiveInRange(valveValue)) {
                it.remove()
                count++
            }
        }
        if (count > 0) {
            logger.trace("$count timed out user(s) removed")
            changed = true
        }
    }

    fun tick() {
        removeTimedOutUsers(DEFAULT_TIMEOUT)
        if (changed) {
            save()
        }
    }

    fun clearCache() {
        allUsers.clear()
        save()
    }
}