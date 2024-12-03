package com.example.bloombooth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bloombooth.auth.FirebaseAuthManager.auth
import com.example.bloombooth.databinding.ActivityPersonalInfoBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class PersonalInfoActivity : AppCompatActivity() {
    private val binding: ActivityPersonalInfoBinding by lazy { ActivityPersonalInfoBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val user = auth.currentUser
        if (user != null) {
            fetchUserData(user.uid)
        } else {
            binding.username.text = "사용자의 닉네임"
            binding.email.text = "이메일이 존재하지 않습니다."
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        addTextWatchers()

        binding.updateUserinfoBtn.setOnClickListener {
            if (validateInputs()) {
                if (isNicknameChanged()) updateUserNickname()
                if (isPwChanged()) updateUserPw()
            }
        }
    }

    private fun fetchUserData(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "사용자의 닉네임"
                    val email = document.getString("email") ?: "이메일이 존재하지 않습니다."

                    binding.username.text = nickname
                    binding.email.text = email
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun addTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.nickname.addTextChangedListener(watcher)
        binding.oldPw.addTextChangedListener(watcher)
        binding.newPw.addTextChangedListener(watcher)
        binding.newPwCheck.addTextChangedListener(watcher)
    }

    private fun validateInputs(): Boolean {
        val nickname = binding.nickname.text.toString()
        val currentPassword = binding.oldPw.text.toString()
        val newPassword = binding.newPw.text.toString()
        val confirmPassword = binding.newPwCheck.text.toString()

        val nicknameRegex = "^[가-힣a-zA-Z]{2,12}$".toRegex()
        if (nickname.isNotBlank() && !nickname.matches(nicknameRegex)) {
            Toast.makeText(this, "닉네임은 한글과 영문을 포함하며 2자~12자여야 합니다.",
                Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isNotBlank() || confirmPassword.isNotBlank() || currentPassword.isNotBlank()) {
            val passwordRegex = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{6,12}$".toRegex()
            if (newPassword.isNotBlank() && !newPassword.matches(passwordRegex)) {
                Toast.makeText(this, "비밀번호는 영문, 숫자, 특수기호를 포함하여 6자~12자여야 합니다.",
                    Toast.LENGTH_SHORT).show()
                return false
            }

            if (currentPassword == newPassword) {
                Toast.makeText(this, "기존 비밀번호와 다른 새 비밀번호를 입력해주세요.",
                    Toast.LENGTH_SHORT).show()
                return false
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.",
                    Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun isNicknameChanged(): Boolean {
        return binding.nickname.text.isNotBlank()
    }

    private fun isPwChanged(): Boolean {
        return binding.oldPw.text.isNotBlank() && binding.newPw.text.isNotBlank() &&
                binding.newPwCheck.text.isNotBlank()
    }

    private fun updateButtonState() {
        binding.updateUserinfoBtn.isEnabled = isNicknameChanged() || isPwChanged()
    }

    private fun updateUserNickname() {
        val nickname = binding.nickname.text.toString()

        if (nickname.isBlank()) {
            Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (nickname.isNotBlank() && nickname == binding.username.text.toString()) {
            Toast.makeText(this, "기존 닉네임과 다른 닉네임을 입력해주세요.",
                Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        user?.let {
            db.collection("user").document(user.uid)
                .update("nickname", nickname)
                .addOnSuccessListener {
                    Toast.makeText(this, "닉네임이 성공적으로 업데이트되었습니다.",
                        Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "닉네임 업데이트에 실패했습니다: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
        }
        binding.nickname.text.clear()
    }

    private fun updateUserPw() {
        val currentPassword = binding.oldPw.text.toString()
        val newPassword = binding.newPw.text.toString()
        val confirmPassword = binding.newPwCheck.text.toString()

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "비밀번호 변경을 위해 모든 비밀번호 필드를 채워주세요.",
                Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.",
                Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다."
                                    , Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "비밀번호 변경에 실패했습니다: " +
                                        "${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "기존 비밀번호가 올바르지 않습니다.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "재인증에 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        clearPasswordFields()
    }

    private fun clearPasswordFields() {
        binding.oldPw.text.clear()
        binding.newPw.text.clear()
        binding.newPwCheck.text.clear()
    }
}