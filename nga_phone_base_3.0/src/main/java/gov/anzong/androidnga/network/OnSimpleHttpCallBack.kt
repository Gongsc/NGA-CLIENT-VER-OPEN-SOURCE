package gov.anzong.androidnga.network

/**
 * Created by Justwen on 2017/10/10.
 */
fun interface OnSimpleHttpCallBack<T> {

    fun onResult(data: T)
}
