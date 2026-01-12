package com.example.projektna.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.projektna.R
import com.example.projektna.util.Resource
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var layoutUsername: TextInputLayout
    private lateinit var inputUsername: TextInputEditText
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var inputPassword: TextInputEditText
    private lateinit var layoutPasswordConfirm: TextInputLayout
    private lateinit var inputPasswordConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var textLogin: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        layoutUsername = view.findViewById(R.id.layout_username)
        inputUsername = view.findViewById(R.id.input_username)
        layoutEmail = view.findViewById(R.id.layout_email)
        inputEmail = view.findViewById(R.id.input_email)
        layoutPassword = view.findViewById(R.id.layout_password)
        inputPassword = view.findViewById(R.id.input_password)
        layoutPasswordConfirm = view.findViewById(R.id.layout_password_confirm)
        inputPasswordConfirm = view.findViewById(R.id.input_password_confirm)
        btnRegister = view.findViewById(R.id.btn_register)
        progress = view.findViewById(R.id.progress)
        textLogin = view.findViewById(R.id.text_login)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            attemptRegister()
        }

        inputPasswordConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister()
                true
            } else {
                false
            }
        }

        textLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    setLoading(true)
                }
                is Resource.Success -> {
                    setLoading(false)
                    showSuccess(getString(R.string.register_success))
                    // Pojdi nazaj na prijavo
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(result.message ?: getString(R.string.auth_error_unknown))
                }
                null -> {
                    // Začetno stanje
                }
            }
        }
    }

    private fun attemptRegister() {
        // Počisti napake
        layoutUsername.error = null
        layoutEmail.error = null
        layoutPassword.error = null
        layoutPasswordConfirm.error = null

        val username = inputUsername.text?.toString()?.trim() ?: ""
        val email = inputEmail.text?.toString()?.trim() ?: ""
        val password = inputPassword.text?.toString() ?: ""
        val passwordConfirm = inputPasswordConfirm.text?.toString() ?: ""

        // Validacija
        var isValid = true

        if (username.isEmpty()) {
            layoutUsername.error = getString(R.string.auth_error_username_required)
            isValid = false
        } else if (username.length < 3) {
            layoutUsername.error = getString(R.string.auth_error_username_short)
            isValid = false
        }

        if (email.isEmpty()) {
            layoutEmail.error = getString(R.string.auth_error_email_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = getString(R.string.auth_error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            layoutPassword.error = getString(R.string.auth_error_password_required)
            isValid = false
        } else if (password.length < 6) {
            layoutPassword.error = getString(R.string.auth_error_password_short)
            isValid = false
        }

        if (passwordConfirm.isEmpty()) {
            layoutPasswordConfirm.error = getString(R.string.auth_error_password_confirm_required)
            isValid = false
        } else if (password != passwordConfirm) {
            layoutPasswordConfirm.error = getString(R.string.auth_error_passwords_mismatch)
            isValid = false
        }

        if (isValid) {
            viewModel.register(username, email, password)
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        inputUsername.isEnabled = !loading
        inputEmail.isEnabled = !loading
        inputPassword.isEnabled = !loading
        inputPasswordConfirm.isEnabled = !loading
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSuccess(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetRegisterState()
    }
}
