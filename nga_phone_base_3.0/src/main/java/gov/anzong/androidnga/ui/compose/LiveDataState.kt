package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner

/** 在组合中读取 LiveData 的当前值，随页面生命周期订阅。 */
@Composable
fun <T> LiveData<T>.observedValue(): T? {
    val owner = LocalLifecycleOwner.current
    var value by remember(this) { mutableStateOf(this.value) }
    DisposableEffect(this, owner) {
        val observer = Observer<T> { value = it }
        observe(owner, observer)
        onDispose { removeObserver(observer) }
    }
    return value
}
