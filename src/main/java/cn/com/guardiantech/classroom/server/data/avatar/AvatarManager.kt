package cn.com.guardiantech.classroom.server.data.avatar

import cn.codetector.util.Configuration.ConfigurationManager
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import com.google.common.primitives.Longs
import io.vertx.core.logging.LoggerFactory
import java.io.File

/**
 * Created by Codetector on 2016/12/23.
 */
object AvatarManager : AbstractDataService(){
    private val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val BASE = ALPHABET.length

    private val logger = LoggerFactory.getLogger("AvatarManager")


    private val avatarConfiguration = ConfigurationManager.getConfiguration("avatar.config.json")
    private val avatarDirectory = File("./${avatarConfiguration.getStringValue("avatarDir","avatar")}/")

    override fun initialize() {
        logger.info("Initializing Avatar Manager")
    }


    override fun saveToDatabase(action: () -> Unit) {
    }

    override fun loadFromDatabase(action: () -> Unit) {
        if (!avatarDirectory.exists()){
            logger.info("Avatar directory created : ${avatarDirectory.name}")
            avatarDirectory.mkdir()
        }
    }

    private fun getRandomUniqueFileName() : String {
        var i = System.nanoTime()
        val sb = StringBuilder("")
        if (i === 0L) {
            return "a"
        }
        while (i > 0) {
            i = fromBase10(i, sb)
        }
        return sb.reverse().toString()
    }

    private fun fromBase10(i: Long, sb: StringBuilder): Long {
        val rem = i % BASE
        sb.append(ALPHABET[rem.toInt()])
        return i / BASE
    }
}