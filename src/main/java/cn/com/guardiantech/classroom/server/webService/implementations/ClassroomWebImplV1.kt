package cn.com.guardiantech.classroom.server.webService.implementations

import cn.codetector.util.Validator.MD5
import cn.codetector.util.Validator.SHA
import cn.com.guardiantech.classroom.server.data.user.UserHash
import cn.com.guardiantech.classroom.server.data.user.UserManager
import cn.com.guardiantech.classroom.server.data.user.WebUser
import cn.com.guardiantech.classroom.server.webService.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

@WebAPIImpl(prefix = "v1")
class ClassroomWebImplV1 : IWebAPIImpl {
    private val noAuthExceptions: Set<String> = hashSetOf("/v1/auth", "/v1/register", "/v1/mfa")
    private val logger = LoggerFactory.getLogger("APIv1")

    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {
        logger.info("v1 Init")
        router.exceptionHandler { e ->
            logger.error("Error in Route", e)
        }
        ajaxXSiteHandler(router)
        preflightRouteHandler(router)
        multipartPostHandler(router)
        authHandler(router, noAuthExceptions)

        router.route("/auth/verify").handler { ctx ->
            ctx.response().end(ctx.user().principal().toString())
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
            if (form.contains("email") and form.contains("fullName") and form.contains("password")) {
                UserManager.registerUser(form.get("email"), form.get("password"), form.get("fullName"), {
                    result ->
                    if (result.succeeded()) {
                        ctx.response().end(JsonObject().put("registerSuccess",result.result()).toString())
                    } else {
                        ctx.fail(500)
                        logger.warn(result.cause())
                    }
                })
            } else {
                ctx.fail(400)
            }
        }

        router.route("/auth/signOut").handler { ctx ->
            UserHash.revokeToken(ctx.request().getHeader("Authorization"))
            ctx.response().end()
        }

//        router.post("/todo/add").handler { ctx ->
//            val post = ctx.request().formAttributes()
//            if (strNotEmpty(ctx.get("title")))
//        }
        //TODO delete
        router.get("/magic/:pass").handler { ctx ->
            ctx.response().end(SHA.getSHA256String(MD5.getMD5String(ctx.pathParam("pass"))))
        }
    }
}