package com.example.kotlin.redis.lock

import com.example.kotlin.config.Loggable
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class LockManager(
    private val redissonClient: RedissonClient
): Loggable{

    /**
     * 단일 키에 대한 락
     */
    fun tryLock(
        key: String,
        waitTime: Long = 3,
        leaseTime: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): RLock? {
        val lock = redissonClient.getLock(key)

        return if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
            log.info { "Lock 획득 성공 - key: $key" }
            lock // lock 객체 반환
        } else {
            null
        }
    }

    /**
     * 여러 키에 대한 멀티락
     */
    fun tryMultiLock(
        keys: List<String>,
        waitTime: Long = 3,
        leaseTime: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): RLock? {

        // 특정 key들에 대한 Rock 객체
        val locks = keys.map { redissonClient.getLock(it) }

        val multiLock = redissonClient.getMultiLock(*locks.toTypedArray())

        return if (multiLock.tryLock(waitTime, leaseTime, timeUnit)) {
            log.info { "MultiLock 획득 성공 - keys: $keys" }
            multiLock // lock 객체 반환
        } else {
            null
        }
    }

    fun unlock(lock: RLock?) {
        if (lock == null) return

        try {
            lock.unlock()
            log.info { "Lock 반환 성공" }
        } catch (e: IllegalMonitorStateException) {
            log.warn { "Lock 이미 해제되었거나 소유하지 않음 - ${e.message}" }
        } catch (e: Exception) {
            log.error(e) { "Lock 해제 실패" }
        }
    }
}