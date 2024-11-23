package com.example.bloombooth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bloombooth.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val toolbar = binding.toolbar
        toolbar.title = "회원 가입"

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.emailVerificationBtn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            if (email.isNotEmpty()) {
                checkEmailAvailability(email)
            } else {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                if (email.isNotEmpty()) {
                    binding.emailVerificationBtn.isEnabled = true
                    binding.emailVerificationBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pink))
                    binding.emailVerificationBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.emailVerificationBtn.text = "중복 확인"
                } else {
                    binding.emailVerificationBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
                    binding.emailVerificationBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.emailVerificationBtn.text = "중복 확인"
                }
            }
        })

        binding.pwInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (isPasswordValid(password)) {
                    binding.passwordError.visibility = TextView.GONE
                    binding.signupBtn.isEnabled = true
                } else {
                    binding.passwordError.visibility = TextView.VISIBLE
                    binding.signupBtn.isEnabled = false
                }
            }
        })

        binding.signupBtn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.pwInput.text.toString()
            val nickname = binding.nicknameInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && nickname.isNotEmpty()) {
                createUser(email, password, nickname)
            } else {
                Toast.makeText(requireContext(),
                    "이메일, 비밀번호, 닉네임을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkEmailAvailability(email: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    if (result?.signInMethods?.isEmpty() == true) {
                        binding.emailVerificationBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pink))
                        binding.emailVerificationBtn.text = "검증 성공!"
                        binding.emailVerificationBtn.isEnabled = false
                        Toast.makeText(requireContext(), "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "이메일 중복검사에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isPasswordValid(password: String): Boolean {
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*])[A-Za-z\\d!@#\$%^&*]{6,12}$"
        return password.matches(regex.toRegex())
    }

    private fun createUser(email: String, password: String, nickname: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val profileUpdates = userProfileChangeRequest {
                            displayName = nickname
                        }
                        it.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    navigateToLoginFragment()
                                }
                            }
                    }
                } else {
                    Toast.makeText(requireContext(),
                        "회원가입이 실패했습니다. : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToLoginFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .addToBackStack(null)
            .commit()
    }
}
