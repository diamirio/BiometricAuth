package com.tailoredapps.biometricauth.delegate.pie

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.BiometricAuthenticationCancelledException
import com.tailoredapps.biometricauth.BiometricAuthenticationException
import com.tailoredapps.biometricauth.BiometricConstants
import com.tailoredapps.biometricauth.delegate.AuthenticationEvent
import io.reactivex.*
import java.util.concurrent.Executor

@TargetApi(28)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PieBiometricAuth(private val context: Context) : BiometricAuth {

    private val fingerprintManagerCompat: FingerprintManagerCompat = FingerprintManagerCompat.from(context)

    override val hasFingerprintHardware: Boolean
        get() = fingerprintManagerCompat.isHardwareDetected

    override val hasFingerprintsEnrolled: Boolean
        get() = fingerprintManagerCompat.hasEnrolledFingerprints()


    private fun internalAuthenticate(cryptoObject: BiometricAuth.Crypto?, title: CharSequence,
                                     subtitle: CharSequence?, description: CharSequence?,
                                     negativeButtonText: CharSequence): Maybe<BiometricAuth.Crypto> {
        return Flowable
                .create<AuthenticationEvent>({ emitter ->
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

                    val convertedCryptoObject = cryptoObject?.toCryptoObject()
                    if (convertedCryptoObject != null) {
                        biometricPrompt.authenticate(
                                convertedCryptoObject,
                                cancellationSignal,
                                executor,
                                getAuthenticationCallbackForFlowableEmitter(emitter)
                        )
                    } else {
                        biometricPrompt.authenticate(
                                cancellationSignal,
                                executor,
                                getAuthenticationCallbackForFlowableEmitter(emitter)
                        )
                    }
                }, BackpressureStrategy.LATEST)
                .filter { event -> event is AuthenticationEvent.Success || event is AuthenticationEvent.Error }
                .firstOrError()
                .onErrorResumeNext { throwable ->
                    if (throwable is NoSuchElementException) {
                        Single.error(BiometricAuthenticationCancelledException())
                    } else {
                        Single.error(throwable)
                    }
                }
                .flatMapMaybe { event ->
                    when (event) {
                        is AuthenticationEvent.Success -> {
                            if (event.crypto != null) {
                                Maybe.just(event.crypto)
                            } else {
                                Maybe.empty()
                            }
                        }
                        is AuthenticationEvent.Error -> Maybe.error(BiometricAuthenticationException(
                                errorMessageId = event.messageId, errorString = event.message
                        ))
                        else -> Maybe.error(BiometricAuthenticationException(
                                errorMessageId = 0,
                                errorString = ""
                        ))
                    }
                }
    }

    override fun authenticate(cryptoObject: BiometricAuth.Crypto, title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Single<BiometricAuth.Crypto> {
        return internalAuthenticate(cryptoObject, title, subtitle, description, negativeButtonText).toSingle()
    }


    override fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Completable {
        return internalAuthenticate(null, title, subtitle, description, negativeButtonText).ignoreElement()
    }


    private fun BiometricAuth.Crypto.toCryptoObject(): BiometricPrompt.CryptoObject? {
        return when {
            this.signature != null -> BiometricPrompt.CryptoObject(this.signature)
            this.cipher != null -> BiometricPrompt.CryptoObject(this.cipher)
            this.mac != null -> BiometricPrompt.CryptoObject(this.mac)
            else -> null
        }
    }

    private fun getAuthenticationCallbackForFlowableEmitter(emitter: FlowableEmitter<AuthenticationEvent>): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                if (errorCode == BiometricConstants.Error.USER_CANCELED) {
                    //on the event of user cancelled, do not propagate the original error
                    emitter.onError(BiometricAuthenticationCancelledException())
                } else {
                    emitter.onNext(AuthenticationEvent.Error(errorCode, errString ?: ""))
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                emitter.onNext(AuthenticationEvent.Help(helpCode, helpString ?: ""))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                emitter.onNext(AuthenticationEvent.Success(result?.cryptoObject?.let { BiometricAuth.Crypto(it) }))
            }

            override fun onAuthenticationFailed() {
                emitter.onNext(AuthenticationEvent.Failed)
            }
        }
    }

}