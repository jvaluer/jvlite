package com.petukhovsky.jvaluer

import com.petukhovsky.jvaluer.commons.test.TestVerdict
import com.petukhovsky.jvaluer.micro.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.PathResource
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.time.Instant
import java.util.stream.Stream

/**
 * Created by arthur on 21.02.17.
 */
@Document
data class Submit(
        @Id var id: String? = null,
        var author: String = "",
        var task: String = "",
        var completed: Boolean = false,
        var tests: LinkedHashMap<String, TestResult> = LinkedHashMap(),
        var status: String = "В очереди",
        var points: Double = 0.toDouble(),
        var maxTime: Long = 0L,
        var lang: String? = null,
        var exeId: String? = null,
        @Indexed(unique = true) var num: Int = 0,
        var submitted: Instant = Instant.now(),
        var src: String = "",
        var compilationLog: String = ""
)

data class TestResult(
        var comment: String = "",
        var verdict: RunVerdict = RunVerdict.UNKNOWN,
        var time: Long = 0,
        var exitCode: Long = 0
)

data class TestResultWithName(
        val name: String,
        val result: TestResult
)

@Service
open class TestingService @Autowired constructor(
        val submitRepo: SubmitRepository,
        val compileService: CompileService,
        val languageService: LanguageService
) {

    val monitor = Object()
    val executor = SimpleAsyncTaskExecutor()

    val testingLoop = executor.execute {
        while (true) {
            try {
                synchronized(monitor) {
                    val submit = submitRepo.findByCompleted(false)
                    if (submit == null) {
                        monitor.wait(10000)
                        return@synchronized
                    }
                    if (submit.exeId == null) {
                        submit.status = "Компилируется"
                        submitRepo.save(submit)
                        val res = compileService.compile(submit.src, submit.lang!!)
                        submit.compilationLog = res.info.log
                        if (!res.info.success) {
                            submit.status = "Ошибка компиляции"
                            submit.completed = true
                        } else {
                            submit.status = "Ожидание выполнения"
                            submit.exeId = res.hash
                        }
                        submitRepo.save(submit)
                        return@synchronized
                    }
                    val task = Tasks.getTaskById(submit.task)!!
                    val pointsForTest: Double = if (task.tests.isEmpty()) 0.0 else task.model.points / task.tests.size
                    val lang = languageService.findById(submit.lang!!)!!
                    for (test in task.tests) {
                        val name = test.name
                        if (submit.tests[name] != null) continue

                        submit.status = "Выполняется на $name"
                        submitRepo.save(submit)

                        var run = lang.run(
                                compileService.compile(submit.src, submit.lang!!).resource!!,
                                RunOptions(
                                    PathResource(test.input),
                                    task.model.timeLimit,
                                    task.model.memoryLimit,
                                    task.model.fin,
                                    task.model.fout
                                )
                        )

                        submit.maxTime = maxOf(submit.maxTime, run.time)

                        val testResult: TestResult

                        if (run.verdict != RunVerdict.SUCCESS) {
                            testResult = TestResult(run.comment, run.verdict)
                        } else {
                            val check = task.checker.check(test, run.output)
                            if (check.verdict == RunVerdict.ACCEPTED) submit.points += pointsForTest
                            testResult = check
                        }
                        testResult.time = run.time
                        testResult.exitCode = run.exitCode

                        submit.tests[name] = testResult

                        submitRepo.save(submit)
                    }

                    submit.status = "Полное решение"

                    for ((key, value) in submit.tests) {
                        if (value.verdict != RunVerdict.ACCEPTED) {
                            submit.status = value.verdict.toString() + " на тесте $key"
                            break
                        }
                    }
                    submit.completed = true
                    submitRepo.save(submit)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submit(author: String, task: String, lang: String, src: String): ResponseEntity<String> {
        languageService.findById(lang)!!
        submitRepo.save(Submit(
                author = author,
                task = task,
                lang = lang,
                num = getNum(),
                src = src
        ))
        executor.execute {
            synchronized(monitor) {
                monitor.notifyAll()
            }
        }
        return ResponseEntity.ok("all ok")
    }

    fun getNum(): Int {
        return (submitRepo.findFirstByOrderByNumDesc()?.num ?: 0) + 1
    }

    fun buildTable(): ResultTable {
        val tasks = Tasks.all.map(Task::id)
        val n = tasks.size
        val subs = submitRepo.findAll()
        val rows
          = subs.groupBy { it.author }
                .mapValues {
                    it.value.groupBy { it.task }
                            .mapValues { it.value.maxBy { it.points }!!.points }
                            .let { resultToArray(it, tasks) }
                }
                .map {
                    ResultRow(
                            it.key,
                            it.value,
                            it.value.sum()
                    )
                }
                .sortedByDescending { it.sum }
        return ResultTable(tasks.size, tasks.toTypedArray(), rows.toTypedArray())
    }

    private fun resultToArray(map: Map<String, Double>, tasks: List<String>): Array<Double> {
        val result = Array(tasks.size, { 0.0 })
        println(map)
        map.mapKeys { tasks.indexOf(it.key) }
           .filter { it.key != -1 }
           .forEach { k, v -> result[k] = v }
        return result
    }


}

interface SubmitRepository : MongoRepository<Submit, String> {
    fun findFirstByOrderByNumDesc(): Submit?
    fun findByAuthor(author: String): List<Submit>
    fun findByCompleted(completed: Boolean): Submit?
    fun findByNum(num: Int): Submit?
}