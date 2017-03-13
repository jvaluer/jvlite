package com.petukhovsky.jvaluer.micro

import com.mongodb.BasicDBObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.security.MessageDigest
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * Created by arthur on 12.02.17.
 */

@Service
class CompileService(
    @Autowired val files: FileStorage,
    @Autowired val langs: LanguageService
) {

    val hashSync = AnySynchronized()

    fun compile(src: String, langId: String) : CompiledResource {
        val hash = sourceHash(src, langId)

        return hashSync.synchronizedByKey(hash, fun() : CompiledResource {
            if (files.contains(hash)) {
                val resource = files.get(hash)!!
                val meta = resource.metadata

                val result = CompiledResource(
                        resource,
                        CompilationInfo(
                                (meta.get("log") ?: "") as String,
                                (meta.get("time") ?: 0) as Long,
                                (meta.get("success") ?: false) as Boolean
                        ),
                        hash
                )
                return result
            }

            val lang = langs.findById(langId)!!

            val result = lang.compile(StringResource(src))
            val info = result.info

            val resource = CompiledResource(
                    files.store(
                            hash,
                            result.resource ?: StringResource(""),
                            BasicDBObject().apply {
                                put("log", info.log)
                                put("time", info.time)
                                put("success", info.success)
                            }
                    ),
                    info,
                    hash
            )

            return resource
        })
    }

    fun compile(hash: String): CompiledResource {
        return hashSync.synchronizedByKey(hash, fun() : CompiledResource {
            val resource = files.get(hash)!!
            val meta = resource.metadata

            val result = CompiledResource(
                    resource,
                    CompilationInfo(
                            (meta.get("log") ?: "") as String,
                            (meta.get("time") ?: 0) as Long,
                            (meta.get("success") ?: false) as Boolean
                    ),
                    hash
            )
            return result
        })
    }
}

val hexAdapter = HexBinaryAdapter()

private fun sourceHash(src: String, lang: String) : String {
    val bytes = MessageDigest.getInstance("SHA-1").digest("$lang#$src".toByteArray())
    return hexAdapter.marshal(bytes)
}

class StringResource(string : String) : ByteArrayResource(string.toByteArray())

data class CompiledResource(
        val resource: Resource?,
        val info: CompilationInfo,
        val hash: String
)

data class CompilationInfo(
        val log: String,
        val time: Long,
        val success: Boolean
)