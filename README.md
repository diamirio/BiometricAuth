# BiometricAuth

[ ![Download](https://api.bintray.com/packages/tailoredmedia/maven/biometricauth/images/download.svg) ](https://bintray.com/tailoredmedia/maven/biometricauth/_latestVersion)
![API 15](https://img.shields.io/badge/API-15-yellow.svg)

This library brings the new Android P BiometricPrompt for fingerprint authentication to Android SDK 23, using RxJava2 (and Kotlin).


| Android 23..27 (>= Marshmallow)  |  Android 28 (Pie) |
| :------------------------------: | :---------------: |
| ![Dialog on Marshmallow devices](https://github.com/tailoredmedia/BiometricAuth/raw/master/screenshots/marshmallow.gif "Dialog shown on Android 23..27 devices") | ![Dialog on Pie devices](https://github.com/tailoredmedia/BiometricAuth/raw/master/screenshots/pie.gif "Dialog shown on Android 28 devices") |


## Setup

To use this library your `minSdkVersion` must be >= 15 (Note that the dialog however will only show starting Android SDK 23).

```gradle
allprojects {
    repositories {
        ...
        jcenter()
    }
}

dependencies {
    implementation 'com.tailoredapps:biometricauth:1.1.1'
}
```


## Usage


Create a `BiometricAuth` instance:

```kotlin
val biometricAuth = BiometricAuth.create(this); // where this is an (AppCompat-)Activity
```

```kotlin
if(!biometricAuth.hasFingerprintHardware) {
    //The devices does not support fingerprint authentication (i.e. has no fingerprint hardware)
    Toast.makeText(this, "Device does not support fingerprint", Toast.LENGTH_SHORT).show()
} else if(!biometricAuth.hasFingerprintsEnrolled) {
    //The user has not enrolled any fingerprints (i.e. fingerprint authentication is not activated by the user)
    Toast.makeText(this, "User has not enrolled any fingerprints", Toast.LENGTH_SHORT).show()
} else {
    biometricAuth
        .authenticate(
                title = "Please authenticate",
                subtitle = "'Awesome Feature' requires your authentication",
                description = "'Awesome Feature' exposes data private to you, which is why you need to authenticate.",
                negativeButtonText = "Cancel",
                prompt = "Touch the fingerprint sensor",
                notRecognizedErrorText = "Not recognized"
        )
        .subscribe(
                { Log.d("BiometricAuth", "User authentication successful.") },
                { throwable ->
                    if(throwable is BiometricAuthenticationCancelledException) {
                        Log.d("BiometricAuth", "User cancelled the operation")
                    } else if(throwable is BiometricAuthenticationException) {
                        // Might want to check throwable.errorMessageId for fields in BiometricConstants.Error,
                        // to get more information about the error / further actions here.
                        Log.d("BiometricAuth", "Unrecoverable authentication error")
                    } else {
                        Log.d("BiometricAuth", "Error during user authentication.")
                    }
                }
        )
}
```

The `authenticate()` function returns a [Completable](http://reactivex.io/RxJava/javadoc/io/reactivex/Completable.html), which either:

* completes, which indicates an authentication success, or
* emits an error:
  * [`BiometricAuthenticationCancelledException`](https://github.com/tailoredmedia/BiometricAuth/blob/master/biometricauth/src/main/java/com/tailoredapps/biometricauth/Exceptions.kt), which signals that the operation has been cancelled (most likely triggered by the user).
  * [`BiometricAuthenticationException`](https://github.com/tailoredmedia/BiometricAuth/blob/master/biometricauth/src/main/java/com/tailoredapps/biometricauth/Exceptions.kt), which signals an unrecoverable error during biometric authentication (e.g. too many invalid attempts).
    The exception contains the localized error-message provided by the system.
    Furthermore, the errorMessageId contained in this exception is most likely one of the fields in [`BiometricConstants.Error`](https://github.com/tailoredmedia/BiometricAuth/blob/master/biometricauth/src/main/java/com/tailoredapps/biometricauth/BiometricConstants.kt) (but no exclusively), which allows to get more information on the type or error.
  * any other _unexpected_ error during authentication (Not any of the *internal* fingerprint errors like "not detected", as they will be handled internally).


### CryptoObject

If you need to pass a crypto-object, add a `BiometricAuth.CryptoObject` as a first parameter to the `BiometricAuth.authenticate` method, which will then return a [Single](http://reactivex.io/RxJava/javadoc/io/reactivex/Single.html) including the CryptoObject as a success-value.


## What's inside?

On Android P (SDK 28) devices, the new [BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt) API is used.

On devices running Android Marshmallow to Oreo (SDK 23..27), the [FingerprintManagerCompat](https://developer.android.com/reference/android/support/v4/hardware/fingerprint/FingerprintManagerCompat) API is used in combination with a custom UI, which imitates the AndroidPie BiometricPrompt Bottom-Sheet.

On older devices, where Fingerprint Authentication is not supported by native Android SDKs, calling `hasFingerprintHardware` or `hasFingerprintsEnrolled` will always return false.


# License

```
Copyright 2018 Tailored Media GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
