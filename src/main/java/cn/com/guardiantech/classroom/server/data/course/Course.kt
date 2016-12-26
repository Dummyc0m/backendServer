package cn.com.guardiantech.classroom.server.data.course

import cn.com.guardiantech.classroom.server.data.user.User
import io.vertx.core.json.JsonArray
import java.util.*

/**
 * Created by Codetector on 2016/12/26.
 */
class Course(val courseId: Int, var courseTitle: String, var courseSubtitle: String, tags: String) {
    val courseTags: MutableList<String> = ArrayList<String>()

    init {
        this.courseTags.clear()
        JsonArray(tags).forEach {
            tag ->
            if (tag is String) {
                addTag(tag)
            } else {

            }
        }
    }

    fun addTag(tag: String) {
        if (!this.courseTags.contains(tag.toLowerCase())) {
            this.courseTags.add(tag.toLowerCase())
        }
    }

    fun removeTag(tag: String){
        val iterator = this.courseTags.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == (tag)) {
                iterator.remove()
                return
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Course){
            return this.courseId == other.courseId
        }
        return super.equals(other)
    }
}