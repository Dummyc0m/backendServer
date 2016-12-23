package cn.com.guardiantech.classroom.server.data.user

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

/**
 * Created by codetector on 21/11/2016.
 */
class IDAuthProvider : AuthProvider {
    val timeout = 1000 * 60 * 30

    override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
        if (authInfo!!.containsKey("auth")) {
            if (UserHash.isAuthKeyValid(authInfo.getString("auth"))) {
                resultHandler!!.handle(Future.succeededFuture(UserHash.getUserByAuthKey(authInfo.getString("auth"))))
            } else {
                resultHandler!!.handle(Future.failedFuture("Invalid Token"))
            }
        } else {
            resultHandler!!.handle(Future.failedFuture("Malformed authInfo"))
        }
    }
//    if (authInfo!!.containsKey("username") && authInfo.containsKey("password")){
//        val username:String = authInfo.getString("username")!!
//        val db_password:String = authInfo.getString("db_password")!!
//        if (UserManager.hasUser(username)){
//            if (UserManager.getUserByUsername(username).authenticate(password)){
//                resultHandler!!.handle(Future.succeededFuture(WebUser(UserManager.getUserByUsername(username))))
//            }else{
//                resultHandler!!.handle(Future.failedFuture("Wrong password"))
//            }
//        }else{
//            resultHandler!!.handle(Future.failedFuture("User DNE"))
//        }
//    }else{
//        resultHandler!!.handle(Future.failedFuture("Malformed auth info"))
//    }
}