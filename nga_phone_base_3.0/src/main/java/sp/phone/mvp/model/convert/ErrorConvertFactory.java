package sp.phone.mvp.model.convert;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonObject;

import java.net.UnknownHostException;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ContextUtils;;

/**
 * Created by Justwen on 2017/11/23.
 */
public class ErrorConvertFactory {

    public static String getErrorMessage(String js) {
        if (js.isEmpty()) {
            return ContextUtils.getString(R.string.network_error);
        } else if (js.contains("未登录")) {
            return "请重新登录";
        } else if (js.contains("无此页")) {
            return ContextUtils.getString(R.string.last_page_prompt);
        } else {
            try {
                NgaJsonObject obj = (NgaJsonObject) NgaJson.parse(js);
                obj = (NgaJsonObject) obj.get("data");
                obj = (NgaJsonObject) obj.get("__MESSAGE");
                return obj.getString("1");
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static String getErrorMessage(Throwable throwable) {
        String error;
        if (throwable instanceof UnknownHostException) {
            error = ContextUtils.getString(R.string.network_error);
        } else {
            error = throwable.getMessage();
        }
        return error;
    }

}
