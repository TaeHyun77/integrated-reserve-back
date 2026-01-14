package com.example.kotlin.member

import com.example.kotlin.util.removeSpacesAndHyphens

@JvmInline
value class CheckUsername private constructor (val username: String) {

    companion object {
        // 브라우저가 # 이후는 프래그먼트로 간주하여 서버에 전달하지 않음, 따라서 #은 허용하면 안됨
        private val USERNAME_REGEX = Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@*^]+$")

        fun from(username: String): CheckUsername {
            return CheckUsername(username)
        }
    }

    init {
        validateUsername(username)
    }

    private fun validateUsername(username: String) {
        require(USERNAME_REGEX.matchEntire(username) != null) {
            "Invalid username"
        }
    }
}