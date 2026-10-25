package com.example.mybase.data.document

/** Small weighted access-order cache independent of Android so eviction and identity can be tested locally. */
class BoundedValueCache<K, V>(private val maxWeight: Int, private val weightOf: (V) -> Int) {
    private val values = LinkedHashMap<K, V>(16, 0.75f, true)
    private var weight = 0
    @Synchronized fun get(key: K): V? = values[key]
    @Synchronized fun put(key: K, value: V) {
        remove(key)
        val valueWeight = weightOf(value).coerceAtLeast(0)
        if (valueWeight > maxWeight) return
        values[key] = value
        weight += valueWeight
        val iterator = values.entries.iterator()
        while (weight > maxWeight && iterator.hasNext()) {
            val entry = iterator.next()
            weight -= weightOf(entry.value).coerceAtLeast(0)
            iterator.remove()
        }
    }
    @Synchronized fun remove(key: K) { values.remove(key)?.let { weight -= weightOf(it).coerceAtLeast(0) } }
    @Synchronized fun clear() { values.clear(); weight = 0 }
    @Synchronized fun size(): Int = values.size
}
