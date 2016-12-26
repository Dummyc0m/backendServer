/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.data

import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration
import io.vertx.ext.jdbc.JDBCClient

/**
 * Created by Codetector on 21/11/2016.
 */
abstract class AbstractDataService {
    protected lateinit var dbClient: JDBCClient
    private var hasChange = false
    protected val dbprefix = DatabaseConfiguration.db_prefix

    fun isInitialized(): Boolean {
        return this.dbClient != null
    }

    fun setDBClient(dbClient: JDBCClient) {
        this.dbClient = dbClient
        initialize()
    }

    abstract fun initialize()

    abstract fun saveToDatabase(action: () -> Unit)
    fun saveToDatabase() {
        saveToDatabase {}
    }

    abstract fun loadFromDatabase(action: () -> Unit)
    fun loadFromDatabase() {
        loadFromDatabase {}
    }

    fun markChange() {
        this.hasChange = true
    }

    protected fun hasChanged(): Boolean {
        if (hasChange) {
            hasChange = false
            return true
        }
        return false
    }

    open fun tick() {
        if (hasChanged()){
            saveToDatabase()
        }
    }
}