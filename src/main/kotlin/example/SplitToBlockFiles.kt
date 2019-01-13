package example

import com.subgraph.orchid.encoders.Hex
import org.bitcoinj.core.Block
import org.bitcoinj.core.Context
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import java.io.File
import java.io.FileOutputStream

val mainNetParams = MainNetParams().apply { Context.getOrCreate(this) }

//val splitFilesDir = File("./split-files")
//val blockChainFiles = listOf(File("./btc-data/blocks/blk00000.dat"))
val splitFilesDir = File("/Volumes/Mac2/Downloads/bitcoin-block-files")
val allBlockchainFiles = File("/Volumes/Mac2/Downloads/Library/Application Support/Bitcoin/blocks").listFiles().filter { it.name.startsWith("blk") }

val blockChainFiles = removeHandledFiles(allBlockchainFiles)

fun removeHandledFiles(allBlockFiles: List<File>): List<File> {
    val allSplitFiles = findAllIndividualBlockFiles(splitFilesDir)
    val handledFiles = allBlockFiles.takeWhile { file ->
        println("check handled file: $file")
        val loader = BlockFileLoader(mainNetParams, listOf(file))
        val block = loader.next()
        val height = findBlockHeightByHash(allSplitFiles, block.hashAsString)
        height != null
    }

    return if (handledFiles.isEmpty()) allBlockFiles else {
        val startFile = handledFiles.last()
        allBlockFiles.dropWhile { it != startFile }
    }
}

fun findAllIndividualBlockFiles(dir: File): List<File> {
    return dir.listFiles().toList().flatMap { it.listFiles().toList() }
}

val leadingMagicBytes = Hex.decode("f9beb4d91d010000")!!

fun dirNameForHeight(height: Int): String {
    return (height / 10000).toString()
}

fun buildFile(height: Int, prevHash: String?, thisHash: String): File {
    return File(splitFilesDir, "${dirNameForHeight(height)}/$height:${prevHash ?: "null"}-$thisHash.block")
}

fun writeToFile(block: Block, file: File) {
    println("write to: $file")
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    val byteArrayStream = FileOutputStream(file)
    byteArrayStream.write(leadingMagicBytes)
    block.bitcoinSerialize(byteArrayStream)
    byteArrayStream.close()
}

fun findBlockHeightByHash(allFiles: List<File>, hash: String): Int? {
    fun findBlockHeightFromFile(fileName: String, hash: String): Int? {
        val (height, _, thisHash) = """(\d+):(\w+)-(\w+).block""".toRegex().matchEntire(fileName)!!.destructured
        return if (thisHash == hash) height.toInt() else null
    }

    val height = allFiles.asSequence().map { it.name }.map { fileName -> findBlockHeightFromFile(fileName, hash) }.find { it != null }
    return height
}

fun calcHeightByPrevHash(prevHash: String): Int {
    val allFiles = findAllIndividualBlockFiles(splitFilesDir)
    val prevHeight = findBlockHeightByHash(allFiles, prevHash)
    return prevHeight!! + 1
}

fun main(args: Array<String>) {
    val loader = BlockFileLoader(mainNetParams, blockChainFiles)

    var height: Int? = null

    for (block in loader) {
        // genesis block
        if (block.prevBlockHash.toString() == "0000000000000000000000000000000000000000000000000000000000000000") {
            height = 0
            writeToFile(block, buildFile(0, null, block.hashAsString))
        } else {
            if (height == null) {
                height = calcHeightByPrevHash(block.prevBlockHash.toString())
            }
            writeToFile(block, buildFile(height, block.prevBlockHash.toString(), block.hashAsString))
        }
        height += 1
    }
}