package com.petukhovsky.jvaluer.micro

import com.petukhovsky.jvaluer.util.FilesUtils
import org.apache.catalina.manager.StatusTransformer.formatTime
import org.apache.commons.io.IOUtils
import org.springframework.core.io.PathResource
import org.springframework.core.io.Resource
import org.w3c.dom.Document
import java.nio.file.Files
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by arthur on 12.02.17.
 */

val hostDir = "/var/lib/jv"
val host = Paths.get(hostDir)

val wallLimitAppend = 5000L
val maxTimeLimit = 10000L

val compileTimeLimit = 60000L

val monitor = Object()

interface JExecutorService {
    fun run(
            exe: Resource,
            opt: RunOptions,
            format: MessageFormat,
            fexe: String = "exe"
    ) : RunResult

    fun compile(
            src: Resource,
            format: MessageFormat,
            fsrc: String = "src",
            fout: String = "out"
    ) : CompiledResource
}

object JValuerDocker : JExecutorService {

    val image = "arthur/runexe"

    val containerDir = "/jv"

    fun dockerPrefix(memory: Long?): String
            = "docker run --network none --rm " +
                (if (memory == null) "" else "-m " + memory + "M") +
                " -v $hostDir:$containerDir $image"

    override fun run(
            exe: Resource,
            opt: RunOptions,
            format: MessageFormat,
            fexe: String
    ) : RunResult
    = synchronized(monitor, fun(): RunResult {
        FilesUtils.assureEmptyDir(host)

        val fin = opt.fin ?: "stdin"
        val fout = opt.fout ?: "stdout"

        val hin = host.resolve(fin)
        val hout = host.resolve(fout)
        val hexe = host.resolve(fexe)

        exe.inputStream.use {
            Files.copy(it, hexe)
        }
        FilesUtils.chmod(hexe, 555)

        println(Files.size(hexe))

        opt.input.inputStream.use {
            Files.copy(it, hin)
        }
        Files.write(hout, "".lines())

        val timeLimit = opt.timeLimit ?: maxTimeLimit

        val exeResult = executeProcessOut(
                "${dockerPrefix(opt.memoryLimit)} " +
                        "runexe -t $timeLimit -d $containerDir -i $fin -o $fout -xml " +
                        format.format(arrayOf(containerDir + "/" + fexe)),
                timeLimit + wallLimitAppend
        )

        val xml = exeResult.stdout

        try {
            val doc = xmlBuilder.parse(IOUtils.toInputStream(xml, "UTF-8"))
            val verdict = doc["invocationVerdict"]
            val exitCode = doc["exitCode"].toLong()
            val userTime = doc["processorUserModeTime"].toLong()
            val kernelTime = doc["processorKernelModeTime"].toLong()
            val passedTime = doc["passedTime"].toLong()
            val consumedMemory = doc["consumedMemory"].toLong()
            val comment = doc["comment"]

            return RunResult(
                    PathResource(hout),
                    if (exitCode != 0L) {
                        RunVerdict.RUNTIME_ERROR
                    } else if (userTime > timeLimit) {
                        RunVerdict.TIME_LIMIT_EXCEEDED
                    } else RunVerdict.valueOf(verdict),
                    userTime,
                    0,
                    exitCode,
                    "Memory consumption is unknown. Output size is ${Files.size(hout)} bytes. Runexe comment: $comment"

            )
        } catch (e: Exception) {
            return RunResult(
                    PathResource(hout),
                    if (exeResult.time > timeLimit) {
                        RunVerdict.TIME_LIMIT_EXCEEDED
                    } else RunVerdict.INTERNAL_ERROR,
                    -1,
                    -1,
                    -1,
                    "Invalid runexe response. It can be caused by exceeding of memory limit or time limit"
            )
        }
    })

