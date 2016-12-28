package cn.com.guardiantech.classroom.server.data.security.authlog

/**
 * Created by codetector on 29/12/2016.
 */
enum class UserActivityLogType(private val value: Int) {
    AUTHENTICATION(0),
    MULTI_FACTOR_AUTHENTICATION(1)
}