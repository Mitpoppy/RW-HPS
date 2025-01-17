/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.asm.api

fun interface Redirection {
    @Throws(Throwable::class)
    operator fun invoke(obj: Any, desc: String, type: Class<*>, vararg args: Any?): Any?

    companion object {
        fun of(value: Any?): Redirection {
            return Redirection { _: Any, _: String, _: Class<*>, _: Array<out Any?> -> value }
        }

        const val CAST_PREFIX = "<cast> "
        const val METHOD_NAME = "invoke"
        const val METHOD_DESC = "(Ljava/lang/Object;Ljava/lang/String;" + "Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;"
    }
}
