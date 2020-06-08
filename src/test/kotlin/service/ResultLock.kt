package test.kotlin.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Utility class
 *
 * Block execution until result is as expected or timeout (deadline) elapse
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class ResultLock<T: Any>(alternativeResult: T, private val waitResult: T? = null): ReentrantLock() {
    private var completed: Boolean = false
    private val lock = newCondition()
    var result: T = alternativeResult

    fun deadline(maxMillis: Int) = withLock {
        if (!completed) lock.await(maxMillis.toLong(), TimeUnit.MILLISECONDS)
        completed = true
    }

    fun set(action: () -> T) = withLock {
        result = action()
        if (!completed && waitResult?.let { it == result } != false) {
            completed = true
            lock.signalAll()
        }
    }
}