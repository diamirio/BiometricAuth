package com.tailoredapps.biometricauth.delegate.marshmallow

import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.os.CancellationSignal
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.BiometricAuthenticationCancelledException
import com.tailoredapps.biometricauth.BiometricAuthenticationException
import com.tailoredapps.biometricauth.R
import com.tailoredapps.biometricauth.delegate.AuthenticationEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.MaybeEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@TargetApi(23)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MarshmallowFingerprintDialog : BottomSheetDialogFragment() {

    companion object {
        fun create(crypto: BiometricAuth.Crypto?, title: CharSequence, subtitle: CharSequence?,
                   description: CharSequence?, negativeButtonText: CharSequence,
                   prompt: CharSequence, notRecognizedErrorText: CharSequence,
                   requestId: Int): MarshmallowFingerprintDialog {
            return MarshmallowFingerprintDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_REQUEST_ID, requestId)
                    crypto?.let { putSerializable(EXTRA_CRYPTO, it) }
                    putCharSequence(EXTRA_TITLE, title)
                    putCharSequence(EXTRA_SUBTITLE, subtitle)
                    putCharSequence(EXTRA_DESCRIPTION, description)
                    putCharSequence(EXTRA_NEGATIVE, negativeButtonText)
                    putCharSequence(EXTRA_PROMPT, prompt)
                    putCharSequence(EXTRA_NOT_RECOGNIZED, notRecognizedErrorText)
                }
            }
        }

        private const val EXTRA_REQUEST_ID = "req_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SUBTITLE = "subtitle"
        private const val EXTRA_DESCRIPTION = "description"
        private const val EXTRA_NEGATIVE = "negative"
        private const val EXTRA_PROMPT = "prompt"
        private const val EXTRA_NOT_RECOGNIZED = "not_recognized"
        private const val EXTRA_CRYPTO = "crypto"
    }

    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var description: TextView
    private lateinit var iconImageView: ImageView
    private lateinit var errorIconImageView: ImageView
    private lateinit var iconTextView: TextView
    private lateinit var cancelButton: Button

    private inline val emitter: MaybeEmitter<BiometricAuth.Crypto>?
        get() = arguments?.getInt(EXTRA_REQUEST_ID)?.let { MarshmallowBiometricAuth.getEmitter(it) }

    private lateinit var cancellationSignal: CancellationSignal
    private lateinit var authManager: MarshmallowAuthManager

    private val compositeDisposable = CompositeDisposable()

    /**
     * Contains the animation duration in milliseconds. Is set in [onCreateDialog].
     */
    private var animationTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cancellationSignal = CancellationSignal()
        authManager = MarshmallowAuthManager(context!!)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = FixedWidthInLandscapeBottomSheetDialog(440f, requireContext(), theme)
        dialog.setContentView(R.layout.dialog_fingerprint)

        val sheetView = dialog.findViewById<View>(R.id.bottom_sheet_container)?.parent as? View
        val behavior = sheetView?.let { BottomSheetBehavior.from(it) }

        animationTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        title = dialog.findViewById(R.id.title)!!
        subtitle = dialog.findViewById(R.id.subtitle)!!
        description = dialog.findViewById(R.id.description)!!
        iconImageView = dialog.findViewById(R.id.icon)!!
        errorIconImageView = dialog.findViewById(R.id.error_icon)!!
        iconTextView = dialog.findViewById(R.id.icon_text)!!
        cancelButton = dialog.findViewById(R.id.cancel)!!


        arguments?.also { arguments ->
            title.text = arguments.getCharSequence(EXTRA_TITLE)
            subtitle.setTextOrGone(arguments.getCharSequence(EXTRA_SUBTITLE))
            description.setTextOrGone(arguments.getCharSequence(EXTRA_DESCRIPTION))
            cancelButton.text = arguments.getCharSequence(EXTRA_NEGATIVE)
            iconTextView.text = arguments.getCharSequence(EXTRA_PROMPT)
        }


        cancelButton.setOnClickListener {
            errorResetTimerDisposable?.dispose()
            emitter?.onCancel()
            cancellationSignal.cancel()
            dismissAllowingStateLoss()
        }

        dialog.setOnShowListener {
            sheetView?.height?.let { height -> behavior?.peekHeight = height }
            authManager.authenticate(arguments?.getSerializable(EXTRA_CRYPTO) as? BiometricAuth.Crypto, cancellationSignal)
                    .observeOn(AndroidSchedulers.mainThread())
                    .delegateErrorOutputToViewOnNext()
                    .filter { event -> event is AuthenticationEvent.Success || event is AuthenticationEvent.Error }
                    .switchMapSingle { event ->
                        if (event is AuthenticationEvent.Error) {
                            //delay error to have the error-text shown in the bottom sheet for some while to the user
                            Single.timer(2, TimeUnit.SECONDS).map { event }
                        } else {
                            Single.just(event)
                        }
                    }
                    .firstElement()
                    .subscribe(
                            { event ->
                                if (event is AuthenticationEvent.Success) {
                                    if (event.crypto != null) {
                                        emitter?.onSuccess(event.crypto)
                                    } else {
                                        emitter?.onComplete()
                                    }
                                } else if (event is AuthenticationEvent.Error) {
                                    if (event.messageId == 5) {  //"Fingerprint operation cancelled."
                                        emitter?.onCancel()
                                    } else {
                                        emitter?.onError(BiometricAuthenticationException(
                                                errorMessageId = event.messageId,
                                                errorString = event.message
                                        ))
                                    }
                                } else {
                                    emitter?.onError(BiometricAuthenticationException(
                                            errorMessageId = 0,
                                            errorString = ""
                                    ))
                                }
                                dismissAllowingStateLoss()
                            },
                            { throwable -> emitter?.onError(throwable) }
                    )
                    .let(compositeDisposable::add)
        }

        return dialog
    }

    private fun TextView.setTextOrGone(text: CharSequence?) {
        if (text.isNullOrBlank()) {
            this.text = null
            this.visibility = View.GONE
        } else {
            this.visibility = View.VISIBLE
            this.text = text
        }
    }

    /**
     * Calls [showError] on help/error items respectively with the string emitted within the error
     */
    private fun Flowable<AuthenticationEvent>.delegateErrorOutputToViewOnNext(): Flowable<AuthenticationEvent> {
        return this.doOnNext { event ->
            when (event) {
                is AuthenticationEvent.Error -> {
                    showError(event.message)
                }
                is AuthenticationEvent.Help -> {
                    showError(event.message)
                }
                is AuthenticationEvent.Failed -> {
                    showError(arguments?.getCharSequence(EXTRA_NOT_RECOGNIZED) ?: "Not recognized")
                }
                is AuthenticationEvent.Success -> {
                }
            }
        }
    }

    /**
     * @return A new [AnimationSet] with a shared [DecelerateInterpolator] and fillAfter enabled.
     */
    private fun getAnimationSet(): AnimationSet {
        return AnimationSet(true).apply {
            interpolator = DecelerateInterpolator()
            fillAfter = true
            isFillEnabled = true
        }
    }

    /**
     * @return A new [RotateAnimation] which rotates for [animationTime] 360 degrees either [forward] or backward.
     */
    private fun getRotateAnimation(forward: Boolean): RotateAnimation {
        return RotateAnimation(
                if (forward) 0f else 360f, if (forward) 360f else 0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = animationTime
            fillAfter = true
        }
    }

    /**
     * @return a new [AlphaAnimation] which translates from alpha 1 to 0 if [disappearing] is true, or from 0 to 1 if [disappearing] is false.
     */
    private fun getAlphaAnimation(disappearing: Boolean): AlphaAnimation {
        return AlphaAnimation(if (disappearing) 1f else 0f, if (disappearing) 0f else 1f).apply {
            duration = animationTime
            fillAfter = true
        }
    }

    /**
     * Animates the icons:
     * * if [reverse] is false (default):
     *    * Fades the alpha of the fingerprint-icon from 1 to 0
     *    * Fades the alpha of the error-icon from 0 to 1
     *    * Rotates both the fingerprint and error icon 360 degrees
     * * if [reverse] is true:
     *    * Fades the alpha of the fingerprint-icon from 0 to 1
     *    * Fades the alpha of the error-icon from 1 to 0
     *    * Rotates both the fingerprint and error icon -360 degrees
     */
    private fun transitionToError(reverse: Boolean = false) {
        val fingerprintIconAnimationSet = getAnimationSet().apply {
            addAnimation(getRotateAnimation(reverse.not()))
            addAnimation(getAlphaAnimation(reverse.not()))
        }
        iconImageView.startAnimation(fingerprintIconAnimationSet)


        val errorIconAnimationSet = getAnimationSet().apply {
            addAnimation(getRotateAnimation(reverse.not()))
            addAnimation(getAlphaAnimation(reverse))
        }
        errorIconImageView.startAnimation(errorIconAnimationSet)

        if (reverse.not()) {
            errorIconImageView.visibility = View.VISIBLE
        }

        errorIconImageView.visibility = View.VISIBLE
    }

    /**
     * Only rotates the error icon for 360 degrees, which should be done if a new error occurs while
     * a previous error is still shown.
     */
    private fun rotateErrorIcon() {
        val errorIconAnimationSet = getAnimationSet().apply {
            addAnimation(getRotateAnimation(true))
        }
        errorIconImageView.startAnimation(errorIconAnimationSet)
    }

    /**
     * [Disposable] for the timer which starts when an error is shown, and clears the error being
     * shown after the given time.
     */
    private var errorResetTimerDisposable: Disposable? = null

    private fun showError(message: CharSequence) {
        // Check whether there is already an error still showing,
        // as then only the error-icon should be rotated.
        val isErrorStillShowing = errorResetTimerDisposable?.isDisposed?.not() ?: false

        // dispose the errorResetTimer, to not clear any error still shown
        // (or clear the new error just after it has been set)
        errorResetTimerDisposable?.dispose()
        errorResetTimerDisposable = null

        iconTextView.text = message
        iconTextView.setTextColor(ContextCompat.getColor(iconTextView.context, R.color.biometric_icon_text_color_error))

        if (isErrorStillShowing) {
            rotateErrorIcon()
        } else {
            transitionToError()
        }

        errorResetTimerDisposable = Completable.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            //Reset the error message, revert the error animation
                            iconTextView.text = arguments?.getCharSequence(EXTRA_PROMPT) ?: "Touch the fingerprint sensor"
                            iconTextView.setTextColor(ContextCompat.getColor(iconTextView.context, R.color.biometric_icon_text_color))
                            transitionToError(reverse = true)
                        },
                        { Log.i("BiometricAuth", "Timer error", it) }
                )
                // Also add this disposable to the compositeDisposable,
                // as this e.g. is the only one being cleared at onDestroy)
                .also { compositeDisposable.add(it) }
    }

    override fun onCancel(dialog: DialogInterface) {
        errorResetTimerDisposable?.dispose()
        cancellationSignal.cancel()
        emitter?.onCancel()
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        errorResetTimerDisposable?.dispose()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun MaybeEmitter<BiometricAuth.Crypto>.onCancel() = this.onError(BiometricAuthenticationCancelledException())


    /**
     * BottomSheetDialog, which sets the view to a fixed width if its shown in landscape mode.
     */
    internal class FixedWidthInLandscapeBottomSheetDialog(
            private val fixedLandscapeWidthInDp: Float,
            context: Context,
            @StyleRes theme: Int
    ) : BottomSheetDialog(context, theme) {

        private inline val Float.dpAsPixel: Int
            get() = (this * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window?.setLayout(fixedLandscapeWidthInDp.dpAsPixel, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }

    }

}