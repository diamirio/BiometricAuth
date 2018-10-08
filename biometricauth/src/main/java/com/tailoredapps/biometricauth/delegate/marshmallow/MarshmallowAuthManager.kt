package com.tailoredapps.biometricauth.delegate.marshmallow

import android.annotation.TargetApi
import android.content.Context
import android.support.annotation.RestrictTo
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import com.tailoredapps.biometricauth.delegate.AuthenticationEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

@TargetApi(23)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MarshmallowAuthManager(context: Context) {

    private val fingerprintManagerCompat: FingerprintManagerCompat = FingerprintManagerCompat.from(context)

    val hasFingerprintHardware: Boolean
        get() = fingerprintManagerCompat.isHardwareDetected

    val hasFingerprintsEnrolled: Boolean
        get() = fingerprintManagerCompat.hasEnrolledFingerprints()

    fun authenticate(cancellationSignal: CancellationSignal = CancellationSignal()): Flowable<AuthenticationEvent> {
        return Flowables
                .create<AuthenticationEvent>(BackpressureStrategy.LATEST) { emitter ->
                    emitter.setCancellable { cancellationSignal.cancel() }

                    fingerprintManagerCompat.authenticate(
                            null,
                            0,
                            cancellationSignal,
                            object : FingerprintManagerCompat.AuthenticationCallback() {
                                override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
                                    //error, no further callback calls
                                    emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.ERROR, errMsgId, errString))
                                }

                                override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
                                    emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.HELP, helpMsgId, helpString))
                                }

                                override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
                                    emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.SUCCESS))
                                }

                                override fun onAuthenticationFailed() {
                                    //fingerprint was detected successfully, but is e.g. not one of the saved ones
                                    emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.FAILED))
                                }
                            },
                            null
                    )
                }
    }
}
