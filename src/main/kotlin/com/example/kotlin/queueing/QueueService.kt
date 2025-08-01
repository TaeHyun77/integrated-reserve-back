package com.example.kotlin.queueing

import com.example.kotlin.config.Loggable
import com.example.kotlin.queueing.event.QueueEventPayload
import com.example.kotlin.outbox.Outbox
import com.example.kotlin.outbox.OutboxRepository
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.util.ACCESS_TOKEN
import com.example.kotlin.util.ALLOW_QUEUE
import com.example.kotlin.util.TOKEN_TTL_INFO
import com.example.kotlin.util.WAIT_QUEUE
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Sinks
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant

@Service
class QueueService (

    val sink: Sinks.Many<QueueEventPayload> = Sinks.many().replay().limit(1),

    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val outboxRepository: OutboxRepository

): Loggable {

    /**
     * 대기열 등록
     *
     * queueType은 "reserve_[대기열 종류]"의 형태로 되어 있음
    * */
    suspend fun registerUserToWaitQueue(
        userId: String,
        queueType: String,
        enterTimestamp: Long
    ): Long {

        val inWait = searchUserRanking(userId, queueType, "wait")
        val inAllow = searchUserRanking(userId, queueType, "allow")
        val waitQueueKey = queueType + WAIT_QUEUE

        // 대기열 혹은 참가열에 존재한다면 에러 발생
        if (inWait != -1L || inAllow != -1L) {
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.ALREADY_REGISTERED_USER)
        }

        val wasAdded = reactiveRedisTemplate.opsForZSet()
            .add(waitQueueKey, userId, enterTimestamp.toDouble())
            .awaitFirstOrNull() // Boolean 혹은 null 반환

        if (wasAdded == null || !wasAdded) {
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.ALREADY_REGISTERED_USER)

        // 대기열에 성공적으로 추가 되었다면 카프카 메세지 전송
        } else {
            sendEventToKafka(waitQueueKey, userId, "WAIT")
        }

        val rank = searchUserRanking(userId, queueType, "wait")
        log.info{"${userId}님 ${rank}번째로 사용자 대기열 등록 성공" }

        return rank
    }

    /**
     * 대기열 or 참가열에서 사용자 순위 조회
     * 존재하지 않는다면 -1L 반환, 존재한다면 사용자의 순위 반환
     * */
    suspend fun searchUserRanking(
        userId: String,
        queueType: String,
        queueCategory: String
    ): Long {

        val keyType = if (queueCategory == "wait") WAIT_QUEUE else ALLOW_QUEUE

        val queueKey = queueType + keyType

        val redisRank = reactiveRedisTemplate.opsForZSet()
            .rank(queueKey, userId)
            .awaitFirstOrNull()

        val rank = redisRank?.takeIf { it >= 0 }?.plus(1) ?: -1

        if (rank == -1L) {
            log.info { "[$queueCategory] $userId 님이 존재하지 않습니다." }
        } else {
            log.info { "[$queueCategory] $userId 님의 현재 순위 $rank" }
        }

        return rank
    }

    suspend fun cancelWaitOrAllowUser(
        userId: String,
        queueType: String,
        queueCategory: String
    ): ResponseEntity<String> {

        try {
            if (queueCategory == "wait") {
                val waitQueueKey = "$queueType$WAIT_QUEUE"

                val removedCount = reactiveRedisTemplate.opsForZSet()
                    .remove(waitQueueKey, userId) // 성공 : 삭제된 개수 반환 , 실패 : 0L 반환
                    .awaitSingle()

                if (removedCount == 0L) {
                    throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.USER_NOT_FOUND_IN_THE_QUEUE)
                }

                sendEventToKafka(waitQueueKey, userId, "CANCELED")

                return ResponseEntity.ok("대기열 삭제 완료")

            } else {
                val allowQueueKey = "$queueType$ALLOW_QUEUE"

                val allowRemovedCount = reactiveRedisTemplate.opsForZSet()
                    .remove(allowQueueKey, userId)
                    .awaitSingle()

                if (allowRemovedCount == 0L) {
                    throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.USER_NOT_FOUND_IN_THE_QUEUE)
                }

                val ttlRemovedCount = reactiveRedisTemplate.opsForZSet()
                    .remove(TOKEN_TTL_INFO, userId)
                    .awaitSingle()

                if (ttlRemovedCount == 0L) {
                    log.warn { "${userId}님의 TTL 키가 존재하지 않아 삭제되지 않았습니다." }
                }

                return ResponseEntity.ok("참가열 삭제 완료")
            }
        } catch (e: ReserveException) {
            log.error { "예약 취소 중 오류 발생" }
            throw e
        }
    }

    /**
     * 유효성 검사를 위한 토큰 생성
     */
    suspend fun generateAccessToken(
        userId: String,
        performanceId: Long
    ): String {

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val raw = performanceId.toString() + ACCESS_TOKEN + userId
            val hash = digest.digest(raw.toByteArray(StandardCharsets.UTF_8))

            buildString {
                hash.forEach { append(String.format("%02x", it)) }
            }
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Token 생성 실패", e)
        }
    }

    /**
     * 생성한 토큰을 쿠키에 저장
     */
    suspend fun sendCookie(
        userId: String,
        performanceId: Long,
        response: HttpServletResponse
    ): ResponseEntity<String> {

        val encodedName = URLEncoder.encode(userId, StandardCharsets.UTF_8)
        val token = generateAccessToken(userId, performanceId)
        val cookieName = performanceId.toString() + "_user-access-cookie_$encodedName"

        val cookie = Cookie(cookieName, token).apply {
            path = "/"
            maxAge = 300
        }

        response.addCookie(cookie)

        return ResponseEntity.ok("쿠키 발급 완료")
    }

    /**
     * 토큰 유효성 검사
     */
    suspend fun isAccessTokenValid(
        userId: String,
        performanceId: Long,
        token: String
    ): Boolean {

        val ttlInfo = reactiveRedisTemplate
            .opsForZSet()
            .score(TOKEN_TTL_INFO, userId)
            .awaitFirstOrNull() ?: return false

        val expireTime = ttlInfo.toLong()
        val now = Instant.now().epochSecond

        if (expireTime < now) return false

        return if (generateAccessToken(userId, performanceId) == token) {
            log.info { "유효한 토큰입니다 !" }
            true
        } else false
    }

    /**
     * 대기열에서 새로고침 시 대기열 후순위 재등록 로직 - WAIT
     */
    suspend fun reEnterWaitQueue(
        userId: String,
        queueType: String
    ) {

        val now = Instant.now()
        val newTimestamp = now.epochSecond * 1_000_000_000L + now.nano
        val waitQueueKey = "$queueType$WAIT_QUEUE"

        val added = reactiveRedisTemplate
            .opsForZSet()
            .add(waitQueueKey, userId, newTimestamp.toDouble())
            .awaitFirstOrNull()

        if (added == true) {
            sendEventToKafka(waitQueueKey, userId, "WAIT")
        }
    }

    /**
     * 참가열로 사용자 이동 + 토큰 TTL 저장
     */
    suspend fun allowUser(
        queueType: String,
        count: Long
    ): Long {
        val waitQueueKey = "$queueType$WAIT_QUEUE"
        val allowQueueKey = "$queueType$ALLOW_QUEUE"

        val poppedUsers = reactiveRedisTemplate
            .opsForZSet()
            .popMin(waitQueueKey, count)
            .collectList()
            .awaitSingle()

        if (poppedUsers.isEmpty()) return 0

        var movedCount = 0L

        for (user in poppedUsers) {
            val userId: String = user.value.toString()

            val now = Instant.now()
            val timestamp = now.epochSecond * 1_000_000_000L + now.nano
            val expireAt = now.plus(Duration.ofMinutes(10)).epochSecond

            // 참가열에 사용자 이동
            reactiveRedisTemplate.opsForZSet()
                .add(allowQueueKey, userId, timestamp.toDouble())
                .awaitFirstOrNull()

            // 참가열 TTL 생성
            reactiveRedisTemplate.opsForZSet()
                .add(TOKEN_TTL_INFO, userId, expireAt.toDouble())
                .awaitFirstOrNull()

            sendEventToKafka(allowQueueKey, userId, "ALLOW")
            movedCount++
        }

        return movedCount
    }

    /**
     * 대기열 → 참가열 사용자 이동 스케줄링
     */
    @OptIn(DelicateCoroutinesApi::class)
    @Scheduled(fixedDelay = 5000, initialDelay = 30000)
    fun moveUserToAllowQ() {

        val maxAllowedUsers = 3L
        val queueTypes = listOf("reserve_공연A", "reserve_공연B", "reserve_공연C")

        queueTypes.forEach { queueType ->
            kotlinx.coroutines.GlobalScope.launch {
                val count = allowUser(queueType, maxAllowedUsers)

                if (count > 0) {
                    log.info{ "$queueType 허용열로 이동한 사용자 : $count" }
                } else {
                    log.info{ "$queueType 허용열로 이동한 사용자 : 0" }
                }
            }
        }
    }

    @Transactional
    suspend fun sendEventToKafka(
        queueType: String,
        userId: String,
        status: String
    ) {
        withContext(Dispatchers.IO) {
            val existing = outboxRepository.findByUserId(userId)

            if (existing != null) {
                log.info { "$userId 님 상태 변경 ⇒ $status" }

                existing.updateUserStatus(status)
                outboxRepository.save(existing)
            } else {
                val newOutbox = Outbox(
                    queueType = queueType,
                    userId = userId,
                    status = status
                )
                outboxRepository.save(newOutbox)
            }
        }
    }
}