    override fun compile(
            src: Resource,
            format: MessageFormat,
            fsrc: String,
            fout: String
    ) : CompiledResource
    = synchronized(monitor, fun() : CompiledResource {
        FilesUtils.assureEmptyDir(host)

        val hsrc = host.resolve(fsrc)
        val hout = host.resolve(fout)

        src.inputStream.use {
            Files.copy(it, hsrc)
        }

        val startTime = System.currentTimeMillis()

        val log =
                executeProcessOut(
                        "${dockerPrefix(512)} " + format.format(
                                arrayOf(
                                        containerDir + "/" + fsrc,
                                        containerDir + "/" + fout
                                )
                        ),
                        compileTimeLimit
                )

        val endTime = System.currentTimeMillis()

        val timePassed = endTime - startTime

        val success = Files.exists(hout)

        return CompiledResource(
                if (success) {
                    PathResource(hout)
                } else {
                    null
                },
                CompilationInfo(
                        log.let {
                            String.format(
                                    "stdout:%n" +
                                    "${it.stdout}%n" +
                                    "stderr:%n" +
                                    "${it.stderr}%n" +
                                    "[Done in ${formatTime(timePassed, true)}]"
                            )
                        },
                        timePassed,
                        success
                ),
                ""
        )
    })
}

object LocalExecutorService : JExecutorService {

    override fun run(
            exe: Resource,
            opt: RunOptions,
            format: MessageFormat,
            fexe: String
    ) : RunResult
            = synchronized(monitor, fun(): RunResult {
        FilesUtils.assureEmptyDir(host)

        val fin = opt.fin ?: "stdin"
        val fout = opt.fout ?: "stdout"

        val hin = host.resolve(fin)
        val hout = host.resolve(fout)
        val hexe = host.resolve(fexe)

        exe.inputStream.use {
            Files.copy(it, hexe)
        }
        FilesUtils.chmod(hexe, 555)

        opt.input.inputStream.use {
            Files.copy(it, hin)
        }
        Files.write(hout, "".lines())

        val timeLimit = opt.timeLimit ?: maxTimeLimit

        executeProcessOut(
                hexe.toAbsolutePath().toString(),
                timeLimit + wallLimitAppend
        ).stdout

        return RunResult(
                PathResource(hout),
                RunVerdict.SUCCESS,
                0,
                0,
                0,
                "Memory consumption is unknown. Output size is ${Files.size(hout)} bytes."

        )
    })

    override fun compile(
            src: Resource,
            format: MessageFormat,
            fsrc: String,
            fout: String
    ) : CompiledResource
            = synchronized(monitor, fun() : CompiledResource {
        FilesUtils.assureEmptyDir(host)

        val hsrc = host.resolve(fsrc)
        val hout = host.resolve(fout)

        src.inputStream.use {
            Files.copy(it, hsrc)
        }

        val startTime = System.currentTimeMillis()

        val log =
                executeProcessOut(
                        format.format(
                                arrayOf(
                                        hsrc.toAbsolutePath().toString(),
                                        hout.toAbsolutePath().toString()
                                )
                        ),
                        compileTimeLimit
                )

        val endTime = System.currentTimeMillis()

        val timePassed = endTime - startTime

        val success = Files.exists(hout)

        return CompiledResource(
                if (success) {
                    PathResource(hout)
                } else {
                    null
                },
                CompilationInfo(
                        log.let {
                            String.format(
                                    "stdout:%n" +
                                            "${it.stdout}%n" +
                                            "stderr:%n" +
                                            "${it.stderr}%n" +
                                            "[Done in ${formatTime(timePassed, true)}]"
                            )
                        },
                        timePassed,
                        success
                ),
                ""
        )
    })
}

operator fun Document.get(key: String): String {
    return this.getElementsByTagName(key).item(0).textContent!!
}

val xmlFactory = DocumentBuilderFactory.newInstance()
val xmlBuilder = xmlFactory.newDocumentBuilder()

data class ExecutionResult(
        val stdout: String,
        val stderr: String,
        val time: Long
)

fun executeProcessOut(cmd: String, timeLimit: Long? = null): ExecutionResult {
    println("execute cmd: $cmd")

    val startTime = System.currentTimeMillis()

    val runtime = Runtime.getRuntime()
    val process = runtime.exec(cmd)
    if (timeLimit != null) process.waitFor(timeLimit, TimeUnit.MILLISECONDS)
    if (process.isAlive) {
        process.destroy()
        process.destroyForcibly()
    }

    val endTime = System.currentTimeMillis()

    return ExecutionResult(
            try {
                IOUtils.toString(process.inputStream, "UTF-8")
            } catch (e: Exception) {
                ""
            },
            try {
                IOUtils.toString(process.errorStream, "UTF-8")
            } catch (e: Exception) {
                ""
            },
            endTime - startTime
    )
}

val jExe = JValuerDocker