package com.example.myapplication.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Google sign-in using Android's Credential Manager, exchanged for a Firebase
 * Auth session. Firebase keeps the user signed in between app launches, so
 * [user] starts out non-null for a returning user and the login screen can be
 * skipped.
 *
 * [context] must be an Activity context (LocalContext.current inside Compose),
 * because Credential Manager shows its account picker on top of the Activity.
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle(context: Context) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val option = GetSignInWithGoogleOption
                    .Builder(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential =
                        GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    _user.value = auth.currentUser
                } else {
                    _error.value = "Unexpected sign-in response. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                // User closed the account picker. Not an error worth showing.
            } catch (e: GetCredentialException) {
                _error.value = e.message ?: "Google sign-in failed."
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong."
            } finally {
                _loading.value = false
            }
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        _user.value = null
        viewModelScope.launch {
            try {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Firebase is already signed out; clearing the cached Google state is best-effort.
            }
        }
    }
}
