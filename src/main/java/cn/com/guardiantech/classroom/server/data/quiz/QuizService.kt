package cn.com.guardiantech.classroom.server.data.quiz

import cn.com.guardiantech.classroom.server.data.AbstractDataService
import io.vertx.core.logging.LoggerFactory

object QuizService: AbstractDataService() {
    val logger = LoggerFactory.getLogger("QuizService")
    override fun initialize() {
        logger.info("QuizService Initialized")
    }

    override fun saveToDatabase(action: () -> Unit) {
    }

    override fun loadFromDatabase(action: () -> Unit) {
    }
}