package com.tailoredapps.biometricauth.delegate.marshmallow

import android.annotation.TargetApi
import android.support.annotation.RestrictTo
import android.support.v7.app.AppCompatActivity
import com.tailoredapps.biometricauth.BiometricAuth
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import java.lang.ref.WeakReference
import java.security.SecureRandom

@TargetApi(23)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MarshmallowBiometricAuth(private val activity: AppCompatActivity) : BiometricAuth {

    companion object {
        private val random = SecureRandom()

        /**
         * Maps the requestCode to a CompletableEmitter.
         * This is needed for OrientationChanges, etc., where the dialog is recreated, but the emitter stays the same.
         *
         * It is stored as a [WeakReference], as the host activity/fragment could have been
         * destroyed, but the map would still hold a reference to the emitter.
         */
        private val emitterMap: MutableMap<Int, WeakReference<CompletableEmitter>> = mutableMapOf()

        /**
         * Adds the given [emitter] to the [emitterMap].
         *
         * Returns a new `requestId`, under which the given [emitter] is stored in the [emitterMap].
         *
         * @param emitter The [CompletableEmitter] to store in the [emitterMap]
         * @return the generated id (`requestId`) which is the key to retrieve the stored [emitter] using [getEmitter] or [removeEmitter].
         */
        internal fun addEmitter(emitter: CompletableEmitter): Int {
            return generatedRequestId.also {
                emitterMap[it] = WeakReference(emitter)
            }
        }

        /**
         * @return the [CompletableEmitter] set using [addEmitter] with the given [requestId], or null.
         */
        internal fun getEmitter(requestId: Int): CompletableEmitter? = emitterMap[requestId]?.get()

        /**
         * Removes an emitter stored under the given [requestId] in the [emitterMap] if set.
         *
         * @param requestId the id returned by [addEmitter] for the emitter to destroy
         */
        internal fun removeEmitter(requestId: Int) {
            emitterMap.remove(requestId)
        }

        /**
         * Creates a new unique `requestId`, which is not yet present in the [emitterMap].
         */
        private val generatedRequestId: Int
            get() {
                var randomInt: Int
                do {
                    randomInt = random.nextInt()
                } while (emitterMap.containsKey(randomInt))
                return randomInt
            }
    }


    private val authManager = MarshmallowAuthManager(activity)

    override val hasFingerprintHardware: Boolean
        get() = authManager.hasFingerprintHardware

    override val hasFingerprintsEnrolled: Boolean
        get() = authManager.hasFingerprintsEnrolled

    override fun authenticate(title: CharSequence, subtitle: CharSequence?, description: CharSequence?,
                              prompt: CharSequence, notRecognizedErrorText: CharSequence,
                              negativeButtonText: CharSequence): Completable {
        var requestId = 0
        return Completable.create { emitter ->
            requestId = addEmitter(emitter)
            val dialog = MarshmallowFingerprintDialog.create(title, subtitle, description, negativeButtonText, prompt, notRecognizedErrorText, requestId)
            dialog.show(activity.supportFragmentManager, "MarshmallowDialog")
        }.doAfterTerminate {
            removeEmitter(requestId)
        }
    }

}