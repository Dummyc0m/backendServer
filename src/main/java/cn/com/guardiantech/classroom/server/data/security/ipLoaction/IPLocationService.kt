package cn.com.guardiantech.classroom.server.data.security.ipLoaction

import cn.com.guardiantech.classroom.server.Main
import cn.com.guardiantech.classroom.server.data.AbstractDataService
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory
import org.jsoup.Jsoup

/**
 * Created by codetector on 28/12/2016.
 */
object IPLocationService : AbstractDataService() {
    val logger = LoggerFactory.getLogger("IPLocationService")
    lateinit var httpClient: HttpClient
    override fun initialize() {
        logger.info("Initializing IP Geo Location Service")
        httpClient = Main.sharedVertx.createHttpClient()
    }

    override fun saveToDatabase(action: () -> Unit) {
    }

    override fun loadFromDatabase(action: () -> Unit) {
    }

    fun getLocationWithCache(ip: String, handler: (AsyncResult<String>) -> Any) {
        dbClient.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().queryWithParams("SELECT * FROM `${dbprefix}_iptable` WHERE `ip` = ?",
                        JsonArray().add(ip),
                        {
                            query ->
                            if (query.succeeded() && query.result().numRows > 0){
                                handler.invoke(Future.succeededFuture(query.result().rows[0].getString("geoLocation")))
                            } else {
                                getLocation(ip, handler)
                            }
                            conn.result().close()
                        })
            }
        }
    }

    fun getLocation(ip: String, handler: (AsyncResult<String>) -> Any) {
        httpClient.get(80, "ip.cn", "/index.php?ip=${ip}", { response ->
            response.bodyHandler { body ->
                try {
                    val elems = Jsoup.parse(body.toString("UTF-8")).getElementById("result").getElementsByTag("code")
                    if (elems.size > 1) {
                        handler.invoke(Future.succeededFuture(elems[1].text()))
                        dbClient.getConnection { conn ->
                            if (conn.succeeded()) {
                                conn.result().updateWithParams("INSERT INTO `${dbprefix}_iptable` (`ip`, `geoLocation`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `geoLocation` = VALUES(`geoLocation`)", JsonArray().add(elems[0].text()).add(elems[1].text()), { sql ->
                                    if (sql.failed()) {
                                        logger.error("Failed to log", sql.cause())
                                    }
                                    conn.result().close()
                                })
                            }
                        }
                    } else {
                        handler.invoke(Future.failedFuture("Unable to locate information from ip.cn"))
                        logger.warn("Having trouble locating information from ip.cn")
                    }
                } catch (t: Throwable) {
                    logger.error("ip.cn structure may have changed!", t)
                }
            }
        }).putHeader("Content-Type", "text/html; charset=utf-8").putHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36").end()
    }
}