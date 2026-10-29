package gov.anzong.androidnga.util

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

const val PROJECT_URL = "https://github.com/Gongsc/NGA-CLIENT-VER-OPEN-SOURCE"
const val ISSUES_URL = "$PROJECT_URL/issues"
const val RELEASES_URL = "$PROJECT_URL/releases"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/Gongsc/NGA-CLIENT-VER-OPEN-SOURCE/releases/latest"

data class ReleaseInfo(val version: String, val url: String, val notes: String)

sealed interface UpdateResult {
    data class Available(val release: ReleaseInfo) : UpdateResult
    /** [latest] is null when the repository has no published release yet. */
    data class UpToDate(val latest: String?) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/** Numeric parts of a tag such as "v4.1.0" or "4.1.0-beta"; null when the tag is not a version. */
fun parseVersion(raw: String): List<Int>? {
    val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    if (core.isEmpty() || parts.size > 4) return null
    return parts.map { part -> part.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
}

fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = parseVersion(candidate) ?: return false
    val b = parseVersion(current) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
        if (diff != 0) return diff > 0
    }
    return false
}

/** Maps a GitHub "latest release" response to a result; release links outside this project fall back to [RELEASES_URL]. */
fun interpretLatestRelease(code: Int, tag: String?, htmlUrl: String?, notes: String?, currentVersion: String): UpdateResult = when {
    code == 404 -> UpdateResult.UpToDate(null)
    code == 403 || code == 429 -> UpdateResult.Failed("请求过于频繁，请稍后再试")
    code != 200 -> UpdateResult.Failed("GitHub 返回错误（$code）")
    tag.isNullOrBlank() || parseVersion(tag) == null -> UpdateResult.Failed("无法识别发布版本号")
    isNewerVersion(tag, currentVersion) -> UpdateResult.Available(ReleaseInfo(
        tag.trim().removePrefix("v").removePrefix("V"),
        htmlUrl?.takeIf { it.startsWith("$RELEASES_URL/") } ?: RELEASES_URL,
        notes.orEmpty().trim()))
    else -> UpdateResult.UpToDate(tag.trim().removePrefix("v").removePrefix("V"))
}

private val updateClient by lazy {
    OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
}

/** Queries the latest GitHub release on [NgaDispatchers.io]; never throws for network or parse errors. */
suspend fun checkLatestRelease(currentVersion: String, client: OkHttpClient = updateClient): UpdateResult =
    withContext(NgaDispatchers.io) {
        val request = Request.Builder().url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NGA-Client/$currentVersion")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code != 200) return@use interpretLatestRelease(response.code, null, null, null, currentVersion)
                val json = NgaJson.parseObject(response.body?.string().orEmpty())
                    ?: return@use UpdateResult.Failed("无法解析 GitHub 响应")
                interpretLatestRelease(200, json.getString("tag_name"), json.getString("html_url"), json.getString("body"), currentVersion)
            }
        } catch (e: IOException) {
            UpdateResult.Failed("网络连接失败")
        } catch (e: RuntimeException) {
            UpdateResult.Failed("无法解析 GitHub 响应")
        }
    }
