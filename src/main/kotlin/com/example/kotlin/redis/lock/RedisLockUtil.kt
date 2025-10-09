package com.example.kotlin.redis.lock

import com.example.kotlin.config.Loggable
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class RedisLockUtil(
    private val lockManager: LockManager
): Loggable {

    /**
     * 단일 키용 락
     */
    fun <T> acquireLockAndRun(key: String, block: () -> T): T {
        val lock = lockManager.tryLock(key)
            ?: throw ReserveException(HttpStatus.CONFLICT, ErrorCode.REDIS_FAILED_TO_ACQUIRED_LOCK)

        return try {
            block()
        } finally {
            lockManager.unlock(lock)
        }
    }

    /**
     * 여러 키에 대한 멀티락
     */
    fun <T> acquireMultiLockAndRun(keys: List<String>, block: () -> T): T {
        if (keys.isEmpty()) {
            log.error { "[RedisLockError] keys are empty" }
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.REDIS_NOT_EXIST_LOCK_KEY)
        }

        val lock = lockManager.tryMultiLock(keys)
            ?: throw ReserveException(HttpStatus.CONFLICT, ErrorCode.REDIS_FAILED_TO_ACQUIRED_LOCK)

        return try {
            block()
        } finally {
            lockManager.unlock(lock)
        }
    }
}