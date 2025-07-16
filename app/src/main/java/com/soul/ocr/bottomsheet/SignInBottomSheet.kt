package com.soul.ocr.bottomsheet

/* --- imports --- */
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.soul.ocr.R
import com.soul.ocr.databinding.DialogSignInBinding

class SignInBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogSignInBinding? = null
    private val binding get() = _binding!!

    private lateinit var oneTap: SignInClient
    private lateinit var req: BeginSignInRequest

    /* --- One‑Tap launcher --- */
    private val oneTapLauncher =
        registerForActivityResult(StartIntentSenderForResult()) { result ->
            try {
                val credential = oneTap.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                val name    = credential.displayName
                if (idToken != null) {
                    Toast.makeText(requireContext(), "Welcome $name!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Log.e(TAG, "googleIdToken is null (email=${credential.id})")
                    Toast.makeText(requireContext(), "Google sign‑in failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "One‑Tap credential error (status=${e.statusCode})", e)
                Toast.makeText(requireContext(), "Google sign‑in failed", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* --- One‑Tap config --- */
        oneTap = Identity.getSignInClient(requireContext())
        req = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.server_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()

        /* --- Clicks --- */
        binding.btnGoogle.setOnClickListener { launchOneTap() }
        binding.btnEmail.setOnClickListener {
            dismiss()
            EmailSignInBottomSheet().show(parentFragmentManager, "EmailSignInSheet")
        }
    }

    private fun launchOneTap() {
        oneTap.beginSignIn(req)
            .addOnSuccessListener(requireActivity()) { res ->
                try {
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(res.pendingIntent.intentSender).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "IntentSender launch failure", e)
                    Toast.makeText(requireContext(), "Google sign‑in failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "beginSignIn failed", e)
                if (e is ApiException) {
                    Log.e(TAG, "Status code = ${e.statusCode}")
                }
                Toast.makeText(
                    requireContext(),
                    "Google sign‑in failed (${e.localizedMessage ?: "unknown"})",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SignInSheet"
    }
}
