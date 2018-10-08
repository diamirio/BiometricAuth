package com.tailoredapps.biometricauth.delegate

import android.support.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class AuthenticationEvent(
        val type: Type,
        val messageId: Int? = null,
        val string: CharSequence? = null
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    enum class Type {
        ERROR,
        HELP,
        SUCCESS,
        FAILED
    }
}

