package cn.lyric.getter.hook.app

import cn.lyric.getter.hook.BaseHook
import cn.lyric.getter.tool.HookTools
import cn.lyric.getter.tool.HookTools.isQQLite
import cn.lyric.getter.tool.Tools.isNot

object Meizu : BaseHook() {
    override val name: String
        get() = this.javaClass.simpleName

    override fun init() {
        isQQLite {
            HookTools.QQLite()
        }.isNot {
            HookTools.MockFlyme().mock().notificationLyric()
        }
    }
}