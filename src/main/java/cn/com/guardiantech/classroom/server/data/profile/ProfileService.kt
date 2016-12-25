package cn.com.guardiantech.classroom.server.data.profile

import cn.com.guardiantech.classroom.server.data.AbstractDataService
import io.vertx.core.logging.LoggerFactory

object ProfileService : AbstractDataService(){
    val logger = LoggerFactory.getLogger("ProfileService")

    override fun initialize() {
        logger.info("Initializing ProfileService...")
    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
        
    }
}