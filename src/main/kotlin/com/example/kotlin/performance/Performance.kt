package com.example.kotlin.performance

import com.example.kotlin.screenInfo.ScreenInfo
import jakarta.persistence.*

@Entity
class Performance(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_id")
    val id: Long? = null,

    val type: String,

    val title: String,

    val duration: String,

    var price: Long,

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], orphanRemoval = true)
    val screenInfoList: List<ScreenInfo> = ArrayList()
)