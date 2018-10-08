package com.tailoredapps.biometricauth

import android.os.Build
import android.support.v7.app.AppCompatActivity
import com.tailoredapps.biometricauth.delegate.legacy.LegacyBiometricAuth
import com.tailoredapps.biometricauth.delegate.marshmallow.MarshmallowBiometricAuth
import com.tailoredapps.biometricauth.delegate.pie.PieBiometricAuth
import io.reactivex.Completable
import io.reactivex.annotations.CheckReturnValue

interface BiometricAuth {

    companion object {

        /**
         * Create a [BiometricAuth] instance targeting the devices' version-code (taken from [Build.VERSION.SDK_INT]).
         *
         * @param activity the current activity (as [AppCompatActivity]) which requests the authentication.
         *
         * @return an instance of [BiometricAuth], which targets the devices' SDK version.
         */
        fun create(activity: AppCompatActivity): BiometricAuth {
            val versionCode = Build.VERSION.SDK_INT
            return when {
                versionCode >= Build.VERSION_CODES.P -> PieBiometricAuth(activity)
                versionCode >= Build.VERSION_CODES.M -> MarshmallowBiometricAuth(activity)
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

}