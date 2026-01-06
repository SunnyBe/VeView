package com.veview.veviewsdk.annotations

/**
 * Attaches a description to a data class field, intended to be used by an AI
 * analysis engine to understand the field's purpose when generating JSON.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class FieldDescription(
    val text: String,
    val type: JsonType = JsonType.STRING
)

/**
 * An enum representing the supported JSON types for AI model instructions.
 */
enum class JsonType {
    STRING,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    LIST_OF_STRINGS,
    LIST_OF_INTEGERS
}
