package com.example.projektna.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import android.widget.TextView

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var layoutEmail: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var textRegister: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        layoutEmail = view.findViewById(R.id.layout_email)
        inputEmail = view.findViewById(R.id.input_email)
        layoutPassword = view.findViewById(R.id.layout_password)
        inputPassword = view.findViewById(R.id.input_password)
        btnLogin = view.findViewById(R.id.btn_login)
        progress = view.findViewById(R.id.progress)
        textRegister = view.findViewById(R.id.text_register)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        textRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    setLoading(true)
                }
                is Resource.Success -> {
                    setLoading(false)
                    // Navigiraj na glavni zaslon
                    findNavController().navigate(R.id.action_login_to_main)
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

    private fun attemptLogin() {
        // Počisti napake
        layoutEmail.error = null
        layoutPassword.error = null

        val email = inputEmail.text?.toString()?.trim() ?: ""
        val password = inputPassword.text?.toString() ?: ""

        // Validacija
        var isValid = true

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
        }

        if (isValid) {
            viewModel.login(email, password)
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        inputEmail.isEnabled = !loading
        inputPassword.isEnabled = !loading
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetLoginState()
    }
}
