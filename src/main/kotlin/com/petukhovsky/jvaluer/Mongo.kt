package com.petukhovsky.jvaluer

import com.mongodb.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.gridfs.GridFsTemplate

/**
 * Created by arthur on 12.02.17.
 */

@Configuration
open class MongoConfig : AbstractMongoConfiguration() {
    @Bean override fun mongo() = MongoClient("mongo")
    override fun getDatabaseName() = "jv-lite"

    open @Bean fun gridFsTemplate() = GridFsTemplate(mongoDbFactory(), mappingMongoConverter())
}