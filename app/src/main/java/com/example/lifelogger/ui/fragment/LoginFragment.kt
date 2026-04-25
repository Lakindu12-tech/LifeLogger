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
import com.example.lifelogger.databinding.FragmentLoginBinding
import com.example.lifelogger.ui.viewmodel.AuthState
import com.example.lifelogger.ui.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If already logged in, go to list
        if (viewModel.isLoggedIn()) {
            findNavController().navigate(R.id.action_loginFragment_to_entryListFragment)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_entryListFragment)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
