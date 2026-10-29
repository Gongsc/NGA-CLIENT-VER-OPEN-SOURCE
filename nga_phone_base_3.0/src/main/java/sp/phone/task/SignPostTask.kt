package sp.phone.task

import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.withContext
import sp.phone.http.post.ForumActionClient
import sp.phone.param.SignPostParam

/** 修改签名，经 [ForumActionClient] 提交。 */
class SignPostTask @JvmOverloads constructor(private val client: () -> ForumActionClient = ForumActionClient::create) {

    /** 提交签名，成功返回论坛提示；论坛拒绝或结果无法确认时抛出带原因的异常。 */
    suspend fun submit(postParam: SignPostParam): String = withContext(NgaDispatchers.io) {
        client().setSignature(postParam.uid.orEmpty(), postParam.sign.orEmpty())
    }
}
