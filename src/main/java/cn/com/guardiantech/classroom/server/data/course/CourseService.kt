package cn.com.guardiantech.classroom.server.data.course

import cn.com.guardiantech.classroom.server.data.AbstractDataService
import io.vertx.core.logging.LoggerFactory
import java.util.*

/**
 * Created by Codetector on 2016/12/25.
 */
object CourseService : AbstractDataService() {
    val logger = LoggerFactory.getLogger("CourseService")
    val courseList: MutableList<Course> = ArrayList<Course>()

    override fun initialize() {
        logger.info("Initializing ProfileService...")
    }

    override fun saveToDatabase(action: () -> Unit) {

    }

    override fun loadFromDatabase(action: () -> Unit) {
        dbClient.getConnection { con ->
            if (con.succeeded()) {
                con.result().query("SELECT * FROM ${dbprefix}_course", { result ->
                    if (result.succeeded()){

                    }else{
                        logger.error(result.cause())
                        action.invoke()
                    }
                })
            }else{
                logger.error(con.cause())
                action.invoke()
            }
        }
    }

}