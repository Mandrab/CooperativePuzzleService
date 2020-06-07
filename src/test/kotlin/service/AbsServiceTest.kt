package test.kotlin.service

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import org.junit.Before
import service.Gateway
import java.io.File
import kotlin.random.Random

abstract class AbsServiceTest {
    protected val vertx: Vertx = Vertx.vertx()
    protected val client: WebClient = WebClient.create(vertx)

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