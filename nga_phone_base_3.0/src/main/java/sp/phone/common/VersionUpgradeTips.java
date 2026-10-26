package sp.phone.common;

/**
 * Release notes shown once after upgrading; looked up by {@code TIPS_<versionCode>}.
 */
public interface VersionUpgradeTips {

    String TIPS_4000 = "4.0.0 更新内容\n"
            + "● 主要界面改用 Jetpack Compose 与 Material 3 重新实现\n"
            + "● 新增松绿、雾蓝、暖砂、石墨四种配色及深色模式\n"
            + "● 侧边栏分级显示账号与应用入口，并显示账号头像\n"
            + "● 首页收藏与论坛收藏同步，按账号隔离缓存\n"
            + "● 改进主题加载、热点回复、正文图片与滚动性能\n"
            + "● 仅支持 Android 12 及以上系统\n"
            + "问题与建议请前往 GitHub 反馈\n";
}
