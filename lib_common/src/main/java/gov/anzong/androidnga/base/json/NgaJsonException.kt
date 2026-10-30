package gov.anzong.androidnga.base.json

/** JSON 解析、类型转换或绑定失败。 */
class NgaJsonException : RuntimeException {

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
