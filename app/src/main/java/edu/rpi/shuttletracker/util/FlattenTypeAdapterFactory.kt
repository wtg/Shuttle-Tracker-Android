package edu.rpi.shuttletracker.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.lang.reflect.Field

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Flatten(val path: String)

/**
 * Created by Tishka17 on 18.05.2016.
 * Taken and modified from https://github.com/Tishka17/gson-flatten
 * Got from https://stackoverflow.com/a/70074686
 */
open class FlattenTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(
        gson: Gson,
        type: TypeToken<T>,
    ): TypeAdapter<T> {
        val delegateAdapter = gson.getDelegateAdapter(this, type)
        val defaultAdapter =
            gson.getAdapter(JsonElement::class.java)
        val cache: List<FlattenCacheItem> =
            buildCache(type.rawType, gson)
        return object : TypeAdapter<T>() {
            private fun setElement(
                root: JsonObject,
                path: Array<String?>,
                data: JsonElement,
            ) {
                var element: JsonElement? = root
                for (i in 0 until path.size - 1) {
                    // If the path element looks like a number..
                    var index: Int? = null
                    try {
                        index = path[i]?.let { Integer.valueOf(it) }
                    } catch (ignored: NumberFormatException) {
                    }

                    // Get the next object in the chain if it exists already
                    var jsonElement: JsonElement? = null
                    if (element is JsonObject) {
                        jsonElement = element[path[i]]
                    } else if (element is JsonArray && index != null) {
                        if (index >= 0 && index < element.size()) {
                            jsonElement = element[index]
                        }
                    } else {
                        // Failure. We can't walk any further - we don't know
                        // how to write this path. Maybe worth throwing exception?
                        continue
                    }

                    // Object didn't exist in the output already. Create it.
                    if (jsonElement == null || jsonElement == JsonNull.INSTANCE) {
                        // The next element in the chain is an array
                        jsonElement =
                            if (path[i + 1]!!.matches("^\\d+$".toRegex())) {
                                JsonArray()
                            } else {
                                JsonObject()
                            }
                        if (element is JsonObject) {
                            element.add(path[i], jsonElement)
                        } else if (index != null) {
                            element as JsonArray
                            val array = element
                            // Might need to pad the array out if we're writing an
                            // index that doesn't exist yet.
                            while (array.size() <= index) {
                                array.add(JsonNull.INSTANCE)
                            }
                            array[index] = jsonElement
                        }
                    }
                    element = jsonElement
                }
                if (element is JsonObject) {
                    element.add(path[path.size - 1], data)
                } else if (element is JsonArray) {
                    element[path[path.size - 1]?.let { Integer.valueOf(it) }!!] = data
                }
            }

            @Throws(IOException::class)
            override fun write(
                out: JsonWriter,
                value: T,
            ) {
                var res = delegateAdapter.toJsonTree(value)
                if (res.isJsonObject) {
                    val jsonObject = res.asJsonObject
                    for (cacheItem in cache) {
                        val data = jsonObject[cacheItem.name]
                        jsonObject.remove(cacheItem.name)
                        setElement(jsonObject, cacheItem.path, data)
                    }
                    res = jsonObject
                }
                gson.toJson(res, out)
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): T {
                if (cache.isEmpty()) return delegateAdapter.read(`in`)
                val rootElement = defaultAdapter.read(`in`)
                if (!rootElement.isJsonObject) return delegateAdapter.fromJsonTree(rootElement)
                val root = rootElement.asJsonObject
                for (cacheElement in cache) {
                    var element: JsonElement? = root
                    for (s in cacheElement.path) {
                        if (element != null) {
                            if (element!!.isJsonObject) {
                                element = element!!.asJsonObject[s]
                            } else if (element!!.isJsonArray) {
                                element =
                                    try {
                                        element!!.asJsonArray[Integer.valueOf(s)]
                                    } catch (e: NumberFormatException) {
                                        null
                                    } catch (e: IndexOutOfBoundsException) {
                                        null
                                    }
                            } else {
                                element = null
                                break
                            }
                        } else {
                            break
                        }
                    }
                    rootElement.asJsonObject.add(
                        cacheElement.name,
                        element,
                    )
                }
                return delegateAdapter.fromJsonTree(rootElement)
            }
        }.nullSafe()
    }

    private fun buildCache(
        root: Class<*>,
        gson: Gson,
    ): ArrayList<FlattenCacheItem> {
        val cache = ArrayList<FlattenCacheItem>()
        val fields =
            getAnnotatedFields(
                root,
                Flatten::class.java,
            )
        if (fields.isEmpty()) {
            return cache
        }
        var flatten: Flatten
        var path: String
        var cacheItem: FlattenCacheItem
        val fieldNamingStrategy = gson.fieldNamingStrategy()
        for (field in fields) {
            flatten = field.getAnnotation(Flatten::class.java)!!
            path = flatten.path
            val name = fieldNamingStrategy.translateName(field)
            cacheItem =
                FlattenCacheItem(
                    path.split("::".toRegex()).toTypedArray(),
                    name,
                )
            // check path
            for (i in 0 until cacheItem.path.size - 1) {
                if (cacheItem.path[i] == null || cacheItem.path[i]!!.length == 0) {
                    throw RuntimeException("Intermediate path items cannot be empty, found $path")
                }
            }
            val i = cacheItem.path.size - 1
            if (cacheItem.path[i] == null || cacheItem.path[i]!!.length == 0) {
                cacheItem.path[i] = cacheItem.name
            }
            cache.add(cacheItem)
        }
        return cache
    }

    protected class FlattenCacheItem(
        val path: Array<String?>,
        val name: String,
    )

    companion object {
        // Find annotated fields of the class and any superclasses
        private fun getAnnotatedFields(
            mKlass: Class<*>,
            annotationClass: Class<out Annotation?>,
        ): List<Field> {
            var klass: Class<*>? = mKlass
            val fields: MutableList<Field> = ArrayList()
            while (klass != null) {
                for (field in klass.declaredFields) {
                    if (field.isAnnotationPresent(annotationClass)) {
                        fields.add(field)
                    }
                }
                // Walk up class hierarchy
                klass = klass.superclass
            }
            return fields
        }
    }
}
