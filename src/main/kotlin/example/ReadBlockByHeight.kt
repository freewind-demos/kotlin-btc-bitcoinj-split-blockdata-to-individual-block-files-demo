package example

import org.bitcoinj.core.Block
import org.bitcoinj.core.Context
import org.bitcoinj.utils.BlockFileLoader
import java.io.File

fun readBlockByHeight(height: Int): Block {
    val dir = dirNameForHeight(height)
    val file = File("./split-files/$dir").listFiles().find { it.name.startsWith("$height:") }
    val loader = BlockFileLoader(mainNetParams, listOf(file))
    return loader.next()
}

fun main(args: Array<String>) {
    Context.getOrCreate(mainNetParams)
    val block = readBlockByHeight(100)
    println(block)
}