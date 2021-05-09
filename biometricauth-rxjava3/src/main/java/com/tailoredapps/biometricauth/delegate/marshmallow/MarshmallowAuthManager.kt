package com.tailoredapps.biometricauth.delegate.marshmallow

import android.annotation.TargetApi
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.delegate.AuthenticationEvent
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable

/**
 * Manager encapsulating the [FingerprintManagerCompat] library in the _style_ of the
 * [com.tailoredapps.biometricauth.BiometricAuth] interface, for easyness of use.
 */
@TargetApi(23)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MarshmallowAuthManager(context: Context) {

    private val fingerprintManagerCompat: FingerprintManagerCompat = FingerprintManagerCompat.from(context)

    /**
     * Whether the device provides hardware to verify fingerprints
     */
    val hasFingerprintHardware: Boolean
        get() = fingerprintManagerCompat.isHardwareDetected

    /**
     * Whether the user has enrolled fingerprint-authentication (and added fingerprints) on the
     * device.
     */
    val hasFingerprintsEnrolled: Boolean
        get() = fingerprintManagerCompat.hasEnrolledFingerprints()

    /**
     * @param cancellationSignal An (optional) [CancellationSignal] which îs passed onto the
     * [FingerprintManagerCompat] and will be cancelled on the event of the [Flowable] being
     * cancelled.
     *
     * @return a [Flowable], which starts the fingerprint-authentication flow in the background
     * using the [FingerprintManagerCompat], provides all updates as items in the
     * [Flowable] (wrapped as [AuthenticationEvent]) and honors the given [cancellationSignal].
     */
    fun authenticate(crypto: BiometricAuth.Crypto?, cancellationSignal: CancellationSignal = CancellationSignal()): Flowable<AuthenticationEvent> {
        return Flowable
                .create<AuthenticationEvent>({ emitter ->
                    emitter.setCancellable { cancellationSignal.cancel() }

                    fingerprintManagerCompat.authenticate(
                            crypto?.toCryptoObject(),
                            0,
                            cancellationSignal,
                            object : FingerprintManagerCompat.AuthenticationCallback() {
                                override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                                    //error, no further callback calls
                                    emitter.onNext(AuthenticationEvent.Error(errMsgId, errString ?: ""))
                                }

                                override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
                                    emitter.onNext(AuthenticationEvent.Help(helpMsgId, helpString ?: ""))
                                }

                                override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                                    emitter.onNext(AuthenticationEvent.Success(result?.cryptoObject?.let { BiometricAuth.Crypto(it) }))
                                }

                                override fun onAuthenticationFailed() {
                                    //fingerprint was detected successfully, but is e.g. not one of the saved ones
                                    emitter.onNext(AuthenticationEvent.Failed)
                                }
                            },
                            null
                    )
                }, BackpressureStrategy.LATEST)
    }


    private fun BiometricAuth.Crypto.toCryptoObject(): FingerprintManagerCompat.CryptoObject? {
        return when {
            this.signature != null -> FingerprintManagerCompat.CryptoObject(this.signature)
            this.cipher != null -> FingerprintManagerCompat.CryptoObject(this.cipher)
            this.mac != null -> FingerprintManagerCompat.CryptoObject(this.mac)
            else -> null
        }
    }

}
