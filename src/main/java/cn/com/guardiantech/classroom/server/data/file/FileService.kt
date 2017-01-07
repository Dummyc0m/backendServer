package cn.com.guardiantech.classroom.server.data.file

import cn.com.guardiantech.classroom.server.data.AbstractDataService
import io.vertx.ext.web.RoutingContext
import java.io.File

/**
 * Created by Codetector on 2017/1/7.
 */
object FileService : AbstractDataService() {
    val fileStore = File("./store/files")
    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {

    }

    override fun initialize() {
        if (!fileStore.exists()) {
            fileStore.mkdirs()
        }
    }

}