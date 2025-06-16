package de.hype.bingonet.shared.compilation

import java.io.File

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
//        unused
    }

    val PROJECT_DIR by lazy {
        var base = System.getProperty("user.dir")
        if (base.endsWith("/main")) {
            base = base.substringBefore("/main")
        } else if (base.endsWith("/fabric") || base.endsWith("/fabric/run")) {
            base = base.substringBefore("/fabric")
        } else if (base.endsWith("/common") || base.endsWith("/common/run")) {
            base = base.substringBefore("/common")
        }
        return@lazy File(base)
    }
}
