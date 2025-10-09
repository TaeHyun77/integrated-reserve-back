package com.example.kotlin

import org.springframework.data.jpa.repository.JpaRepository

interface CuponRepository: JpaRepository<Cupon, Long> {
}