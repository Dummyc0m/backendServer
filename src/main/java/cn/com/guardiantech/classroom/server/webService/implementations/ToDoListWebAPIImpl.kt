package cn.com.guardiantech.classroom.server.webService.implementations

import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration.db_prefix
import cn.com.guardiantech.classroom.server.data.user.WebUser
import cn.com.guardiantech.classroom.server.webService.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

@WebAPIImpl(prefix = "v1")
class ToDoListWebAPIImpl : IWebAPIImpl {
    private val noAuthExceptions: Set<String> = hashSetOf("/v1/auth", "/v1/register")
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

        router.get("/todo/list").handler { ctx ->
            dbClient.getConnection { conn ->
                if (conn.succeeded()) {
                    conn.result().queryWithParams("SELECT *, UNIX_TIMESTAMP(`itemDue`) as `itemDue` FROM `${db_prefix}_items` WHERE `itemOwner` = ?", JsonArray().add(ctx.user().principal().getString("username")), { q ->
                        if (q.succeeded()){
                            ctx.response().end(JsonObject().put("items",q.result().rows).toString())
                        } else {
                            ctx.fail(500)
                        }
                        conn.result().close()
                    })
                } else {
                    ctx.fail(500)
                }
            }
        }

//        router.post("/todo/add").handler { ctx ->
//            val post = ctx.request().formAttributes()
//            if (strNotEmpty(ctx.get("title")))
//        }
    }
}
fun strNotEmpty(str : String?) : Boolean {
    return str != null && str.isNotBlank()
}