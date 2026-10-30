package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.network.bean.ProfileData
import gov.anzong.androidnga.task.requestProfile

/**
 * 用户资料：按 uid 或用户名读取一次，页面重建后保留；请求随 ViewModel 结束取消。
 *
 * [query] 为空表示缺少参数，直接显示错误。
 */
class ProfileViewModel(
    val query: String?,
    private val request: suspend (String) -> ProfileData = { requestProfile(it) },
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 由 extras 中的 "uid" 或 "username" 创建。 */
    constructor(state: SavedStateHandle) : this(profileQuery(state.get<String>("uid"), state.get<String>("username")))

    var profile by mutableStateOf<ProfileData?>(null); private set
    /** 签名单独保存，修改签名后无需重新读取资料。 */
    var signature by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private var job: Job? = null
    private var started = false

    /** 首次进入时读取；重建后不重复请求。 */
    fun start() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        val query = query ?: run { error = "缺少用户参数"; return }
        job?.cancel()
        loading = true
        error = null
        job = viewModelScope.launch {
            try {
                val data = withContext(worker) { request(query) }
                profile = data
                signature = data.sign
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "用户资料加载失败"
            } finally {
                loading = false
            }
        }
    }

    fun updateSignature(sign: String?) {
        profile?.sign = sign
        signature = sign
    }

    companion object {
        /** uid 优先；“UID123” 形式的用户名按 uid 查询，其余用户名按论坛要求以 GBK 编码。 */
        @JvmStatic
        fun profileQuery(uid: String?, userName: String?): String? = when {
            !uid.isNullOrBlank() -> "uid=${uid.trim()}"
            userName.isNullOrBlank() -> null
            userName.startsWith("UID") && userName.substring(3).all(Char::isDigit) && userName.length > 3 -> "uid=${userName.substring(3)}"
            else -> "username=" + URLEncoder.encode(userName, "GBK")
        }

        /** 论坛金钱以铜币计：100 铜 = 1 银，100 银 = 1 金。无法解析时为 0。 */
        @JvmStatic
        fun money(value: String?): Triple<Long, Long, Long> {
            val total = value?.trim()?.toLongOrNull()?.coerceAtLeast(0) ?: 0
            return Triple(total / 10000, total % 10000 / 100, total % 100)
        }

        /** 威望字段为实际值的 10 倍。 */
        @JvmStatic
        fun fame(value: String?): String {
            val number = value?.trim()?.toDoubleOrNull() ?: return value.orEmpty()
            return (number / 10).toString()
        }
    }
}
