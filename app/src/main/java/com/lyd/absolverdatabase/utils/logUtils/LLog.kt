package com.lyd.absolverdatabase.utils.logUtils

import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

import java.util.regex.Pattern
import kotlin.Exception

object LLog {
    private const val VERBOSE = 2
    private const val DEBUG = 3
    private const val INFO = 4
    private const val WARN = 5
    private const val ERROR = 6

    /**不打印日志的标志*/
    const val NONE = 1
    /**当前日志过滤等级,默认是[DEBUG],用于过滤logcat打印*/
//    var curPriority = DEBUG

    var minPrintPriority = DEBUG
    var maxPrintPriority = ERROR

    var minWritePriority = INFO
    var maxWritePriority = ERROR

    private val interceptors = mutableListOf<Interceptor<in Nothing>>()
    private val chain = Chain(interceptors)

    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    private val MAX_TAG_LENGTH = 23

    private var onetimeTag = ThreadLocal<String>()
    private var tag: String?
        get() = onetimeTag.get()?.also { onetimeTag.remove() }
        set(value) {
            onetimeTag.set(value)
        }

    private var isLoggable = true
    private var methodNameEnable :Boolean = false

    fun setDebug(isLoggable :Boolean,methodNameEnable :Boolean){
        LLog.isLoggable = isLoggable
        LLog.methodNameEnable = methodNameEnable
    }

    fun <T> addInterceptor(interceptor: Interceptor<T>, isLoggable: (T) -> Boolean = { true }) {
        addInterceptor(interceptors.size, interceptor, isLoggable)
    }

    private fun <T> addInterceptor(index :Int, interceptor: Interceptor<T>, isLoggable :(T) -> Boolean = { true }){
        interceptors.add(index,interceptor.apply {
            this.isLoggable = isLoggable
        })
    }

    fun removeInterceptor(interceptor: Interceptor<*>){
        interceptors.remove(interceptor)
    }

    @JvmStatic
    fun v(
        tag :String? = null,
        msg :Any,
        args :List<Any>? = null
    ) {
        log(tempTag = tag, priority = VERBOSE, message = msg,args = args)
    }
    @JvmStatic
    fun d(
        tag :String? = null,
        msg :Any,
        args :List<Any>? = null
    ) {
        log(tempTag = tag, priority = DEBUG, message = msg,args = args)
    }
    @JvmStatic
    fun i(
        tag :String? = null,
        msg :Any,
        args :List<Any>? = null
    ) {
        log(tempTag = tag, priority = INFO, message = msg,args = args)
    }
    @JvmStatic
    fun w(
        tag :String? = null,
        msg :Any,
        exception :Exception ?= null,
        args :List<Any>? = null
    ) {
        log(tempTag = tag, priority = WARN, message = msg, ex = exception, args = args)
    }
    @JvmStatic
    fun e(
        tag :String? = null,
        msg :Any,
        exception :Exception ?= null,
        args :List<Any>? = null
    ) {
        log(tempTag = tag, priority = ERROR, message = msg, ex = exception, args = args)
    }

    private fun log(tempTag: String?, priority :Int = VERBOSE, message :Any,ex: Exception? = null, args :List<Any>?){
        val thisTag = tempTag ?: createTag()
        var tempMsg :String? = null
        if (priority == WARN || priority == ERROR){
            if (ex != null){
                tempMsg = "${message.toString()}:\n     ${getStackTraceString(ex)}"
            }
        }
        chain.proceed(
            tag = /*tempTag?: createTag()*/thisTag, priority = priority, message = tempMsg ?: message,
            args = listOf<Any>(System.currentTimeMillis())// 现在暂时不使用args，使用的时候再把里面的参数放进来
        )
    }


    private val blackList = listOf(
        LLog::class.java.name,
        Chain::class.java.name
    )
    private fun createTag(): String {
        return tag ?: Throwable().stackTrace
            .first { it.className !in blackList }
            .let(LLog::createStackElementTag)
    }
    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        tag = if (methodNameEnable) tag + "." + element.methodName else tag
        // Tag length limit was removed in API 26.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter(256)
        val pw = PrintWriter(sw,false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    fun getLevelByInt(level :Float) :String{
        return when(level.toInt()){
            Log.VERBOSE -> "V"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"

            LLog.NONE -> "NONE"
            else -> "something wrong"
        }
    }

}