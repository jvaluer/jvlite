package com.petukhovsky.jvaluer.micro

import com.mongodb.DBObject
import com.mongodb.client.gridfs.model.GridFSFile
import org.bson.Document
import org.springframework.core.io.Resource
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Service
import java.io.InputStream

/**
 * Created by arthur on 12.02.17.
 */
@Service
class FileStorage(
        val gridFs : GridFsTemplate
) {

    fun contains(hash: String) : Boolean {
        return get(hash) != null
    }

    fun store(hash: String, resource: Resource, meta: DBObject? = null, contentType: String? = null): MGridFsResource {
        val file = gridFs.store(resource.inputStream, hash, contentType, meta)
        return get(hash)!!
    }

    fun get(hash: String): MGridFsResource? {
        return gridFs.getMResource(hash)
    }
}

fun GridFsTemplate.getMResource(filename: String): MGridFsResource? {
    val file = findOne(query(whereFilename().`is`(filename)))
    return if (file != null) MGridFsResource(file, this.getResource(filename).inputStream) else null
}

class MGridFsResource(
        val gridFile: GridFSFile,
        inputStream: InputStream

) : GridFsResource(gridFile, inputStream) {

    val metadata : Document
        get() = gridFile.metadata

}