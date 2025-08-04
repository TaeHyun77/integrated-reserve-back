package com.example.kotlin.config

import com.example.kotlin.jwt.CustomLogoutWebFilter
import com.example.kotlin.jwt.CustomUserDetailService
import com.example.kotlin.jwt.LoginFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

import kotlin.jvm.java

@EnableWebFluxSecurity
@Configuration
class SecurityConfig(
    private val loginWebFilter: LoginFilter,
    private val customLogoutWebFilter: CustomLogoutWebFilter,
    private val userDetailsService: CustomUserDetailService
) {

    @Bean
    fun authenticationManager(): ReactiveAuthenticationManager {
        val manager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
        manager.setPasswordEncoder(passwordEncoder())
        return manager
    }

    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange {
                it.pathMatchers(
                    "/", "/member/**", "/venue/**", "/performance/**",
                    "/screenInfo/**", "/seat/**", "/reToken", "/reserve/**", "/queue/**"
                ).permitAll()
                    .pathMatchers("/admin").hasRole("ADMIN")
                    .pathMatchers("/login", "/logout").permitAll()
                    .anyExchange().authenticated()
            }
            .addFilterAt(loginWebFilter, SecurityWebFiltersOrder.FORM_LOGIN)
            .addFilterBefore(customLogoutWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf("http://localhost:3000", "http://localhost:8080")
        configuration.allowedMethods = listOf("*")
        configuration.allowCredentials = true
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}