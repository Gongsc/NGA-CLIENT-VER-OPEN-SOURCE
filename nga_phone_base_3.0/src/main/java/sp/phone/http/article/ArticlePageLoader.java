package sp.phone.http.article;

import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;

import okhttp3.ResponseBody;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/** 同一账号下按有限顺序尝试服务端响应格式，不触发页面导航。 */
public final class ArticlePageLoader<T> {
    public interface Fetcher {
        Observable<ResponseBody> fetch(String url, Map<String, String> headers);
    }

    public interface Parser<T> {
        T parse(String json);
    }

    private static final String[] FORMATS = {
        "__output=8&noprefix&v2", "", "lite=xml", "__output=10"
    };
    private static final String READ_USER_AGENT = "NGA_WP_JW/(;WINDOWS)";
    private final Fetcher fetcher;
    private final Parser<T> parser;

    public ArticlePageLoader(Fetcher fetcher, Parser<T> parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    public Observable<T> load(String baseUrl, Map<String, String> headers) {
        Map<String, String> snapshot = new HashMap<>();
        if (headers != null) snapshot.putAll(headers);
        snapshot.put("User-Agent", READ_USER_AGENT);
        snapshot.put("X-User-Agent", READ_USER_AGENT);
        snapshot.put("Referer", baseUrl);
        return attempt(baseUrl, snapshot, 0);
    }

    private Observable<T> attempt(String baseUrl, Map<String, String> headers, int format) {
        String url = baseUrl + (FORMATS[format].isEmpty() ? "" : "&" + FORMATS[format]);
        Map<String, String> requestHeaders = new HashMap<>(headers);
        if (format > 0) {
            url += "&nga_client_retry=" + System.nanoTime();
            requestHeaders.put("Cache-Control", "no-cache, no-store");
            requestHeaders.put("Pragma", "no-cache");
        }
        final String requestUrl = url;
        return Observable.defer(() -> fetcher.fetch(requestUrl, requestHeaders))
                .map(
                        body -> {
                            String source;
                            try (ResponseBody response = body) {
                                Charset charset = Charset.forName("GB18030");
                                if (response.contentType() != null)
                                    charset = response.contentType().charset(charset);
                                // NGA 常以 GBK 标头发送 GB18030 内容，使用其超集避免丢失扩展字符。
                                if (charset.name().equalsIgnoreCase("GBK")
                                        || charset.name().equalsIgnoreCase("GB2312"))
                                    charset = Charset.forName("GB18030");
                                source = new String(response.bytes(), charset);
                            }
                            if (ArticleResponse.isIncomplete(source))
                                throw new ResponseFormatException(true);
                            JSONObject data;
                            try {
                                data = ArticleResponse.decode(source);
                            } catch (Exception error) {
                                throw new ResponseFormatException();
                            }
                            String message = ArticleResponse.businessError(data);
                            if (message != null && !message.isEmpty())
                                throw new BusinessException(message);
                            try {
                                validate(data, baseUrl);
                                JSONObject root = new JSONObject();
                                root.put("data", data);
                                T result = parser.parse(root.toJSONString());
                                if (result == null) throw new ResponseFormatException();
                                return result;
                            } catch (RuntimeException error) {
                                throw new ResponseFormatException();
                            }
                        })
                .onErrorResumeNext(
                        error -> {
                            if (!(error instanceof ResponseFormatException))
                                return Observable.error(error);
                            return format + 1 < FORMATS.length
                                    ? attempt(baseUrl, headers, format + 1)
                                    : Observable.error(error);
                        });
    }

    /** 所有来源都先校验，再进入正文转换和缓存，禁止把半页内容当成完整页。 */
    private static void validate(JSONObject data, String baseUrl) throws ResponseFormatException {
        JSONObject topic = data.getJSONObject("__T");
        JSONObject rows = data.getJSONObject("__R");
        if (topic == null
                || rows == null
                || !data.containsKey("__ROWS")
                || !data.containsKey("__R__ROWS")
                || data.getIntValue("__ROWS") < 0
                || rows.size() != data.getIntValue("__R__ROWS"))
            throw new ResponseFormatException();
        okhttp3.HttpUrl requested = okhttp3.HttpUrl.parse(baseUrl);
        String tid = requested == null ? null : requested.queryParameter("tid");
        if (tid != null && !tid.equals(topic.getString("tid"))) throw new ResponseFormatException();
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(String.valueOf(i));
            if (row == null || (!row.containsKey("content") && !row.containsKey("subject")))
                throw new ResponseFormatException();
        }
    }

    public static final class ResponseFormatException extends Exception {
        private final boolean incomplete;

        public ResponseFormatException() {
            this(false);
        }

        private ResponseFormatException(boolean incomplete) {
            super(incomplete ? "主题数据接收不完整，请稍后重试。" : "主题数据暂时无法解析，请稍后重试。");
            this.incomplete = incomplete;
        }

        public boolean isIncomplete() {
            return incomplete;
        }
    }

    public static final class BusinessException extends Exception {
        public BusinessException(String message) {
            super(message);
        }
    }
}
