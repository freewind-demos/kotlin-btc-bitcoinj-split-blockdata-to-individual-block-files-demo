package example

import com.subgraph.orchid.encoders.Hex
import org.bitcoinj.core.Block
import org.bitcoinj.core.Context
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import java.io.File
import java.io.FileOutputStream

val mainNetParams = MainNetParams()
val splitFilesDir = File("./split-files")
val blockChainFiles = listOf(File("./btc-data/blocks/blk00000.dat"))

val leadingMagicBytes = Hex.decode("f9beb4d91d010000")!!

fun dirNameForHeight(height: Int): String {
    return (height / 10000).toString()
}

fun buildFile(height: Int, prevHash: String?, thisHash: String): File {
    return File(splitFilesDir, "${dirNameForHeight(height)}/$height:${prevHash ?: "null"}-$thisHash.block")
}

fun writeToFile(block: Block, file: File) {
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    val byteArrayStream = FileOutputStream(file)
    byteArrayStream.write(leadingMagicBytes)
    block.bitcoinSerialize(byteArrayStream)
    byteArrayStream.close()
}

fun calcHeightByPrevHash(prevHash: String): Int {
    fun findPrevHeightHash(fileName: String): Int? {
        val (height, _, thisHash) = """(\d+):(\w+)-(\w+).block""".toRegex().matchEntire(fileName)!!.destructured
        return if (thisHash == prevHash) height.toInt() else null
    }

    val allFiles = splitFilesDir.listFiles().toList().flatMap { it.listFiles().toList() }
    val prevHeight = allFiles.asSequence().map { it.name }.map { fileName -> findPrevHeightHash(fileName) }.find { it != null }!!
    return prevHeight + 1
}

fun main(args: Array<String>) {
    Context.getOrCreate(mainNetParams)

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