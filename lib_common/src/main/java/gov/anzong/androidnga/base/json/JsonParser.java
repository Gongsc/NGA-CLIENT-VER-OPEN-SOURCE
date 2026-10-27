package gov.anzong.androidnga.base.json;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 宽松 JSON 解析器，接受范围与 fastjson 1.1 对齐。
 *
 * <p>接受前导零数字、无引号键、单引号字符串、块注释、多余逗号、{@code \x} 转义与值 {@code undefined}；拒绝文档后的多余文本、
 * {@code +1}、{@code .5}、{@code NaN}、大写关键字与行注释。对象保持插入顺序。
 */
final class JsonParser {

    /** 对象中值为 undefined 的键被丢弃，数组中视为 null。 */
    private static final Object UNDEFINED = new Object();

    private final String mText;
    private int mPos;

    private JsonParser(String text) {
        mText = text;
    }

    /** 空白文档与 {@code null} 返回 null。 */
    static Object parse(String text) {
        if (text == null) {
            return null;
        }
        JsonParser parser = new JsonParser(text);
        parser.skipBlank();
        if (parser.mPos >= text.length()) {
            return null;
        }
        Object value = parser.readValue();
        parser.skipBlank();
        if (parser.mPos < text.length()) {
            throw parser.error("文档末尾存在多余内容");
        }
        return value == UNDEFINED ? null : value;
    }

    private Object readValue() {
        char c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
            case '\'':
                return readString(c);
            case 't':
                return readKeyword("true", Boolean.TRUE);
            case 'f':
                return readKeyword("false", Boolean.FALSE);
            case 'n':
                return readKeyword("null", null);
            case 'u':
                return readKeyword("undefined", UNDEFINED);
            default:
                if (c == '-' || isDigit(c)) {
                    return readNumber();
                }
                throw error("无法识别的值");
        }
    }

    private NgaJsonObject readObject() {
        NgaJsonObject object = new NgaJsonObject();
        mPos++;
        while (true) {
            skipBlank();
            char c = peek();
            if (c == '}') {
                mPos++;
                return object;
            }
            if (c == ',') {
                mPos++;
                continue;
            }
            String key = c == '"' || c == '\'' ? readString(c) : readIdentifier();
            skipBlank();
            if (peek() != ':') {
                throw error("缺少冒号");
            }
            mPos++;
            skipBlank();
            Object value = readValue();
            if (value != UNDEFINED) {
                object.put(key, value);
            }
            skipBlank();
            c = peek();
            if (c == ',') {
                mPos++;
            } else if (c == '}') {
                mPos++;
                return object;
            } else {
                throw error("对象未正确结束");
            }
        }
    }

    private NgaJsonArray readArray() {
        NgaJsonArray array = new NgaJsonArray();
        mPos++;
        while (true) {
            skipBlank();
            char c = peek();
            if (c == ']') {
                mPos++;
                return array;
            }
            if (c == ',') {
                mPos++;
                continue;
            }
            Object value = readValue();
            array.add(value == UNDEFINED ? null : value);
            skipBlank();
            c = peek();
            if (c == ',') {
                mPos++;
            } else if (c == ']') {
                mPos++;
                return array;
            } else {
                throw error("数组未正确结束");
            }
        }
    }

    private String readString(char quote) {
        StringBuilder builder = new StringBuilder();
        mPos++;
        while (true) {
            if (mPos >= mText.length()) {
                throw error("字符串未结束");
            }
            char c = mText.charAt(mPos++);
            if (c == quote) {
                return builder.toString();
            }
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (mPos >= mText.length()) {
                throw error("字符串未结束");
            }
            char escaped = mText.charAt(mPos++);
            switch (escaped) {
                case '"':
                case '\'':
                case '\\':
                case '/':
                    builder.append(escaped);
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    builder.append((char) readHex(4));
                    break;
                case 'x':
                    builder.append((char) readHex(2));
                    break;
                default:
                    throw error("无效的转义字符");
            }
        }
    }

    private int readHex(int length) {
        if (mPos + length > mText.length()) {
            throw error("转义字符不完整");
        }
        try {
            int value = Integer.parseInt(mText.substring(mPos, mPos + length), 16);
            mPos += length;
            return value;
        } catch (NumberFormatException e) {
            throw error("无效的十六进制转义");
        }
    }

    /** 无引号键：纯数字，或以字母、下划线开头并由字母、数字、下划线组成。 */
    private String readIdentifier() {
        int start = mPos;
        char first = peek();
        if (isDigit(first)) {
            skipDigits();
        } else if (Character.isLetter(first) || first == '_') {
            while (mPos < mText.length()
                    && (Character.isLetterOrDigit(mText.charAt(mPos)) || mText.charAt(mPos) == '_')) {
                mPos++;
            }
        } else {
            throw error("无效的键名");
        }
        return mText.substring(start, mPos);
    }

    private Object readKeyword(String keyword, Object value) {
        if (!mText.startsWith(keyword, mPos)) {
            throw error("无法识别的值");
        }
        mPos += keyword.length();
        if (mPos < mText.length() && isIdentifierPart(mText.charAt(mPos))) {
            throw error("无法识别的值");
        }
        return value;
    }

    private Object readNumber() {
        int start = mPos;
        if (peek() == '-') {
            mPos++;
        }
        int digits = skipDigits();
        if (digits == 0) {
            throw error("数字格式错误");
        }
        boolean fraction = false;
        boolean exponent = false;
        if (mPos < mText.length() && mText.charAt(mPos) == '.') {
            mPos++;
            fraction = true;
            if (skipDigits() == 0) {
                throw error("数字格式错误");
            }
        }
        if (mPos < mText.length() && (mText.charAt(mPos) == 'e' || mText.charAt(mPos) == 'E')) {
            mPos++;
            exponent = true;
            if (mPos < mText.length() && (mText.charAt(mPos) == '+' || mText.charAt(mPos) == '-')) {
                mPos++;
            }
            if (skipDigits() == 0) {
                throw error("数字格式错误");
            }
        }
        if (mPos < mText.length() && (isIdentifierPart(mText.charAt(mPos)) || mText.charAt(mPos) == '.')) {
            throw error("数字格式错误");
        }
        String number = mText.substring(start, mPos);
        if (exponent) {
            return Double.parseDouble(number);
        }
        if (fraction) {
            return new BigDecimal(number);
        }
        // fastjson 按位数而非数值区分：9 位以内为 Integer，否则为 Long 或 BigInteger。
        if (digits < 10) {
            return Integer.parseInt(number);
        }
        BigInteger value = new BigInteger(number);
        return value.bitLength() < 64 ? (Object) value.longValue() : value;
    }

    private int skipDigits() {
        int start = mPos;
        while (mPos < mText.length() && isDigit(mText.charAt(mPos))) {
            mPos++;
        }
        return mPos - start;
    }

    private void skipBlank() {
        while (mPos < mText.length()) {
            char c = mText.charAt(mPos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f' || c == '\b') {
                mPos++;
            } else if (c == '/' && mText.startsWith("/*", mPos)) {
                int end = mText.indexOf("*/", mPos + 2);
                if (end < 0) {
                    throw error("注释未结束");
                }
                mPos = end + 2;
            } else {
                return;
            }
        }
    }

    private char peek() {
        if (mPos >= mText.length()) {
            throw error("文档意外结束");
        }
        return mText.charAt(mPos);
    }

    private NgaJsonException error(String message) {
        return new NgaJsonException(message + "，位置 " + mPos);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** 关键字与数字之后不能紧跟的字符。 */
    private static boolean isIdentifierPart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || isDigit(c);
    }
}
