/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.com.guardiantech.classroom.server.data.user

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider

/**
 * Created by codetector on 21/11/2016.
 */
class WebUser(val user: User) : io.vertx.ext.auth.User {
    var lastActive = System.currentTimeMillis()
        private set
    var mfaAuthed = false

    constructor(user: User, lastActive: Long) : this(user) {
        this.lastActive = lastActive
    }

    constructor(user: User, mfa:Boolean) : this(user) {
        this.mfaAuthed = mfa
    }

    constructor(user: User, lastActive: Long, mfa: Boolean) : this(user, lastActive) {
        this.mfaAuthed = mfa
    }

    init {
        if (!this.user.hasMFA()){
            this.mfaAuthed = true
        }
    }
    override fun isAuthorised(authority: String?, resultHandler: Handler<AsyncResult<Boolean>>?): io.vertx.ext.auth.User {
        resultHandler!!.handle(Future.succeededFuture(mfaAuthed && user.hasPermission(authority!!)))
        return this
    }

    fun renewSession() {
        this.lastActive = System.currentTimeMillis()
    }

    override fun clearCache(): io.vertx.ext.auth.User {
        return this
    }

    override fun setAuthProvider(authProvider: AuthProvider?) {

    }

    override fun principal(): JsonObject {
        return JsonObject().put("email", this.user.email).put("id", this.user.id)
    }

    fun lastActiveInRange(timeRange: Long): Boolean {
        return (Math.abs(System.currentTimeMillis() - lastActive) < timeRange)
    }
}