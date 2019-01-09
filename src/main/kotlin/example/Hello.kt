package example

import org.bitcoinj.core.Context
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import java.io.File

val mainNetParams = MainNetParams()
val blockChainFiles = listOf(File("./btc-data/blocks/blk00000.dat"))

fun main(args: Array<String>) {
    Context.getOrCreate(mainNetParams)

    val loader = BlockFileLoader(mainNetParams, blockChainFiles)
    for (block in loader) {
        System.out.println(block)
    }
}