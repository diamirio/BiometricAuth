package com.tailoredapps.biometricauth

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import com.tailoredapps.biometricauth.delegate.androidxlegacy.AndroidXBiometricAuth
import com.tailoredapps.biometricauth.delegate.legacy.LegacyBiometricAuth
import com.tailoredapps.biometricauth.delegate.marshmallow.MarshmallowBiometricAuth
import com.tailoredapps.biometricauth.delegate.pie.PieBiometricAuth
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import java.io.Serializable
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * An interface to authenticate a user using biometrics.
 *
 * The process of authentication should as follows:
 *
 * * Check whether the device supports fingerprint authentication by verifying that
 * [hasFingerprintHardware] returns `true`.
 * * Check whether the user on the device has enrolled fingerprint authentication and has at least
 * 1 fingerprint added by verifying that [hasFingerprintsEnrolled] returns `true`.
 * * Subscribe to the [Completable] returned by [authenticate]. If it completed, the authentication
 * was successful. If an error was emitted, check the error (and the JavaDoc of [authenticate]
 * for an explanation of the different errors).
 */
interface BiometricAuth {

    companion object {

        /**
         * **Deprecated**: Use the method with the optional Boolean parameter 'useAndroidXBiometricPrompt'.
         */
        @Deprecated(
                message = "This method is solely kept for binary compatibility reasons. Use create() with two parameters (FragmentActivity, Boolean) instead.",
                replaceWith = ReplaceWith("BiometricAuth.create(activity, useAndroidXBiometricPrompt = false)")
        )
        fun create(activity: AppCompatActivity): BiometricAuth {
            return create(activity = activity, useAndroidXBiometricPrompt = false)
        }

        /**
         * Create a [BiometricAuth] instance targeting the devices' version-code (taken from [Build.VERSION.SDK_INT]).
         *
         * @param activity the current activity (as [AppCompatActivity]) which requests the authentication.
         * @param useAndroidXBiometricPrompt Whether on pre android 10 devices the [AndroidXBiometricAuth]
         * should be used in favor of the custom implementation [MarshmallowBiometricAuth].
         * The main difference here is, that the backport is not necessarily shown as
         * bottom-sheet-dialog, whereas the [MarshmallowBiometricAuth] is always shown as a
         * bottom-sheet-dialog.
         *
         * @return an instance of [BiometricAuth], which targets the devices' SDK version.
         */
        fun create(activity: FragmentActivity, useAndroidXBiometricPrompt: Boolean = false): BiometricAuth {
            val versionCode = Build.VERSION.SDK_INT
            return when {
                versionCode >= Build.VERSION_CODES.P -> PieBiometricAuth(activity)
                versionCode >= Build.VERSION_CODES.M -> if (useAndroidXBiometricPrompt) {
                    AndroidXBiometricAuth(activity)
                } else {
                    MarshmallowBiometricAuth(activity)
                }
                else -> LegacyBiometricAuth()
            }
        }

    }


    /**
     * Returns whether the devices supports fingerprint authentication (i.e. has fingerprint hardware).
     *
     * This will return false on all devices < *Android SDK 23*, on *Android SDK 23* and above it will actually
     * check if the devices has fingerprint hardware.
     */
    val hasFingerprintHardware: Boolean


    /**
     * Returns whether the user has enrolled any fingerprints on the given device.
     *
     * This will return false on all devices < *Android SDK 23*, on *Android SDK 23* and above it will
     * actually check whether the user has enrolled any fingerprints
     */
    val hasFingerprintsEnrolled: Boolean


