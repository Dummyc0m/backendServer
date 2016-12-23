package cn.com.guardiantech.classroom.server.data.permission

data class Permission(val name: String, val description: String = "Ability to $name") {
    override fun toString(): String {
        return this.name
    }
}