package gov.anzong.androidnga.base.json;

/** JSON 解析、类型转换或绑定失败。 */
public class NgaJsonException extends RuntimeException {

    public NgaJsonException(String message) {
        super(message);
    }

    public NgaJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
