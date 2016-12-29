package cn.com.guardiantech.classroom.server.webService.implementations

import cn.codetector.util.Validator.MD5
import cn.codetector.util.Validator.SHA
import cn.com.guardiantech.classroom.server.data.configuration.DatabaseConfiguration.db_prefix
import cn.com.guardiantech.classroom.server.data.security.ipLoaction.IPLocationService
import cn.com.guardiantech.classroom.server.data.user.WebUser
import cn.com.guardiantech.classroom.server.webService.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

@WebAPIImpl(prefix = "test")
class Test : IWebAPIImpl {
    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {
        router.get("/compute/:pass").handler { ctx ->
            ctx.response().end(SHA.getSHA256String(MD5.getMD5String(ctx.pathParam("pass"))))
        }
        router.get("/geoip/:ip").handler {
            ctx ->
            IPLocationService.getLocation(ctx.pathParam("ip"), {response ->
                ctx.response().end(response.result())
            })
        }
    }
}