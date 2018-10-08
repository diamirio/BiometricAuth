package com.tailoredapps.biometricauth.delegate.pie

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.support.annotation.RestrictTo
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.BiometricAuthenticationCancelledException
import com.tailoredapps.biometricauth.BiometricAuthenticationException
import com.tailoredapps.biometricauth.delegate.AuthenticationEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.FlowableEmitter
import io.reactivex.Single
import io.reactivex.rxkotlin.Flowables
import java.util.concurrent.Executor

@TargetApi(28)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PieBiometricAuth(private val context: Context) : BiometricAuth {

    private val fingerprintManagerCompat: FingerprintManagerCompat = FingerprintManagerCompat.from(context)

    override val hasFingerprintHardware: Boolean
        get() = fingerprintManagerCompat.isHardwareDetected

    override val hasFingerprintsEnrolled: Boolean
        get() = fingerprintManagerCompat.hasEnrolledFingerprints()


    override fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Completable {
        return Flowables.create<AuthenticationEvent>(BackpressureStrategy.LATEST) { emitter ->
            val executor = Executor { it.run() }

            val cancellationSignal = CancellationSignal()
            emitter.setCancellable { cancellationSignal.cancel() }

            val biometricPrompt = BiometricPrompt.Builder(context).apply {
                setTitle(title)
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
                setNegativeButton(
                        negativeButtonText,
                        executor,
                        DialogInterface.OnClickListener { _, _ ->
                            cancellationSignal.cancel()
                            emitter.onError(BiometricAuthenticationCancelledException())
                        }
                )
            }.build()

            biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    getAuthenticationCallbackForFlowableEmitter(emitter)
            )
        }
                .filter { event -> event.type == AuthenticationEvent.Type.SUCCESS || event.type == AuthenticationEvent.Type.ERROR }
                .firstOrError()
                .onErrorResumeNext { throwable ->
                    if (throwable is NoSuchElementException) {
                        Single.error(BiometricAuthenticationCancelledException())
                    } else {
                        Single.error(throwable)
                    }
                }
                .flatMapCompletable { event ->
                    if (event.type == AuthenticationEvent.Type.SUCCESS) {
                        Completable.complete()
                    } else {
                        Completable.error(BiometricAuthenticationException(
                                errorMessageId = event.messageId ?: 0,
                                errorString = event.string ?: ""
                        ))
                    }
                }
    }


    private fun getAuthenticationCallbackForFlowableEmitter(emitter: FlowableEmitter<AuthenticationEvent>): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == 10) {   //"Fingerprint operation cancelled by the user."
                    emitter.onError(BiometricAuthenticationCancelledException())
                } else {
                    emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.ERROR, errorCode, errString))
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.HELP, helpCode, helpString))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.SUCCESS))
            }

            override fun onAuthenticationFailed() {
                emitter.onNext(AuthenticationEvent(AuthenticationEvent.Type.FAILED))
            }
        }
    }

}