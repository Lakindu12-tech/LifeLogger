package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.lifelogger.R
import com.example.lifelogger.databinding.FragmentRegisterBinding
import com.example.lifelogger.ui.viewmodel.AuthState
import com.example.lifelogger.ui.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (password == confirmPassword) {
                    viewModel.register(email, password)
                } else {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginPrompt.setOnClickListener {
            findNavController().popBackStack()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.registerButton.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Registration Successful. Please check your email if confirmation is required.", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.registerButton.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.registerButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
