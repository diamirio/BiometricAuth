package com.tailoredapps.biometricauth.delegate

import androidx.annotation.RestrictTo
import com.tailoredapps.biometricauth.BiometricAuth


/**
 * Events emitted by the biometric-auth-delegates (pie or marshmallow implementation) for further
 * processing.
 *
 * This class / events / subclasses are not directly exposed to the API, they are mapped to other
 * classes/exceptions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
sealed class AuthenticationEvent {

    /**
     * Signals that an unrecoverable error occurred (either Marshmallows or Pie's
     * `onAuthenticationError` has been called).
     *
     * This signals, that there has been an unrecoverable error while authenticating, which has
     * the effect that the given authentication process has been cancelled
     * (no further events will occur).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class Error(
            /**
             * The id of the message, if set.
             *
             * See [com.tailoredapps.biometricauth.BiometricConstants.Error] for possible values.
             */
            val messageId: Int,
            val message: CharSequence
    ) : AuthenticationEvent()

    /**
     * Signals that a recoverable error has occurred (either Marshmallows or Pie's
     * `onAuthenticationHelp` has been called).
     *
     * The message associated with this event provides a help for the user for what went wrong
     * and how the user may fix the process.
     *
     * An exemplary message is e.g. "Sensor dirty, please clean it."
     *
     * This event does **not** signal the end of the authentication process, more events may
     * follow.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class Help(
            /**
             * The id of the message, if set.
             *
             * See [com.tailoredapps.biometricauth.BiometricConstants.Help] for possible values.
             */
            val messageId: Int,
            val message: CharSequence
    ) : AuthenticationEvent()

    /**
     * Signals that the authentication has been successful (either Marshmallows or Pie's
     * `onAuthenticationSucceeded` has been called).
     *
     * This event signals the end of the process, as the authentication has succeeded.
     * No further events will be sent.
     *
     * @param crypto The crypto-object, if set in the request
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class Success(
            val crypto: BiometricAuth.Crypto?
    ) : AuthenticationEvent()

    /**
     * Signals that e.g. a fingerprint has been successfully identified, but was not recognized
     * (e.g. an unauthorized finger was used).
     *
     * This does **not** signal the event of the process, further events may follow.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    object Failed : AuthenticationEvent()

}
