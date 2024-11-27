package com.example.bloombooth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bloombooth.databinding.FragmentRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

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
            if (isEmailValid(email)) {
                checkEmailAvailability(email)
            } else if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "유효하지 않은 이메일 형식입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.emailVerificationBtn.apply {
                    text = "중복 확인"
                    isEnabled = true
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.pwInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val confirmPassword = binding.pwCheckInput.text.toString()

                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    binding.passwordCheckError.visibility = View.VISIBLE
                } else {
                    binding.passwordCheckError.visibility = View.GONE
                }

                if (password.isEmpty()) {
                    binding.passwordError.visibility = View.GONE
                } else if (isPasswordValid(password)) {
                    binding.passwordError.visibility = View.GONE
                } else {
                    binding.passwordError.visibility = View.VISIBLE
                }

                updateSignupButtonState()
            }
        })

        binding.pwCheckInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = binding.pwInput.text.toString()
                val confirmPassword = s.toString()

                if (password != confirmPassword) {
                    binding.passwordCheckError.visibility = View.VISIBLE
                } else {
                    binding.passwordCheckError.visibility = View.GONE
                }

                updateSignupButtonState()
            }
        })

        binding.nicknameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val nickname = s.toString()
                if (nickname.isEmpty()) {
                    binding.nicknameError.visibility = View.GONE
                } else if (isNicknameValid(nickname)) {
                    binding.nicknameError.visibility = View.GONE
                } else {
                    binding.nicknameError.visibility = View.VISIBLE
                }
                updateSignupButtonState()
            }
        })

        binding.signupBtn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.pwInput.text.toString()
            val nickname = binding.nicknameInput.text.toString()

            createUser(email, password, nickname)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex())
    }

    private fun isPasswordValid(password: String): Boolean {
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*])[A-Za-z\\d!@#\$%^&*]{6,12}$"
        return password.matches(regex.toRegex())
    }

    private fun isNicknameValid(nickname: String): Boolean {
        val regex = "^[가-힣a-zA-Z]{2,12}$"
        return nickname.matches(regex.toRegex())
    }

    private fun updateSignupButtonState() {
        val email = binding.emailInput.text.toString()
        val password = binding.pwInput.text.toString()
        val confirmPassword = binding.pwCheckInput.text.toString()
        val nickname = binding.nicknameInput.text.toString()

        val isReady = email.isNotEmpty() &&
                isPasswordValid(password) &&
                password == confirmPassword &&
                isNicknameValid(nickname)

        binding.signupBtn.isEnabled = isReady
        binding.signupBtn.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (isReady) R.color.pink else R.color.gray
            )
        )
    }

    private fun checkEmailAvailability(email: String) {
        val db = Firebase.firestore
        db.collection("user")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.emailVerificationBtn.apply {
                        text = "검증 성공!"
                        isEnabled = false
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pink))
                    }
                    Toast.makeText(requireContext(), "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "이메일 중복 검사에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUser(email: String, password: String, nickname: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "nickname" to nickname,
                            "email" to email
                        )

                        val db = FirebaseFirestore.getInstance()
                        db.collection("user")
                            .document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "회원가입이 성공적으로 완료되었습니다:) 해당 계정으로 로그인해주세요!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigateToLoginFragment()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "데이터 저장 실패: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "회원가입에 실패했습니다: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
