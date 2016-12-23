/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server

import cn.com.guardiantech.classroom.server.console.consoleManager.ConsoleManager
import cn.com.guardiantech.classroom.server.data.DataService
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import cn.com.guardiantech.classroom.server.data.user.UserHash
import cn.com.guardiantech.classroom.server.webService.WebService
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.ext.jdbc.JDBCClient
import org.apache.logging.log4j.LogManager

object Main {
    val rootLogger = LogManager.getLogger("Server Root")
    val globalConfig = ConfigurationManager.getConfiguration("mainConfig.json")
    val sharedVertx: Vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(globalConfig.getIntegerValue("workerPoolSize", 32)))
    val sharedJDBCClient: JDBCClient = JDBCClient.createShared(sharedVertx, DatabaseConfiguration.getVertXJDBCConfigObject())

    init {
        rootLogger.info("Starting Server...")
    }

    fun initService() {
        try {
            DataService.start()
            WebService.initService(sharedVertx, sharedJDBCClient) //Init Web API Services
        } catch (t: Throwable) {
            rootLogger.error(t)
        }
        ConsoleManager.monitorStream("ConsoleIn", System.`in`)
    }

    fun save() {
        UserHash.save()
        DataService.save()
    }

    fun stopService() {
        rootLogger.info("Shutting down Server")
        ConsoleManager.stop()
        WebService.shutdown()
        UserHash.save()
        DataService.terminate()
        DataService.save {
            //TODO move configuration shutdown into Dataservice
            rootLogger.info("Disconnecting from Database")
            sharedJDBCClient.close()
            rootLogger.info("All Database connection shutdown")
            sharedVertx.close({ res ->
                if (res.succeeded()) {
                    rootLogger.info("Vert.X Shutdown")
                }
            })
        }

    }
}

fun main(args: Array<String>) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    System.setProperty("logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    Main.initService()
}