package com.example.kotlin

import com.example.kotlin.redis.lock.RedisLockUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CuponService(
    private val cuponRepository: CuponRepository,
    private val redisLockUtil: RedisLockUtil,
) {

    fun couponDecrease(couponName: String, couponId: Long) {
        redisLockUtil.acquireLockAndRun(key = "lock:$couponName") {
            decreaseCntTransactional(couponId)
        }
    }

    @Transactional
    fun decreaseCntTransactional(couponId: Long) {

        val coupon: Cupon = cuponRepository.findById(couponId)
            .orElseThrow { IllegalArgumentException("쿠폰을 찾을 수 없습니다.") }

        coupon.decrease()
        cuponRepository.save(coupon)
    }
}