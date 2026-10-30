package gov.anzong.androidnga.task

import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.http.OnHttpCallBack

/** 以提示显示写操作结果：成功时为论坛的提示文字，失败时为原因。 */
object ToastResult : OnHttpCallBack<String?> {
    override fun onSuccess(data: String?) = ToastUtils.success(data)
    override fun onError(text: String?) = ToastUtils.error(text ?: "提交失败，请稍后重试")
}
