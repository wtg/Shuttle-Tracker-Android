package edu.rpi.shuttletracker.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okio.IOException
import java.lang.reflect.Field

/**
 * credits to https://github.com/Tishka17/gson-flatten for inspiration
 * Author: A$CE
 * getting item from a nested json
 * solution from https://stackoverflow.com/a/68898339
 */

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Flatten(val path: String)

class FlattenTypeAdapterFactory(
    private val pathDelimiter: String = ".",
) : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegateAdapter = gson.getDelegateAdapter(this, type)
        val defaultAdapter = gson.getAdapter(JsonElement::class.java)
        val flattenedFieldsCache = buildFlattenedFieldsCache(type.rawType)

        return object : TypeAdapter<T>() {

            @Throws(IOException::class)
            override fun read(reader: JsonReader): T {
                // if this class has no flattened fields, parse it with regular adapter
                if (flattenedFieldsCache.isEmpty()) {
                    return delegateAdapter.read(reader)
                }
                // read the whole json string into a jsonElement
                val rootElement = defaultAdapter.read(reader)
                // if not a json object (array, string, number, etc.), parse it
                if (!rootElement.isJsonObject) {
                    return delegateAdapter.fromJsonTree(rootElement)
                }
                // it's a json object of type T, let's deal with it
                val root = rootElement.asJsonObject
                // parse each field
                for (field in flattenedFieldsCache) {
                    var element: JsonElement? = root
                    // dive down the path to find the right element
                    for (node in field.path) {
                        // can't dive down null elements, break
                        if (element == null) break
                        // reassign element to next node down
                        element = when {
                            element.isJsonObject -> element.asJsonObject[node]
                            element.isJsonArray -> try {
                                element.asJsonArray[node.toInt()]
                            } catch (e: Exception) { // NumberFormatException | IndexOutOfBoundsException
                                null
                            }
                            else -> null
                        }
                    }
                    // lift deep element to root element level
                    root.add(field.name, element)
                    // this keeps nested element un-removed (i suppose for speed)
                }
                // now parse flattened json
                return delegateAdapter.fromJsonTree(root)
            }

            override fun write(out: JsonWriter, value: T) {
                throw UnsupportedOperationException()
            }
        }.nullSafe()
    }

    // build a cache for flattened fields's paths and names (reflection happens only here)
    private fun buildFlattenedFieldsCache(root: Class<*>): Array<FlattenedField> {
        // get all flattened fields of this class
        var clazz: Class<*>? = root
        val flattenedFields = ArrayList<Field>()

        while (clazz != null) {
            clazz.declaredFields.filterTo(flattenedFields) {
                it.isAnnotationPresent(Flatten::class.java)
            }
            clazz = clazz.superclass
        }

        if (flattenedFields.isEmpty()) {
            return emptyArray()
        }
        val delimiter = pathDelimiter
        return Array(flattenedFields.size) { i ->
            val ff = flattenedFields[i]
            val a = ff.getAnnotation(Flatten::class.java)!!
            val nodes = a.path.split(delimiter)
                .filterNot { it.isEmpty() } // ignore multiple or trailing dots
                .toTypedArray()
            FlattenedField(ff.name, nodes)
        }
    }

    private class FlattenedField(val name: String, val path: Array<String>)
}
