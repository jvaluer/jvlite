package com.petukhovsky.jvaluer.micro

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by arthur on 12.02.17.
 */

class AnySynchronized {

    val map = ConcurrentHashMap<Any, Any>()

    fun<R> synchronizedByKey(key: Any, block: () -> R) : R {
        return synchronized(map.getOrPut(key, { Any() }), block)
    }
}