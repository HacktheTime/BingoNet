package de.hype.bingonet.shared.compilation.processors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.*
import de.hype.bingonet.shared.compilation.Main.PROJECT_DIR
import de.hype.bingonet.shared.compilation.sbenums.NeuRepoManager
import de.hype.bingonet.shared.compilation.sbenums.minions.MinionRepoManager
import io.github.moulberry.repo.data.NEUItem
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import kotlin.io.path.absolutePathString

@OptIn(DelicateKotlinPoetApi::class, KspExperimental::class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class SBEnumGenerators(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val startTime = System.currentTimeMillis()
        try {
            // --- Caching logic start ---
            val projectRoot = PROJECT_DIR.toPath().absolutePathString()
            val cacheFile = File(Paths.get(projectRoot, "build", ".skyblockitems_cache").toString())
            val backupCacheFile = File(Paths.get(projectRoot, ".skyblockitems_cache_backup").toString())
            val metaCacheFile = File(Paths.get(projectRoot, "build", ".skyblockitems_meta").toString())
            val backupMetaCacheFile = File(Paths.get(projectRoot, ".skyblockitems_meta_backup").toString())
            var currentCommit = try {
                ProcessBuilder("git", "rev-parse", "HEAD")
                    .directory(File(projectRoot))
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader().readText().trim()
            } catch (e: Exception) {
                null
            }
//        currentCommit=null

            // Restore cache/meta from backup if build cache is missing but backup exists
            if (!cacheFile.exists() && backupCacheFile.exists()) {
                cacheFile.parentFile.mkdirs()
                backupCacheFile.copyTo(cacheFile, overwrite = true)
            }
            if (!metaCacheFile.exists() && backupMetaCacheFile.exists()) {
                metaCacheFile.parentFile.mkdirs()
                backupMetaCacheFile.copyTo(metaCacheFile, overwrite = true)
            }

            val lastCommit = cacheFile.takeIf { it.exists() }?.readText()?.trim()
            val cacheHit = currentCommit != null && currentCommit == lastCommit

            val minionpkg = "de.hype.bingonet.shared.compilation.sbenums.minions"
            val minionRegistryName = "MinionTypes"
            val minionSpec = TypeSpec.enumBuilder(minionRegistryName)

            // always include all minion types (remove cacheHit guard)
            MinionRepoManager.minionTypes.forEach {
                minionSpec.addEnumConstant(it.key)
            }

            try {
                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    minionpkg,
                    minionRegistryName,
                ).writer().use {
                    FileSpec.builder(minionpkg, minionRegistryName)
                        .addType(minionSpec.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) {
                // ignore if this file was already generated
            }

            // --- SkyblockItems splitting logic ---
            val itempkg = "de.hype.bingonet.shared.compilation.sbenums"
            val itemType = "SkyblockItems"
            val chunkSize = 200

            // --- Property/Chunk metadata ---
            data class ItemMeta(val name: String, val chunk: Int)

            val allProperties = mutableListOf<Pair<String, PropertySpec>>()
            val metaList = mutableListOf<ItemMeta>()
            var chunkObjectNames: List<String>

            if (!cacheHit) {
                // Build property list and meta
                val nameUsage = mutableMapOf<String, MutableList<Pair<String, NEUItem>>>()
                NeuRepoManager.items.forEach { (itemId, item) ->
                    var displayName = item.displayName
                        .replace("\\[Lvl (\\{LVL\\}|100)]".toRegex(), "")
                        .replace("§.".toRegex(), "")
                        .trim()
                        .replace("[\\s.:/\\[\\]\"`$]".toRegex(), "_")

                    if (displayName.equals("Enchanted_Book", true)) {
                        displayName = "Enchanted_Book_${item.skyblockItemId.split(";").first().lowercase(Locale.US)}"
                    }

                    if (displayName.equals("Attribute_Shard", true)) {
                        displayName = item.skyblockItemId.split(";").first().lowercase(Locale.US)
                    }

                    nameUsage.computeIfAbsent(displayName) { mutableListOf() }
                        .add(itemId to item)
                }

                nameUsage.forEach { (name, entries) ->
                    entries.forEach { (itemId, _) ->
                        val propertyName = if (entries.size > 1) {
                            itemId
                                .split("_", "-", ";", ".", " ")
                                .joinToString("_") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
                        } else {
                            name
                        }
                        val escapedName = if (propertyName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                            propertyName
                        } else {
                            "`$propertyName`"
                        }
                        val propertySpec = PropertySpec.builder(
                            escapedName,
                            NEUItem::class.asClassName()
                        )
                            .addModifiers(KModifier.PUBLIC)
                            .delegate("lazy { %T.items[%S]!! }", NeuRepoManager::class.asClassName(), itemId)
                            .build()
                        allProperties.add(escapedName to propertySpec)
                    }
                }
                chunkObjectNames = allProperties.chunked(chunkSize).mapIndexed { idx, _ -> "SkyblockItemsChunk$idx" }
                allProperties.forEachIndexed { idx, (name, _) ->
                    metaList.add(ItemMeta(name, idx / chunkSize))
                }
                // Save meta for cache hit usage (use tab instead of comma)
                metaCacheFile.writeText(metaList.joinToString("\n") { "${it.name}\t${it.chunk}" })
            } else {
                // On cache hit, restore meta and build property stubs
                val metaLines = metaCacheFile.takeIf { it.exists() }?.readLines() ?: emptyList()
                metaLines.forEach { line ->
                    val sep = line.lastIndexOf('\t')
                    if (sep > 0) {
                        val name = line.substring(0, sep)
                        val chunkStr = line.substring(sep + 1)
                        metaList.add(ItemMeta(name, chunkStr.toInt()))
                    }
                }
                val maxChunk = metaList.maxOfOrNull { it.chunk } ?: 0
                chunkObjectNames = (0..maxChunk).map { "SkyblockItemsChunk$it" }
                // Build stub properties (no delegates)
                metaList.forEach {
                    val propertySpec = PropertySpec.builder(
                        it.name,
                        NEUItem::class.asClassName()
                    )
                        .addModifiers(KModifier.PUBLIC)
                        .getter(FunSpec.getterBuilder().addStatement("error(\"Not available in cache mode\")").build())
                        .build()
                    allProperties.add(it.name to propertySpec)
                }
            }

            // Generate chunked objects
            val chunked = Array(chunkObjectNames.size) { mutableListOf<PropertySpec>() }
            metaList.forEachIndexed { idx, meta ->
                chunked[meta.chunk].add(allProperties[idx].second)
            }
            chunked.forEachIndexed { idx, props ->
                val chunkObj = TypeSpec.objectBuilder(chunkObjectNames[idx])
                    .addModifiers(KModifier.INTERNAL)
                props.forEach { prop -> chunkObj.addProperty(prop) }
                try {
                    environment.codeGenerator.createNewFile(
                        Dependencies.ALL_FILES,
                        itempkg,
                        chunkObjectNames[idx],
                    ).writer().use {
                        FileSpec.builder(itempkg, chunkObjectNames[idx])
                            .addType(chunkObj.build())
                            .build()
                            .writeTo(it)
                    }
                } catch (_: FileAlreadyExistsException) {
                }
            }

            // Generate central SkyblockItems object delegating to chunks
            val itemClassBuilder = TypeSpec.objectBuilder(itemType)
                .addModifiers(KModifier.PUBLIC)
            metaList.forEach {
                val chunkName = chunkObjectNames[it.chunk]
                itemClassBuilder.addProperty(
                    PropertySpec.builder(it.name, NEUItem::class.asClassName())
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %L.%L", chunkName, it.name)
                                .build()
                        )
                        .build()
                )
            }
            try {
                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    itempkg,
                    itemType,
                ).writer().use {
                    FileSpec.builder(itempkg, itemType)
                        .addType(itemClassBuilder.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) {
            }

            // At the end, update both the cache/meta files and their backups
            if (currentCommit != null) {
                cacheFile.parentFile.mkdirs()
                cacheFile.writeText(currentCommit)
                backupCacheFile.writeText(currentCommit)
                metaCacheFile.parentFile.mkdirs()
                metaCacheFile.writeText(metaList.joinToString("\n") { "${it.name}\t${it.chunk}" })
                backupMetaCacheFile.writeText(metaList.joinToString("\n") { "${it.name}\t${it.chunk}" })
            }
            return emptyList()
        } finally {
            val endTime = System.currentTimeMillis()
            println("[Timing] ModalComponentProcessor ran in ${endTime - startTime} ms")
        }
    }
}
