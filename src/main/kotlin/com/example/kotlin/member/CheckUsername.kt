package com.example.kotlin.member

import com.example.kotlin.util.removeSpacesAndHyphens

@JvmInline
value class CheckUsername private constructor (val username: String) {

    companion object {
        // 브라우저가 # 이후는 프래그먼트로 간주하여 서버에 전달하지 않음, 따라서 #은 허용하면 안됨
        private val USERNAME_REGEX = Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@*^]+$")

        // 직접 CheckUsername 인스턴스를 생성할 때 사용할 수 있는 invoke 오버로딩
        // companion object 안에 operator fun invoke(...)를 정의하면 생성자 호출처럼 사용할 수 있음
        operator fun invoke(username: String): CheckUsername {
            val cleanedUsername = username.removeSpacesAndHyphens()

            return CheckUsername(cleanedUsername)
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

/* 실행 흐름
* 요청에서 CheckUsername(username)을 호출하면, 먼저 removeSpacesAndHyphens()이 실행되며, CheckUsername.Companion.invoke(username)가 실행됩니다.
* invoke 함수는 내부적으로 CheckUsername(...)를 생성하며, 이 과정에서 init 블록이 실행되어 정규식을 기반으로 사용자명 유효성 검증이 수행됩니다.
* */