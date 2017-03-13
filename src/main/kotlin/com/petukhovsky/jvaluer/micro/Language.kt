package com.petukhovsky.jvaluer.micro

import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.text.MessageFormat

/**
 * Created by arthur on 12.02.17.
 */
@Service
class LanguageService {
    val all = listOf(
        Lang("cpp", "g++ -O2 -std=c++14 {src} -o {out}", srcFile = "a.cpp", exeFile = "a.out"),
        Lang("pabcnet", "pabcnetc {src} {out}", "mono {exe}", srcFile = "src.pas", exeFile = "a.exe"),
        Lang("cpp11", "g++ -O2 -std=c++11 {src} -o {out}", srcFile = "a.cpp", exeFile = "a.out"),
        Lang("fpc", "fpc -O2 {src}", srcFile = "a.pas", exeFile = "a")
    )

    val map = all.associateBy(Lang::id)

    fun findById(langId: String): Language? = map[langId]
}

abstract class Language(
        val id: String
) {
    abstract fun compile(src: Resource) : CompiledResource
    abstract fun run(exe: Resource, opt: RunOptions) : RunResult
}

class Lang(
        id: String,
        compilePattern: String,
        executePattern: String = "{exe}",
        val exeFile: String = "exe",
        val srcFile: String = "src"
) : Language(id) {

    val compileFormat = MessageFormat(
        compilePattern
            .replace("{src}", "{0}")
            .replace("{out}", "{1}")
    )

    val executeFormat = MessageFormat(
        executePattern
            .replace("{exe}", "{0}")
    )

    override fun compile(src: Resource) : CompiledResource
            = jExe.compile(src, compileFormat, srcFile, exeFile)

    override fun run(exe: Resource, opt: RunOptions) : RunResult
            = jExe.run(exe, opt, executeFormat, exeFile)

}

