package com.tailoredapps.biometricauth.delegate.legacy

import android.support.annotation.RestrictTo
import com.tailoredapps.biometricauth.BiometricAuth
import io.reactivex.Completable

@RestrictTo(RestrictTo.Scope.LIBRARY)
class LegacyBiometricAuth : BiometricAuth {

    override val hasFingerprintHardware: Boolean = false

    override val hasFingerprintsEnrolled: Boolean = false

    override fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Completable {
        return Completable.error(NotImplementedError("Pre Android 23 devices do not support native biometric authentication"))
    }

}