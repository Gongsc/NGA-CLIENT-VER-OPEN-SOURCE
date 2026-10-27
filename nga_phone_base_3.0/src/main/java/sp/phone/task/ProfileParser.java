package sp.phone.task;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonObject;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.common.PreferenceKey;

import java.util.ArrayList;
import java.util.List;

import sp.phone.http.bean.AdminForumsData;
import sp.phone.http.bean.ProfileData;
import sp.phone.http.bean.ReputationData;
import sp.phone.util.NLog;
import sp.phone.util.StringUtils;

/** 个人资料响应解析，自 JsonProfileLoadTask 原样拆出。 */
final class ProfileParser {

    private static final String TAG = JsonProfileLoadTask.class.getSimpleName();

    private ProfileParser() {}

    /** 解析个人资料响应；失败时抛出带提示文字的 IllegalStateException。 */
    static ProfileData parse(String js) {
        if (StringUtils.isEmpty(js)) {
            throw new IllegalStateException(ContextUtils.getString(R.string.network_error));
        }
        js = js.replaceAll("window.script_muti_get_var_store=", "");
        if (js.contains("/*error fill content")) {
            js = js.substring(0, js.indexOf("/*error fill content"))
                    .replaceAll("\"content\":\\+(\\d+),", "\"content\":\"+$1\",");
        }
        js = js.replaceAll("\"subject\":\\+(\\d+),", "\"subject\":\"+$1\",")
                .replaceAll("/\\*\\$js\\$\\*/", "");
        NgaJsonObject obj = NgaJson.parseObject(js);
        if (obj.containsKey("data")) {
            NgaJsonObject dataObj = obj.getJSONObject("data");
            try {
                ProfileData ret = new ProfileData();
                dataObj = dataObj.getJSONObject("0");
                buildBasicInfo(ret, dataObj);
                buildReputation(ret, dataObj.getJSONObject("reputation"));
                buildAdminForums(ret, dataObj.getJSONObject("adminForums"));
                return ret;
            } catch (Exception e) {
                NLog.e(TAG, "can not parse :\n" + js);
            }
        }
        obj = obj.getJSONObject("error");
        throw new IllegalStateException(obj == null ? "二哥玩坏了或者你需要重新登录" : obj.getString("0"));
    }

    private static void buildAdminForums(ProfileData ret, NgaJsonObject obj) {
        if (obj != null) {
            List<AdminForumsData> entryList = new ArrayList<>();
            for (String key : obj.keySet()) {
                AdminForumsData entry = new AdminForumsData(key, obj.getString(key));
                entryList.add(entry);
            }
            ret.setAdminForums(entryList);
        }
    }

    private static void buildReputation(ProfileData ret, NgaJsonObject obj) {
        if (obj != null) {
            List<ReputationData> entryList = new ArrayList<>();
            for (int i = 0; i < obj.size(); i++) {
                NgaJsonObject subObj = obj.getJSONObject(String.valueOf(i));
                if (subObj != null) {
                    ReputationData entry = new ReputationData(subObj.getString("0"), subObj.getString("1"));
                    entryList.add(entry);
                }
            }
            ret.setReputationEntryList(entryList);
        }
    }

    private static void buildBasicInfo(ProfileData ret, NgaJsonObject obj) {
        String defaultValue = "N/A";

        String money = obj.getString("money");
        ret.setMoney(StringUtils.isEmpty(money) ? "0" : money);

        String frame = obj.getString("fame");
        ret.setFrame(StringUtils.isEmpty(frame) ? defaultValue : frame);

        String posts = obj.getString("posts");
        ret.setPostCount(StringUtils.isEmpty(posts) ? defaultValue : posts);

        String email = obj.getString("email");
        ret.setEmailAddress(StringUtils.isEmpty(email) ? defaultValue : email);

        String phone = obj.getString("phone");
        ret.setPhoneNumber(StringUtils.isEmpty(phone) ? defaultValue : phone);

        String userName = obj.getString("username");
        ret.setUserName(StringUtils.isEmpty(userName) ? defaultValue : userName);

        String uid = obj.getString("uid");
        ret.setUid(StringUtils.isEmpty(uid) ? defaultValue : uid);

        String group = obj.getString("group");
        ret.setMemberGroup(StringUtils.isEmpty(group) ? defaultValue : group);

        String verified = obj.getString("verified");
        if (!StringUtils.isEmpty(verified)) {
            int state = Integer.parseInt(verified);
            if (state == -1) {
                ret.setNuked(true);
            } else if (state < -1) {
                ret.setMuted(true);
            }
        }
        if (!ret.isNuked()) {
            NgaJsonObject buffObj = obj.getJSONObject("buffs");
            if (buffObj != null && buffObj.containsKey("0")) {
                ret.setMutedTime(buffObj.getString("0"));
                ret.setMuted(true);
            } else if (!StringUtils.isEmpty(obj.getString("muteTime")) && !"0".equals(obj.getString("muteTime"))) {
                ret.setMuted(true);
                ret.setMutedTime("禁言至: " + StringUtils.timeStamp2Date1(obj.getString("muteTime")));
            }
        }
        String regdate = obj.getString("regdate");
        ret.setRegisterDate(StringUtils.isEmpty(regdate) ? defaultValue : StringUtils.timeStamp2Date1(regdate));

        String sign = obj.getString("sign");
        ret.setSign(StringUtils.isEmpty(sign) ? "无签名" : sign);

        ret.setAvatarUrl(obj.getString(PreferenceKey.PREFERENCE_AVATAR));
    }
}
