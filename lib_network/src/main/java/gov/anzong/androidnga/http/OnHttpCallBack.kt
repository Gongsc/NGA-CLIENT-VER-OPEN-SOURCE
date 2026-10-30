package gov.anzong.androidnga.http

interface OnHttpCallBack<in T> {

    fun onError(text: String?) {
    }

    fun onSuccess(data: T) {
    }

    fun onError(msg: String?, t: Throwable?) {
    }
}
