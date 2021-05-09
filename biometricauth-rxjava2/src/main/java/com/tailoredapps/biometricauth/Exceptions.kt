package com.tailoredapps.biometricauth

/**
 * Signals an unrecoverable error during biometric authentication.
 * This is for example the error, if too many attempts with an invalid fingerprint are performed.
 *
 * The BottomSheet will show the provided [errorString] and will after some time close the dialog.
 *
 * You might want to check the [errorMessageId] with the values set in [BiometricConstants.Error] object,
 * which represent the values which may be set there.
 * Note, however, that the list of fields in the [BiometricConstants.Error] object is not exhaustive, which
 * means that other (vendor-specific) values may be present in [errorMessageId].
 *
 * @param errorMessageId The messageId of the error
 * @param errorString The (localized) string of the error
 */
class BiometricAuthenticationException(val errorMessageId: Int, val errorString: CharSequence) : RuntimeException(errorString.toString())


/**
 * Signals that the biometric authentication has been cancelled (most likely by the user).
 *
 * The BottomSheet has been closed, there is no need to show the user explicitly that the operation
 * has been cancelled, it has either been shown to him, or the user has triggered the action.
 */
class BiometricAuthenticationCancelledException : RuntimeException("The Authentication has been cancelled")