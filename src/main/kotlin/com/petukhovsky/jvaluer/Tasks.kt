package com.petukhovsky.jvaluer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.commons.checker.CheckResult
import com.petukhovsky.jvaluer.commons.checker.TokenChecker
import com.petukhovsky.jvaluer.commons.data.PathData
import com.petukhovsky.jvaluer.micro.RunVerdict
import com.petukhovsky.jvaluer.util.FastScanner
import com.petukhovsky.jvaluer.util.FilesUtils
import com.petukhovsky.jvaluer.util.TruncateUtils
import org.springframework.core.io.Resource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * Created by arthur on 21.02.17.
 */

object Tasks {
    var dir = Paths.get("/data/tasks/")
    var all = loadTasks()

    fun loadTasks() : List<Task> {
        val list = mutableListOf<Task>()
        Files.newDirectoryStream(dir).use {
            it.mapNotNullTo(list) { loadTask(it) }
        }
        return list
    }

    fun loadTask(path: Path): Task? {
        try {
            val index = path.resolve("index.json")
            if (!Files.exists(index)) return null
            val model = Files.newInputStream(index).use {
                objectMapper.readValue<TaskModel>(it)
            }
            val tests = findTests(path.resolve(model.testsDir))
            return Task(
                    path.fileName.toString(),
                    path,
                    model,
                    tests
            )
        } catch (e: Exception) {
        }
        return null
    }

    private fun findTests(dir: Path): Array<Test> {
        val names: Set<String> = Files.list(dir).map{ it.fileName }.map(Path::toString).collect(Collectors.toSet())
        val result = mutableListOf<Test>()
        for (input in names) {
            if (!input.endsWith(".in")) continue
            val name = input.dropLast(3)
            val output = name + ".out"
            if (!names.contains(output)) continue
            result.add(Test(name, dir.resolve(input), dir.resolve(output)))
        }
        result.sortBy(Test::name)
        return result.toTypedArray()
    }

    fun getTaskById(id: String): Task? {
        return all.find { it.id == id }
    }
}

val objectMapper = ObjectMapper().findAndRegisterModules()

data class Task(
        val id: String,
        val location: Path,
        val model: TaskModel,
        val tests: Array<Test>,
        val checker: Checker = StdChecker()
) {

    val statementPath: Path
        get() = this.location.resolve(model.statement.file)
    val htmlStatement: String
        get() = renderHTML()

    fun renderHTML(): String {
        if (model.statement.type == StatementType.HTML) {
            return Files.lines(statementPath).toArray().joinToString("\n")
        }
        null!! //TODO
    }
}

data class Test(
        val name: String,
        val input: Path,
        val output: Path
)

data class TaskModel(
        val type: ModelType = ModelType.STD,
        val name: String = "Unnamed task",
        val testsDir: String = "tests",
        val statement: TaskStatement = TaskStatement(),
        val points: Double = 100.0,
        val timeLimit: Long? = 2000,
        val memoryLimit: Long? = null,
        val fin: String? = null,
        val fout: String? = null
)

data class TaskStatement(
        val type: StatementType = StatementType.HTML,
        val file: String = "statement.html"
)

enum class StatementType {
    HTML, PDF
}

enum class ModelType {
    STD
}


interface Checker{
    fun check(test: Test, output: Resource): TestResult
}

class StdChecker : Checker {
    override fun check(test: Test, output: Resource): TestResult {
        val answerScanner = FastScanner(PathData(test.output))
        val outScanner = FastScanner(output.inputStream)
        var token = 0
        var correct = false

        val comment: String
        while (true) {
            val answerString = answerScanner.next()
            val outString = outScanner.next()
            if (answerString != outString) {
                if (outString == null) {
                    comment = "Expected " + TruncateUtils.truncate(answerString!!, 10) + " but reached end of file"
                } else if (answerString == null) {
                    comment = "Expected end of file, but read " + TruncateUtils.truncate(outString, 10)
                } else {
                    comment = "Expected $answerString but read $outString"
                }
                break
            }

            if (answerString == null) {
                correct = true
                comment = "ok $token tokens"
                break
            }

            ++token
        }

        answerScanner.close()
        outScanner.close()
        return TestResult(comment, if (correct) RunVerdict.ACCEPTED else RunVerdict.WRONG_ANSWER)
    }

}