    /**
     * Actually starts authentication with showing a BottomSheet UI.
     *
     * Note that you need to check first if the device supports
     * fingerprinting [hasFingerprintHardware] and the user
     * has enrolled fingerprints on this device [hasFingerprintsEnrolled].
     *
     * This will immediately return a [Completable.error] if called on a device running a lower SDK than 23.
     *
     * If you want to pass a crypro-object, see the [authenticate]-method returning a [Maybe].
     *
     * @param title The title of the BottomSheet shown.
     * @param subtitle The subtitle of the BottomSheet shown, optional.
     * @param description The description of the BottomSheet shown, optional.
     * @param prompt The prompt text shown below the fingerprint icon in the bottomSheet on Android SDK 23..27 devices. (will default to the english default text, if not set)
     * @param notRecognizedErrorText The text shown below the alert icon in the BottomSheet on Android SDK 23..27 devices which indicates that the fingerprint has not been recognized. (will default to the english default text, if not set)
     * @param negativeButtonText The cancel button text of the BottomSheet shown.
     *
     * @return A [Completable], which will either:
     * * complete (which indicates that the authentication has been successful)
     * * emit an error, which is either:
     *    * a [BiometricAuthenticationCancelledException] (signals that the authentication has been cancelled),
     *    * a [BiometricAuthenticationException] (signals that the authentication has not been successful), or
     *    * any other _unexpected_ error during authentication. Note, that the help messages / retries are handled internally.
     */
    @CheckReturnValue
    fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                     prompt: CharSequence = "Touch the fingerprint sensor",
                     notRecognizedErrorText: CharSequence = "Not recognized",
                     negativeButtonText: CharSequence): Completable

    /**
     * Actually starts authentication with showing a BottomSheet UI.
     *
     * Note that you need to check first if the device supports
     * fingerprinting [hasFingerprintHardware] and the user
     * has enrolled fingerprints on this device [hasFingerprintsEnrolled].
     *
     * This will immediately return a [Completable.error] if called on a device running a lower SDK than 23.
     *
     * If you don't need/want to pass a crypto-object, use the method returning a [Completable].
     *
     * @param cryptoObject The Crypto-Object to pass to the fingerprint authentication.
     * @param title The title of the BottomSheet shown.
     * @param subtitle The subtitle of the BottomSheet shown, optional.
     * @param description The description of the BottomSheet shown, optional.
     * @param prompt The prompt text shown below the fingerprint icon in the bottomSheet on Android SDK 23..27 devices. (will default to the english default text, if not set)
     * @param notRecognizedErrorText The text shown below the alert icon in the BottomSheet on Android SDK 23..27 devices which indicates that the fingerprint has not been recognized. (will default to the english default text, if not set)
     * @param negativeButtonText The cancel button text of the BottomSheet shown.
     *
     * @return A [Completable], which will either:
     * * complete (which indicates that the authentication has been successful)
     * * emit an error, which is either:
     *    * a [BiometricAuthenticationCancelledException] (signals that the authentication has been cancelled),
     *    * a [BiometricAuthenticationException] (signals that the authentication has not been successful), or
     *    * any other _unexpected_ error during authentication. Note, that the help messages / retries are handled internally.
     */
    @CheckReturnValue
    fun authenticate(cryptoObject: Crypto, title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                     prompt: CharSequence = "Touch the fingerprint sensor",
                     notRecognizedErrorText: CharSequence = "Not recognized",
                     negativeButtonText: CharSequence): Single<Crypto>


    /**
     * Class to hold the optional, chosen way of sending a [Signature], [Cipher] or [Mac] to the
     * authentication algorithm, which will get sent back at the success, to verify that the
     * success-value has been emitted for the given request.
     *
     * As per the native API implementation, its only possible to pass on of the three options onto
     * the algorithm, which represents the three public constructors of this class.
     *
     * @param signature A signature to pass on to the biometric authentication
     * @param cipher A cipher to pass on to the biometric authentication
     * @param mac A mac to pass on to the biometric authentication
     */
    data class Crypto internal constructor(
            val signature: Signature?,
            val cipher: Cipher?,
            val mac: Mac?
    ) : Serializable {

        /**
         * Wrap a [Signature] to be sent to the biometric authentication
         */
        @Suppress("unused")
        constructor(signature: Signature) : this(signature, null, null)

        /**
         * Wrap a [Cipher] to be sent to the biometric authentication
         */
        @Suppress("unused")
        constructor(cipher: Cipher) : this(null, cipher, null)

        /**
         * Wrap a [Mac] to be sent to the biometric authentication
         */
        @Suppress("unused")
        constructor(mac: Mac) : this(null, null, mac)


        /**
         * Internal constructor to map a [PieBiometricAuth]-response object into this wrapper object
         */
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(biometricPromptCryptoObject: BiometricPrompt.CryptoObject) : this(
                biometricPromptCryptoObject.signature,
                biometricPromptCryptoObject.cipher,
                biometricPromptCryptoObject.mac
        )

        /**
         * Internal constructor to map a [MarshmallowBiometricAuth]-response object into this wrapper object
         */
        @RequiresApi(23)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(fingerprintManagerCompatCryptoObject: FingerprintManagerCompat.CryptoObject) : this(
                fingerprintManagerCompatCryptoObject.signature,
                fingerprintManagerCompatCryptoObject.cipher,
                fingerprintManagerCompatCryptoObject.mac
        )

        /**
         * Internal constructor to map a [AndroidXBiometricAuth]-response object into this wrapper object
         */
        @RequiresApi(23)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(fingerprintManagerCompatCryptoObject: androidx.biometric.BiometricPrompt.CryptoObject) : this(
                fingerprintManagerCompatCryptoObject.signature,
                fingerprintManagerCompatCryptoObject.cipher,
                fingerprintManagerCompatCryptoObject.mac
        )
    }

}