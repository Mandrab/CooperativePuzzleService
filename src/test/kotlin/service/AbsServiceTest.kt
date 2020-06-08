package test.kotlin.service

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Before
import main.kotlin.service.Gateway
import java.io.File
import kotlin.random.Random

/**
 * Not a direct test class but contains useful/commons methods used in these tests
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
abstract class AbsServiceTest {
    protected val vertx: Vertx = Vertx.vertx()
    protected val client: WebClient = WebClient.create(vertx)

    /**
     * Run methods that returns futures in sequential-like mode.
     * Methods with higher number (in pair) are executed after methods with lower value.
     * If more methods have the same number, execute all of them before proceed with next value.
     * The future result is discarded.
     *
     * @param tests an array of couple where first value indicates 'precedence' (execute ones with lower value first)
     */
    protected fun runSequentially(vararg tests: Pair<Int, () -> Future<Any>>): Future<Any> {
        val sequenceCompleted = Promise.promise<Any>()
        val testsGroups = tests.groupBy { it.first }.toList()

        testsGroups.firstOrNull()?.let { testGroup ->

            CompositeFuture.all(testGroup.second.map { test -> test.second() }.toMutableList()).onSuccess {
                runSequentially(
                    *testsGroups.filterNot { it == testGroup }
                        .map { it.second }.fold(mutableListOf<Pair<Int, () -> Future<Any>>>()) {
                                acc, pairList -> acc.addAll(pairList); acc
                        }.toTypedArray()
                ).onComplete { sequenceCompleted.complete() }
            }.onFailure { println("Test failed at sequence index ${testGroup.first}") }
        } ?: sequenceCompleted.complete()

        return sequenceCompleted.future()
    }

    protected fun notExistingPuzzle() = Random.nextInt().toString()

    protected fun notExistingTile() = Random.nextInt().toString()

    protected fun notExistingPlayer() = Random.nextBoolean().toString()

    /**
     * Clean test folder
     */
    @After fun cleanTrash() {
        File("trash").apply {
            deleteRecursively()
            mkdir()
        }
    }

    /**
     * Clean test folder and start service
     */
    @Before fun startService() {
        File("trash").apply {
            deleteRecursively()
            mkdir()
        }
        val customLock = ResultLock(false)
        val complete = Promise.promise<Void>()
        vertx.deployVerticle(Gateway(complete))
        complete.future().onComplete { customLock.set { true } }
        customLock.deadline(2000)
        assert(customLock.result) { "Service not correctly started" }
    }
}