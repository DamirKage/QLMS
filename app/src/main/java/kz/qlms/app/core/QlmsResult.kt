package kz.qlms.app.core

/** Simple explicit result type so ViewModels never have to guess whether a repository call can throw. */
sealed class QlmsResult<out T> {
    data class Success<T>(val data: T) : QlmsResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : QlmsResult<Nothing>()
    data object Loading : QlmsResult<Nothing>()
}

inline fun <T> QlmsResult<T>.onSuccess(action: (T) -> Unit): QlmsResult<T> {
    if (this is QlmsResult.Success) action(data)
    return this
}

inline fun <T> QlmsResult<T>.onError(action: (String) -> Unit): QlmsResult<T> {
    if (this is QlmsResult.Error) action(message)
    return this
}
