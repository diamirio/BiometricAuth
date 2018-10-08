package com.tailoredapps.biometricsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.BiometricAuthenticationCancelledException
import com.tailoredapps.biometricauth.BiometricAuthenticationException
import io.reactivex.android.schedulers.AndroidSchedulers

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    private val biometricAuth: BiometricAuth by lazy { BiometricAuth.create(this) }

    private val button: Button by lazy { findViewById<Button>(R.id.button) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            biometricAuth.testAuthenticate()
        }
    }

    private fun BiometricAuth.testAuthenticate() {
        if (this.hasFingerprintHardware.not()) {
            Toast.makeText(this@MainActivity, "Devices provides no fingerprint hardware", Toast.LENGTH_SHORT).show()
        } else if (this.hasFingerprintsEnrolled.not()) {
            Toast.makeText(this@MainActivity, "No fingerprints enrolled", Toast.LENGTH_SHORT).show()
        } else {
            this.authenticate(
                    title = "Please authenticate",
                    subtitle = "Using 'Awesome Feature' requires your authentication.",
                    description = "'Awesome Feature' exposes data private to you, which is why you need to authenticate.",
                    negativeButtonText = "Cancel",
                    prompt = "Touch the fingerprint sensor",
                    notRecognizedErrorText = "Not recognized"
            )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                Toast.makeText(this@MainActivity, "Success!", Toast.LENGTH_SHORT).show()
                                Log.d(LOG_TAG, "onSuccess()")
                            },
                            { throwable ->
                                when (throwable) {
                                    is BiometricAuthenticationException -> {
                                        Toast.makeText(this@MainActivity, "Error: ${throwable.errorString}", Toast.LENGTH_SHORT).show()
                                        Log.e(LOG_TAG, "BiometricAuthenticationException(${throwable.errorMessageId}, '${throwable.errorString}')", throwable)
                                    }
                                    is BiometricAuthenticationCancelledException -> {
                                        Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_SHORT).show()
                                        Log.d(LOG_TAG, "onError(BiometricAuthenticationCancelledException)")
                                    }
                                    else -> Log.e(LOG_TAG, "onError()", throwable)
                                }
                            }
                    )
        }
    }
}
