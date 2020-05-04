package com.tailoredapps.biometricsample

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tailoredapps.biometricauth.BiometricAuth
import com.tailoredapps.biometricauth.BiometricAuthenticationCancelledException
import com.tailoredapps.biometricauth.BiometricAuthenticationException
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    private val cryptoManager: CryptoManager by lazy {
        CryptoManager()
    }

    private val biometricAuth: BiometricAuth by lazy {
        BiometricAuth.create(this)
    }

    private val btnAuthenticate: Button by lazy { findViewById<Button>(R.id.btn_authenticate) }
    private val btnAuthenticateWithCrypto: Button by lazy { findViewById<Button>(R.id.btn_authenticate_with_crypto) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAuthenticate.setOnClickListener {
            testAuthenticate()
        }

        btnAuthenticateWithCrypto.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Toast.makeText(this, "KeyGenerator only available on >= Marshmallow", Toast.LENGTH_SHORT).show()
            } else {
                testAuthenticateWithCrypto()
            }
        }
    }


    private fun testAuthenticate() {
        if (biometricAuth.hasFingerprintHardware.not()) {
            Toast.makeText(this, "Devices provides no fingerprint hardware", Toast.LENGTH_SHORT).show()
        } else if (biometricAuth.hasFingerprintsEnrolled.not()) {
            Toast.makeText(this, "No fingerprints enrolled", Toast.LENGTH_SHORT).show()
        } else {
            biometricAuth.authenticate(
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
                                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                                Log.d(LOG_TAG, "onSuccess()")
                            },
                            { throwable ->
                                when (throwable) {
                                    is BiometricAuthenticationException -> {
                                        Toast.makeText(this, "Error: ${throwable.errorString}", Toast.LENGTH_SHORT).show()
                                        Log.e(LOG_TAG, "BiometricAuthenticationException(${throwable.errorMessageId}, '${throwable.errorString}')", throwable)
                                    }
                                    is BiometricAuthenticationCancelledException -> {
                                        Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                                        Log.d(LOG_TAG, "onError(BiometricAuthenticationCancelledException)")
                                    }
                                    else -> Log.e(LOG_TAG, "onError()", throwable)
                                }
                            }
                    )
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private fun testAuthenticateWithCrypto() {
        if (biometricAuth.hasFingerprintHardware.not()) {
            Toast.makeText(this, "Devices provides no fingerprint hardware", Toast.LENGTH_SHORT).show()
        } else if (biometricAuth.hasFingerprintsEnrolled.not()) {
            Toast.makeText(this, "No fingerprints enrolled", Toast.LENGTH_SHORT).show()
        } else {
            biometricAuth.authenticate(
                    cryptoObject = BiometricAuth.Crypto(cryptoManager.cipher),
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
                                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                                Log.d(LOG_TAG, "onSuccess()")
                            },
                            { throwable ->
                                when (throwable) {
                                    is BiometricAuthenticationException -> {
                                        Toast.makeText(this, "Error: ${throwable.errorString}", Toast.LENGTH_SHORT).show()
                                        Log.e(LOG_TAG, "BiometricAuthenticationException(${throwable.errorMessageId}, '${throwable.errorString}')", throwable)
                                    }
                                    is BiometricAuthenticationCancelledException -> {
                                        Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                                        Log.d(LOG_TAG, "onError(BiometricAuthenticationCancelledException)")
                                    }
                                    else -> Log.e(LOG_TAG, "onError()", throwable)
                                }
                            }
                    )
        }
    }


    private class CryptoManager {

        companion object {
            private const val KEY_NAME = "test-key"
        }

        private var keyStore: KeyStore? = null
        private var keyGenerator: KeyGenerator? = null


        val cipher: Cipher by lazy {
            generateKey()
            initCipher()
        }


        @TargetApi(Build.VERSION_CODES.M)
        private fun generateKey() {
            try {
                keyStore = KeyStore.getInstance("AndroidKeyStore").also { keyStore ->
                    keyStore.load(null)
                }

                keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").also { keyGenerator ->
                    keyGenerator?.init(KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            .setUserAuthenticationRequired(true)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .build())

                    keyGenerator?.generateKey()
                }
            } catch (e: KeyStoreException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: NoSuchProviderException) {
                e.printStackTrace()
            } catch (e: InvalidAlgorithmParameterException) {
                e.printStackTrace()
            } catch (e: CertificateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


        @TargetApi(Build.VERSION_CODES.M)
        private fun initCipher(): Cipher {
            try {
                val cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/"
                                + KeyProperties.BLOCK_MODE_CBC + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7)

                keyStore!!.load(null)
                val key = keyStore!!.getKey(KEY_NAME, null) as SecretKey
                cipher.init(Cipher.ENCRYPT_MODE, key)
                return cipher
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Failed to init cipher", e)
            } catch (e: NoSuchPaddingException) {
                throw RuntimeException("Failed to init cipher", e)
            }
        }
    }

}
