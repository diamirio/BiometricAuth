package com.tailoredapps.biometricauth

import com.tailoredapps.biometricauth.delegate.AuthenticationEvent

/**
 * Constant fields which may be present in [AuthenticationEvent.Error] or [AuthenticationEvent.Help]
 * callbacks as `messageId` (or in further perspective the [BiometricConstants.Error] fields as in
 * [BiometricAuthenticationException]).
 *
 * Source:
 * * All fields prefixed `BIOMETRIC_`: android.hardware.biometrics.BiometricConstants
 * * All fields prefixed `FINGERPRINT_`: android.hardware.fingerprint.FingerprintManager.java ->
 * these fields have shown to be the same (or bette: equal values, but less fields than) the
 * `BIOMETRIC_` constants, why only those are used.
 *
 * (not referencing directly, as classes are hidden.)
 */
object BiometricConstants {

    /**
     * Error messages from biometric hardware during initialization, enrollment, authentication or
     * removal.
     *
     * Those values may be present as [AuthenticationEvent.Error.messageId], and can be checked for
     * further logic.
     *
     * However, the [AuthenticationEvent.Error.messageId] may contain other, vendor-specific values
     * not listed here (so this list is not to be seen exhaustive for the
     * [AuthenticationEvent.Error.messageId]).
     */
    object Error {

        /**
         * The hardware is unavailable. Try again later.
         *
         * Originally `BIOMETRIC_ERROR_HW_UNAVAILABLE` / `FINGERPRINT_ERROR_HW_UNAVAILABLE`
         */
        const val HW_UNAVAILABLE = 1

        /**
         * Error state returned when the sensor was unable to process the current image.
         *
         * Originally `BIOMETRIC_ERROR_UNABLE_TO_PROCESS` / `FINGERPRINT_ERROR_UNABLE_TO_PROCESS`
         */
        const val UNABLE_TO_PROCESS = 2

        /**
         * Error state returned when the current request has been running too long. This is intended to
         * prevent programs from waiting for the biometric sensor indefinitely. The timeout is platform
         * and sensor-specific, but is generally on the order of 30 seconds.
         *
         * Originally `BIOMETRIC_ERROR_TIMEOUT` / `FINGERPRINT_ERROR_TIMEOUT`
         */
        const val TIMEOUT = 3

        /**
         * Error state returned for operations like enrollment; the operation cannot be completed
         * because there's not enough storage remaining to complete the operation.
         *
         * Originally `BIOMETRIC_ERROR_NO_SPACE` / `FINGERPRINT_ERROR_NO_SPACE`
         */
        const val NO_SPACE = 4

        /**
         * The operation was canceled because the biometric sensor is unavailable. For example, this may
         * happen when the user is switched, the device is locked or another pending operation prevents
         * or disables it.
         *
         * Originally `BIOMETRIC_ERROR_CANCELED` / `FINGERPRINT_ERROR_CANCELED`
         */
        const val CANCELED = 5

        /**
         * The `BiometricManager.remove` call failed. Typically this will happen when the provided
         * biometric id was incorrect.
         *
         * Originally `BIOMETRIC_ERROR_UNABLE_TO_REMOVE` / `FINGERPRINT_ERROR_UNABLE_TO_REMOVE`
         */
        const val UNABLE_TO_REMOVE = 6

        /**
         * The operation was canceled because the API is locked out due to too many attempts.
         * This occurs after 5 failed attempts, and lasts for 30 seconds.
         *
         * Originally `BIOMETRIC_ERROR_LOCKOUT` / `FINGERPRINT_ERROR_LOCKOUT`
         */
        const val LOCKOUT = 7

        /**
         * The operation was canceled because BIOMETRIC_ERROR_LOCKOUT occurred too many times.
         * Biometric authentication is disabled until the user unlocks with strong authentication
         * (PIN/Pattern/Password)
         *
         * Originally `BIOMETRIC_ERROR_LOCKOUT_PERMANENT` / not set in FingerprintManager.
         */
        const val LOCKOUT_PERMANENT = 9

        /**
         * The user canceled the operation. Upon receiving this, applications should use alternate
         * authentication (e.g. a password). The application should also provide the means to return to
         * biometric authentication, such as a "use biometric" button.
         *
         * Originally `BIOMETRIC_ERROR_USER_CANCELED` / not set in FingerprintManager.
         */
        const val USER_CANCELED = 10

        /**
         * The user does not have any biometrics enrolled.
         *
         * Originally `BIOMETRIC_ERROR_NO_BIOMETRICS` / not set in FingerprintManager.
         */
        const val NO_BIOMETRICS = 11

        /**
         * The device does not have a biometric sensor.
         *
         * Originally `BIOMETRIC_ERROR_HW_NOT_PRESENT` / not set in FingerprintManager.
         */
        const val HW_NOT_PRESENT = 12

    }


    /**
     * Image acquisition messages.
     *
     * Those values may be present as [AuthenticationEvent.Help.messageId], and can be checked for
     * further logic.
     *
     * However, the [AuthenticationEvent.Help.messageId] may contain other, vendor-specific values
     * not listed here (so this list is not to be seen exhaustive for the
     * [AuthenticationEvent.Help.messageId]).
     */
    object Help {

        /**
         * The image acquired was good.
         *
         * Originally `BIOMETRIC_ACQUIRED_GOOD` / `FINGERPRINT_ACQUIRED_GOOD`
         */
        const val ACQUIRED_GOOD = 0

        /**
         * Only a partial biometric image was detected. During enrollment, the user should be informed
         * on what needs to happen to resolve this problem, e.g. "press firmly on sensor." (for
         * fingerprint)
         *
         * Originally `BIOMETRIC_ACQUIRED_PARTIAL` / `FINGERPRINT_ACQUIRED_PARTIAL`
         */
        const val ACQUIRED_PARTIAL = 1

        /**
         * The biometric image was too noisy to process due to a detected condition or a possibly dirty
         * sensor (See [ACQUIRED_IMAGER_DIRTY]).
         *
         * Originally `BIOMETRIC_ACQUIRED_INSUFFICIENT` / `FINGERPRINT_ACQUIRED_INSUFFICIENT`
         */
        const val ACQUIRED_INSUFFICIENT = 2

        /**
         * The biometric image was too noisy due to suspected or detected dirt on the sensor.  For
         * example, it's reasonable return this after multiple [.BIOMETRIC_ACQUIRED_INSUFFICIENT]
         * or actual detection of dirt on the sensor (stuck pixels, swaths, etc.). The user is expected
         * to take action to clean the sensor when this is returned.
         *
         * Originally `BIOMETRIC_ACQUIRED_IMAGER_DIRTY` / `FINGERPRINT_ACQUIRED_IMAGER_DIRTY`
         */
        const val ACQUIRED_IMAGER_DIRTY = 3

        /**
         * The biometric image was unreadable due to lack of motion.
         *
         * Originally `BIOMETRIC_ACQUIRED_TOO_SLOW` / `FINGERPRINT_ACQUIRED_TOO_SLOW`
         */
        const val ACQUIRED_TOO_SLOW = 4

        /**
         * The biometric image was incomplete due to quick motion. For example, this could also happen
         * if the user moved during acquisition. The user should be asked to repeat the operation more
         * slowly.
         *
         * Originally `BIOMETRIC_ACQUIRED_TOO_FAST` / `FINGERPRINT_ACQUIRED_TOO_FAST`
         */
        const val ACQUIRED_TOO_FAST = 5

    }

}