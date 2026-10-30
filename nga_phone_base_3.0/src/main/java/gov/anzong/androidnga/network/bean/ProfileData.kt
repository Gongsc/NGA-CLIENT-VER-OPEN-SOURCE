package gov.anzong.androidnga.network.bean

import gov.anzong.androidnga.task.ProfileStatus

/** 用户资料。 */
class ProfileData {

    var adminForums: List<AdminForumsData>? = null

    var reputationEntryList: List<ReputationData>? = null

    var avatarUrl: String? = null

    var emailAddress: String? = null

    var frame: String? = null

    var memberGroup: String? = null

    var money: String? = null

    var isMuted = false

    var mutedTime: String? = null

    /** 激活状态，响应中没有相关字段时为 null。 */
    var active: ProfileStatus.Active? = null

    /** 各项状态（道具、称号等）的说明，与禁言无关。 */
    var buffs: List<String> = emptyList()

    var phoneNumber: String? = null

    var postCount: String? = null

    var registerDate: String? = null

    var sign: String? = null

    var uid: String? = null

    var userName: String? = null
}
