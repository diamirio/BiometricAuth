package com.tailoredapps.biometricauth.delegate.legacy

import androidx.annotation.RestrictTo
import com.tailoredapps.biometricauth.BiometricAuth
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Implementation of [BiometricAuth] for Pre-Marshmallow devices (SDK < 23).
 *
 * This is a stub-implementation which returns false on the [hasFingerprintHardware] and
 * [hasFingerprintsEnrolled] flags, and returns an error instantly at [authenticate].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class LegacyBiometricAuth : BiometricAuth {

    override val hasFingerprintHardware: Boolean get() = false

    override val hasFingerprintsEnrolled: Boolean get() = false


    override fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Completable {
        return Completable.error(NotImplementedError("Pre Android 23 devices do not support native biometric authentication"))
    }

    override fun authenticate(cryptoObject: BiometricAuth.Crypto, title: CharSequence,
                              subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Single<BiometricAuth.Crypto> {
        return Single.error(NotImplementedError("Pre Android 23 devices do not support native biometric authentication"))
    }

}