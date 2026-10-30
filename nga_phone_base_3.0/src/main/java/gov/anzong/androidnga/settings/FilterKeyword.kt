package gov.anzong.androidnga.settings

/** 关键词屏蔽的一项，以 JSON 保存在设置中（键名 keyword、enabled）。 */
class FilterKeyword() {

    var keyword: String? = null

    var isEnabled = false

    constructor(keyword: String?) : this() {
        this.keyword = keyword
        isEnabled = true
    }
}
