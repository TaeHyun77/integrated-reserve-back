package com.example.kotlin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
class LockTest {

    @Autowired
    private lateinit var cuponService: CuponService

    @Autowired
    private lateinit var cuponRepository: CuponRepository

    private lateinit var initialCoupon: Cupon
    private val COUPON_NAME = "CUPON_001"
    private val INITIAL_STOCK = 100L
    private val THREAD_COUNT = 100

    @BeforeEach
    fun setUp() {
        initialCoupon = Cupon(null, COUPON_NAME, INITIAL_STOCK)

        cuponRepository.save(initialCoupon)
    }

    @AfterEach
    fun afterSetUo() {
        cuponRepository.delete(initialCoupon)
    }

    @DisplayName("락_사용을_통한_100개의_쿠폰_사용_테스트")
    @Test
    fun lockTest(): Unit {

        val executorService = Executors.newFixedThreadPool(THREAD_COUNT)
        val latch = CountDownLatch(THREAD_COUNT)

        val couponId = initialCoupon.id!!

        for (i in 1..THREAD_COUNT) {

            executorService.submit {
                try {
                    cuponService.couponDecrease(COUPON_NAME, couponId)
                } catch (e: Exception) {
                    println("예외 발생: ${e.message}")
                } finally {
                    latch.countDown() // 작업 완료 신호
                }
            }
        }

        latch.await() // 모든 스레드가 작업을 완료할 때까지 대기

        val finalCoupon = cuponRepository.findById(couponId)
            .orElseThrow { IllegalStateException("쿠폰을 찾을 수 없습니다.") }

        assertEquals(0L, finalCoupon.cuponCnt, "잔여 쿠폰 수가 0이 아닙니다.")

        println("잔여 쿠폰 개수 = ${finalCoupon.cuponCnt}")
        executorService.shutdown()
    }
}