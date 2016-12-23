package cn.com.guardiantech.classroom.server.mail

import java.util.*

class MailTemplate constructor(val templateContent: String) {
    private var sender: String? = null
    private var title: String? = null
    private val recipients: MutableList<String> = ArrayList()

    fun setStringValue(templateKey: String, value: Any) {
        this.templateContent.replace("{{" + templateKey + "}}", value.toString());
    }

    fun setListValue(templateKey: String, value: List<Any>) {
        var sb: StringBuilder = StringBuilder()
        value.forEach { v ->
            sb.append(v.toString()).append("<br>")
        }
        this.setStringValue(templateKey, sb.toString())
    }

    fun addRecipients(recipients: List<String>) {
        this.recipients.addAll(recipients)
    }

    fun addRecipient(recipient: String) {
        this.recipients.add(recipient)
    }

    fun setSender(sender: String) {
        this.sender = sender;
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun isComplete(): Boolean {
        return (this.sender != null && this.title != null && (this.recipients.size > 0))
    }


}