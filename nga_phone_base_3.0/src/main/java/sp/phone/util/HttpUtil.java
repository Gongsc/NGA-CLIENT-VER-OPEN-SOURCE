package sp.phone.util;

import gov.anzong.androidnga.common.util.AppEnvironment;

/** 论坛附件域名与头像缓存目录；网络请求统一经 Retrofit（NgaApi）或 NgaWriteClient。 */
public class HttpUtil {

    public static final String NGA_ATTACHMENT_HOST = "img.nga.cn";

    public static final String PATH_AVATAR = AppEnvironment.getExternalStoragePictureDirectory() + "/nga_cache/nga_cache";

    private HttpUtil() {
    }
}
