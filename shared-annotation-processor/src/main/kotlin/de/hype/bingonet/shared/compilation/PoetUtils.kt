package de.hype.bingonet.shared.compilation

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.javapoet.ClassName as JClassName
import com.squareup.javapoet.ParameterizedTypeName as JParameterizedTypeName
import com.squareup.javapoet.TypeName as JTypeName
import com.squareup.javapoet.WildcardTypeName as JWildcardTypeName

// Convert JClassName to KotlinPoet ClassName using public factory functions.
fun JClassName.toKotlinTypeName(): ClassName {
    val names = simpleNames().toList()
    if (names.isEmpty()) error("JClassName must have at least one simple name")
    // Create outer class then nest subsequent classes.
    return names.drop(1).fold(ClassName(packageName(), names.first())) { acc, name ->
        acc.nestedClass(name)
    }
}

// Convert JWildcardTypeName to KotlinPoet TypeName.
fun JWildcardTypeName.toKotlinTypeName(): TypeName {
    return when {
        upperBounds.isNotEmpty() -> WildcardTypeName.producerOf(upperBounds[0].toKotlinTypeName())
        lowerBounds.isNotEmpty() -> WildcardTypeName.consumerOf(lowerBounds[0].toKotlinTypeName())
        else -> STAR
    }
}

// Generic conversion function.
fun JTypeName.toKotlinTypeName(): TypeName = when (this) {
    is JClassName -> toKotlinTypeName()
    is JParameterizedTypeName -> toKotlinTypeName()
    is JWildcardTypeName -> toKotlinTypeName()
    else -> error("Unsupported TypeName conversion: $this")
}

fun JParameterizedTypeName.toKotlinTypeName(): ParameterizedTypeName {
    val kotlinRawType = rawType.toKotlinTypeName()
    val kotlinTypeArgs = typeArguments.map { arg ->
        when (arg) {
            is JParameterizedTypeName -> arg.toKotlinTypeName()
            is JWildcardTypeName -> if (arg.toString() == "?") STAR else arg.toKotlinTypeName()
            else -> arg.toKotlinTypeName()
        }
    }.toTypedArray()
    return kotlinRawType.parameterizedBy(*kotlinTypeArgs)
}

fun Resolver.resolveKSType(from: ClassName): KSType {
    // 1) Build the fully-qualified string, e.g. "com.example.MyClass"
    val fqName = buildString {
        append(from.packageName)
        append('.')
        append(from.simpleNames.joinToString("."))
    }

    // 2) Ask KSP to find the class declaration by that name
    val ksName = this.getKSNameFromString(fqName)
    val decl = this.getClassDeclarationByName(ksName) ?: error("Class not found: $fqName")

    // 3) Ask for its default star-projected type (you can also do .asType(listOf(...)) to fill type args)
    return decl.asStarProjectedType()
}

fun Resolver.resolveKSType(
    typeName: ParameterizedTypeName
): KSType {
    // Resolve the raw type first
    val baseType = this.resolveKSType(typeName.rawType)
    // If there are no type arguments, return the base type as is.
    if (typeName.typeArguments.isEmpty()) {
        return baseType
    }
    // For parameterized types we use the star-projected type since mapping type arguments is not supported.
    val fqName = buildString {
        append(typeName.rawType.packageName)
        append('.')
        append(typeName.rawType.simpleNames.joinToString("."))
    }
    val ksDeclaration = this.getClassDeclarationByName(fqName)
        ?: error("Declaration for $fqName not found")
    return ksDeclaration.asStarProjectedType()
}