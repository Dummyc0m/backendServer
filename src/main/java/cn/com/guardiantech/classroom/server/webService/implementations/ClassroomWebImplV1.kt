package cn.com.guardiantech.classroom.server.webService.implementations

import cn.codetector.util.Validator.MD5
import cn.codetector.util.Validator.SHA
import cn.com.guardiantech.classroom.server.data.multifactorauthentication.MFAUtil
import cn.com.guardiantech.classroom.server.data.profile.ProfileService
import cn.com.guardiantech.classroom.server.data.security.authlog.AuthLogService
import cn.com.guardiantech.classroom.server.data.security.authlog.UserActivityLogType
import cn.com.guardiantech.classroom.server.data.security.ipLoaction.IPLocationService
import cn.com.guardiantech.classroom.server.data.user.UserHash
import cn.com.guardiantech.classroom.server.data.user.UserManager
import cn.com.guardiantech.classroom.server.data.user.WebUser
import cn.com.guardiantech.classroom.server.webService.*
import com.google.common.base.Strings
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.impl.BodyHandlerImpl

@WebAPIImpl(prefix = "v1")
class ClassroomWebImplV1 : IWebAPIImpl {
    private val noAuthExceptions: Set<String> = hashSetOf("/v1/auth", "/v1/register", "/v1/mfa")
    private val logger = LoggerFactory.getLogger("APIv1")

    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {
        logger.info("v1 Init")
        router.exceptionHandler { e ->
            logger.error("Error in Route", e)
        }
        multipartPostHandler(router)
        ajaxXSiteHandler(router)
        preflightRouteHandler(router)
        authHandler(router, noAuthExceptions)

        //Auth & Register
        router.route("/auth/verify").handler { ctx ->
            ctx.response().end(JsonObject().put("isMfaAuthed", (ctx.user() as WebUser).mfaAuthed).put("userinfo", ctx.user().principal()).toString())
        }
        router.post("/auth/mfa").handler { ctx ->
            val form = ctx.request().formAttributes()
            val user = ctx.user() as WebUser
            if (!user.mfaAuthed && user.user.hasMFA()) {
                if (form.contains("code")) {
                    val validationResult = user.user.verifyMFA(form.get("code"))
                    if (validationResult) {
                        user.mfaAuthed = true
                        AuthLogService.logUserActivity(user.user, ctx.request().remoteAddress().host(), UserActivityLogType.MULTI_FACTOR_AUTHENTICATION)
                    }
                    ctx.response().end(JsonObject().put("mfaResult", validationResult).toString())
                } else {
                    ctx.fail(400)
                }
            } else {
                user.mfaAuthed = true
                ctx.response().end(JsonObject().put("mfaResult", user.mfaAuthed).toString())
            }
        }
        router.get("/auth/mfa/getToken").handler { ctx ->
            ctx.response().end(JsonObject().put("token",MFAUtil.generateBase32Secret()).toString())
        }
        router.post("/auth/mfa/setup").handler { ctx ->
            val user = ctx.user() as WebUser
            val form = ctx.request().formAttributes()
            if (!user.user.hasMFA() && form.contains("secret") && form.contains("code")) {
                if (MFAUtil.generateCurrentNumber(form.get("secret")).equals(form.get("code"), ignoreCase = true)){
                    user.user.setUpMFA(form.get("secret"))
                    ctx.response().end(JsonObject().put("succeed", true).toString())
                } else {
                    ctx.response().end(JsonObject().put("succeed" +
                            "", false).toString())
                }
            } else {
                ctx.fail(400)
            }
        }
        router.post("/auth/mfa/disable").handler { ctx ->
            val user = ctx.user() as WebUser
            if (ctx.request().formAttributes().contains("code")) {
                val authSuccess = user.user.verifyMFA(ctx.request().getFormAttribute("code"))
                if (authSuccess) {
                    user.user.disableMFA()
                }
                ctx.response().end(JsonObject().put("succeed", authSuccess).toString())
            } else {
                ctx.fail(400)
            }
        }
        router.post("/auth/changepassword").handler { ctx ->
            val form = ctx.request().formAttributes()
            if (form.get("oldpass").isNotBlank() && form.get("newpass").isNotBlank()) {
                if ((ctx.user() as WebUser).user.updatePassword(form.get("oldpass"), form.get("newpass"))) {
                    ctx.response().end(JsonObject().put("success", true).toString())
                } else {
                    ctx.response().setStatusCode(400).end(JsonObject().put("success", false).toString())
                }
            }
        }
        router.post("/register").handler { ctx ->
            val form = ctx.request().formAttributes()
            if ( (!Strings.isNullOrEmpty(form.get("email"))) and (!Strings.isNullOrEmpty(form.get("fullName"))) and (!Strings.isNullOrEmpty(form.get("password")))) {
                UserManager.registerUser(form.get("email"), form.get("password"), form.get("fullName"), {
                    result ->
                    if (result.succeeded()) {
                        ctx.response().end(JsonObject().put("registerSuccess", result.result()).toString())
                    } else {
                        ctx.fail(500)
                        logger.warn(result.cause())
                    }
                })
                ctx.fail(400)
            }
        }
        router.route("/auth/signOut").handler { ctx ->
            UserHash.revokeToken(ctx.request().getHeader("Authorization"))
            ctx.response().end()
        }
        router.route("/auth/permission/:permission").handler {
            it.user().isAuthorised(it.pathParam("permission"), { perm ->
                it.response().end(JsonObject().put("permission", it.pathParam("permission")).put("isAuthorized", perm.result()).toString())
            })
        }
        router.route("/auth/permission/").handler { ctx ->
            val allPermissions = JsonArray()
            (ctx.user() as WebUser).user.role.allPermissions().forEach { permission ->
                allPermissions.add(permission.name)
            }
            ctx.response().end(JsonObject().put("permissions",allPermissions).encode())
        }

        //User Profile
        router.get("/profile/:serviceName").handler { ctx ->
            val user = ctx.user() as WebUser
            ctx.response().end(ProfileService.fetchUserProfile(user.user, ctx.pathParam("serviceName")).toString())
        }

        //Usercenter - Account
        router.get("/usercenter/authlog").handler { ctx ->
            val user = (ctx.user() as WebUser).user
            AuthLogService.fetchUserActivity(user, UserActivityLogType.AUTHENTICATION, { result ->
                if (result.succeeded()) {
                    ctx.response().end(result.result().toString())
                } else {
                    ctx.fail(result.cause())
                }
            })
        }
        router.get("/usercenter/mfaStatus").handler { ctx ->
            ctx.response().end(JsonObject().put("mfaEnabled", (ctx.user() as WebUser).user.hasMFA()).toString())
        }
        router.get("/usercenter/ipGeolocation/:ip").handler { ctx ->
            IPLocationService.getLocationWithCache(ctx.pathParam("ip"), {
                result ->
                if (result.succeeded()){
                    ctx.response().end(JsonObject().put("location", result.result().split(" ")[0]).toString())
                }else {
                    ctx.response().end(JsonObject().put("location", "Unknown").toString())
                }
            })
        }

        //FileService - Upload / Download
//        router.post("/file/upload").handler { ctx ->
//            ctx.request().uploadHandler { upload ->
//
//            }
//        }
    }
}