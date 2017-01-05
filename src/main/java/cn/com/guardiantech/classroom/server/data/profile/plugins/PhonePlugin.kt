package cn.com.guardiantech.classroom.server.data.profile.plugins

import cn.com.guardiantech.classroom.server.api.IProfileService

class PhonePlugin: IProfileService {
    override fun getServiceName(): String {
        return "phone"
    }

}