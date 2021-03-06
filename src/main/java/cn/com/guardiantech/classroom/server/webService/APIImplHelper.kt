package cn.com.guardiantech.classroom.server.webService

import cn.com.guardiantech.classroom.server.data.security.authlog.AuthLogService
import cn.com.guardiantech.classroom.server.data.security.authlog.UserActivityLogType
import cn.com.guardiantech.classroom.server.data.security.ipLoaction.IPLocationService
import cn.com.guardiantech.classroom.server.data.user.UserHash
import cn.com.guardiantech.classroom.server.data.user.UserManager
import cn.com.guardiantech.classroom.server.data.user.WebUser
import com.google.common.base.Strings
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router

fun ajaxXSiteHandler(router: Router) {
    router.route().handler { ctx ->
        ctx.response().putHeader("Access-Control-Allow-Origin", "*")
        ctx.next()
    }

    router.post().handler { ctx ->
        ctx.response().putHeader("Content-Type", "text/json; charset=utf-8")
        ctx.next()
    }

    router.get().handler { ctx ->
        ctx.response().putHeader("Content-Type", "text/json; charset=utf-8")
        ctx.next()
    }

    router.route().failureHandler { ctx ->
        ctx.response().setStatusCode(if (ctx.statusCode() > 0) {
            ctx.statusCode()
        } else {
            500
        }).putHeader("Access-Control-Allow-Origin", "*").end()
        if (ctx.failure() != null && ctx.failure() is Throwable) {
            ctx.failure().printStackTrace()
        }
    }

}

fun preflightRouteHandler(router: Router) {
    router.options().handler { ctx ->
        ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        ctx.response().putHeader("Access-Control-Allow-Headers", "Authorization")
        ctx.response().end()
    }
}

fun authHandler(router: Router, noAuthExceptions: Set<String>) {
    router.route().handler { ctx ->
        var path = ctx.request().path()
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        if (noAuthExceptions.contains(path)) {
            ctx.next()
        } else {
            val auth = ctx.request().getHeader("Authorization")
            if (auth != null && UserHash.isAuthKeyValid(auth)) {
                ctx.setUser(UserHash.getUserByAuthKey(auth))
                if ((ctx.user() as WebUser).mfaAuthed) {
                    (ctx.user() as WebUser).renewSession()
                    ctx.next()
                } else {
                    ctx.response().setStatusCode(401).end(JsonObject().put("error", "Require MFA").toString())
                }
            } else {
                ctx.response().setStatusCode(401).end(JsonObject().put("error", "Invalid Token").toString())
            }
        }
    }

    router.post("/auth").handler { ctx ->
        if ((!Strings.isNullOrEmpty(ctx.request().getFormAttribute("username")) && (!Strings.isNullOrEmpty(ctx.request().getFormAttribute("password"))))) {
            if (UserManager.hasUser(ctx.request().getFormAttribute("username").toLowerCase())) {
                val user = UserManager.getUserByEmail(ctx.request().getFormAttribute("username").toLowerCase())
                if (user.authenticate(ctx.request().getFormAttribute("password"))) {
                    val hash = UserHash.createWebUser(user)
                    val userIP = ctx.request().remoteAddress().host()
                    IPLocationService.getLocation(userIP, {})
                    AuthLogService.logUserActivity(user, userIP, UserActivityLogType.AUTHENTICATION, Strings.nullToEmpty(ctx.request().getHeader("User-Agent")))
                    ctx.response().end(JsonObject().put("token", hash).put("requireMFA", user.hasMFA()).toString())
                } else {
                    ctx.fail(401)
                }
            } else {
                ctx.fail(401)
            }
        } else {
            ctx.fail(400)
        }
    }
}

fun multipartPostHandler(router: Router) {
    router.post().handler { ctx ->
        ctx.request().isExpectMultipart = true
        ctx.request().endHandler { aVoid ->
            ctx.next()
        }
    }
}