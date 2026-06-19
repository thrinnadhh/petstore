package com.example.domain.common

import java.util.UUID

interface IdGenerator {
    fun next(prefix: String): String
}

class UuidIdGenerator : IdGenerator {
    override fun next(prefix: String): String {
        return prefix + UUID.randomUUID().toString().substring(0, 8)
    }
}
