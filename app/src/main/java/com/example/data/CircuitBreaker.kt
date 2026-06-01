package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object CircuitBreaker {
    enum class State {
        CLOSED,     // Normal operation, requests flow through
        OPEN,       // Service is failing, requests are blocked/fail-fast
        HALF_OPEN   // Testing if service has recovered
    }

    private val _state = MutableStateFlow(State.CLOSED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val failureCount = AtomicInteger(0)
    private val lastStateChangeTime = AtomicLong(0L)
    
    private const val FAILURE_THRESHOLD = 3
    private const val RESET_TIMEOUT_MS = 10000L // 10 seconds to retry recovery

    fun <T> execute(fallback: T, block: () -> T): T {
        val currentState = _state.value
        val now = System.currentTimeMillis()

        // Handle OPEN state timeout and recovery check
        if (currentState == State.OPEN) {
            if (now - lastStateChangeTime.get() > RESET_TIMEOUT_MS) {
                transitionTo(State.HALF_OPEN)
            } else {
                // Circuit is OPEN, fail-fast and return fallback instantly
                println("🔌 [Circuit Breaker] State is OPEN - Blocking query/upload and executing immediate offline fallback.")
                return fallback
            }
        }

        return try {
            val result = block()
            // If request succeeds in HALF_OPEN, close the circuit back to normal
            if (_state.value == State.HALF_OPEN) {
                transitionTo(State.CLOSED)
            }
            result
        } catch (e: Exception) {
            val count = failureCount.incrementAndGet()
            println("🔌 [Circuit Breaker] Call failed. Failure count: $count/$FAILURE_THRESHOLD. Error: ${e.message}")
            
            // Log this failure event to Firebase Crashlytics for production telemetry
            AnalyticsManager.logException(e, "CircuitBreaker execution failure count: $count")
            
            if (count >= FAILURE_THRESHOLD && _state.value != State.OPEN) {
                transitionTo(State.OPEN)
            }
            fallback
        }
    }

    private fun transitionTo(newState: State) {
        _state.value = newState
        lastStateChangeTime.set(System.currentTimeMillis())
        if (newState == State.CLOSED) {
            failureCount.set(0)
        }
        println("🔌 [Circuit Breaker] Transitioned state to: $newState")
    }
}
