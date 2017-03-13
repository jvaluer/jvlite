package com.petukhovsky.jvaluer.micro

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.Executors

/**
 * Created by arthur on 12.02.17.
 */

data class RunResponse(
    val status: String,
    val compilation: CompilationInfo?,
    val result: RunResult?
)

data class RunRequest(
        val src: String,
        val opt: RunOptions
)

data class RunOptions (
        val input: Resource,
        val timeLimit: Long?,
        val memoryLimit: Long?,
        val fin: String?,
        val fout: String?
)

data class RunResult(
        val output: Resource,
        val verdict: RunVerdict,
        val time: Long,
        val memory: Long,
        val exitCode: Long,
        val comment: String
)

enum class RunVerdict {
    ACCEPTED,
    WRONG_ANSWER,
    SUCCESS,
    TIME_LIMIT_EXCEEDED,
    OUTPUT_LIMIT_EXCEEDED,
    INTERNAL_ERROR,
    RUNTIME_ERROR,
    MEMORY_LIMIT_EXCEEDED,
    IDLENESS_LIMIT_EXCEEDED,
    SECURITY_VIOLATION,
    FAIL,
    CRASH,
    UNKNOWN
}