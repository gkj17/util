package cn.guankejian.util

import android.util.Log
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

const val FILENAME = "logk"

var hasBorder = true
//sealed class TYPE {
//    abstract val level: Int
//}
//object V:TYPE(){override val level = Log.VERBOSE}
//object I:TYPE(){override val level = Log.INFO}
//object W:TYPE(){override val level = Log.WARN}
//object E:TYPE(){override val level = Log.ERROR}
//object A:TYPE(){override val level = Log.ASSERT}

const val V = Log.VERBOSE
const val D = Log.DEBUG
const val I = Log.INFO
const val W = Log.WARN
const val E = Log.ERROR
const val A = Log.ASSERT

//sealed class ObjectType {
//    object TypeJson : ObjectType()
//
//    object TypeXml : ObjectType()
//    object TypeOther : ObjectType()
//}

const val TYPE_JSON = 0
const val TYPE_XML = 1

var printLevel = V


private val FILE_SEP = System.getProperty("file.separator")
private val LINE_SEP = System.getProperty("line.separator")

private const val TOP_CORNER = "┌"
private const val MIDDLE_CORNER = "├"
private const val LEFT_BORDER = "│ "
private const val BOTTOM_CORNER = "└"
private const val SIDE_DIVIDER = "────────────────────────────────────────────────────────"
private const val MIDDLE_DIVIDER = "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"
private const val TOP_BORDER = TOP_CORNER + SIDE_DIVIDER + SIDE_DIVIDER
private const val MIDDLE_BORDER = MIDDLE_CORNER + MIDDLE_DIVIDER + MIDDLE_DIVIDER
private const val BOTTOM_BORDER = BOTTOM_CORNER + SIDE_DIVIDER + SIDE_DIVIDER
private const val MAX_LEN = 1100 // fit for Chinese character


class Config(
    var tag:String="",
    var detail:Boolean = true
)

fun Any?.logV(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(V,config,this,*params)
}

fun Any?.logD(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(D,config,this,*params)
}

fun Any?.logI(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(I,config,this,*params)
}
fun Any?.logW(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(W,config,this,*params)
}

fun Any?.logE(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(E,config,this,*params)
}

fun Any?.logA(vararg params:Any?,config: Config.() -> Unit = {}) {
    log(A,config,this,*params)
}

//fun Any?.logERuntimeException(msg: Any = "") {
//    if (printLevel <= Log.ERROR) {
//        Log.e("", msg.toString(), RuntimeException(msg.toString()))
//    }
//}


private fun log(level: Int, config: Config.() -> Unit = {}, vararg objects:Any?) {
    if(printLevel > level)
        return
    val configObject = Config().apply(config)
    val tag = configObject.tag
    val detail = configObject.detail

    val tagHead = processTagAndHead(tag)
    val body = processBody(level,*objects)
    print2Console(level,tagHead.tag,tagHead.consoleHead,detail,body)
}

class TagHead internal constructor(
    var tag: String,
    var consoleHead: Array<String>?,
    var fileHead: String
)

fun processTagAndHead(tag:String,hasHead:Boolean = true): TagHead {
    val stackTrace = Throwable().stackTrace
    val targetElement = stackTrace.filter { !it.className.lowercase().contains(FILENAME.lowercase()) }.first()
    val tagName:String = targetElement.className.run { this.substring(this.lastIndexOf(".")+1) }
    if (!hasHead) {
        return TagHead(tagName, null,": ")
    }else{
        val lineNumber = targetElement.lineNumber
        val threadName = Thread.currentThread().name
        val className = tagName
        val methodName = targetElement.methodName
        val fileName = targetElement.fileName

        val head = "ThreadName：${threadName} │ ClassName：${className} │ MethodName：${methodName}\n${LEFT_BORDER}Location：${targetElement.className}.${methodName}(${fileName}:${lineNumber})"

        val fileHead = " [$head]: "
        return TagHead(tag.ifEmpty { tagName }, arrayOf(head), fileHead)
    }
}

private fun formatObject(type: Int, any: Any?): String {
    if (any == null) return "null"
    return when(type){
        TYPE_JSON ->formatXml(any.toString())
        TYPE_XML -> formatXml(any.toString())
        else -> any.toString()
    }
}

private fun formatXml(inputXml: String): String {
    var outputXml: String = inputXml
    try {
        val xmlInput: Source = StreamSource(StringReader(inputXml))
        val xmlOutput = StreamResult(StringWriter())
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(xmlInput, xmlOutput)
        outputXml = xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">$LINE_SEP")
    } catch (e: Exception) {
        e.stackTraceToString().logE()
    }
    return outputXml
}

private fun processBody(type: Int, vararg contents: Any?): String {
    val body = if (contents.size == 1) {
        formatObject(type,contents[0])
    } else {
        val sb = StringBuilder()
        var i = 0
        val len = contents.size
        while (i < len) {
            val content = contents[i]
            sb.append("args")
                .append("[")
                .append(i)
                .append("]")
                .append(" = ")
                .append(formatObject(type,content))
                .append(LINE_SEP)
            ++i
        }
        sb.toString()
    }
    return body.ifEmpty { "" }
}


private fun print2Console(type: Int, tag: String, msg: String) {
    Log.println(type, tag, msg)
}
private fun print2Console(
    type: Int,
    tag: String,
    head: Array<String>?,
    detail:Boolean,
    msg: String
) {

    printBorder(type, tag, true)
    if(detail)
        printHead(type, tag, head)
    printMsg(type, tag, msg)
    printBorder(type, tag, false)
}


private fun printBorder(type: Int, tag: String, isTop: Boolean) {
    print2Console(
        type,
        tag,
        if (isTop) TOP_BORDER else BOTTOM_BORDER
    )
}

private fun printHead(type: Int, tag: String, head: Array<String>?) {
    if (head != null) {
        for (aHead in head) {
            print2Console(type,tag,if (hasBorder) LEFT_BORDER + aHead else aHead)
        }
        if (hasBorder) print2Console(type,tag,MIDDLE_BORDER)
    }
}

private fun printMsg(type: Int, tag: String, msg: String) {
    val len = msg.length
    val countOfSub = len / MAX_LEN
    if (countOfSub > 0) {
        var index = 0
        for (i in 0 until countOfSub) {
            printSubMsg(type, tag, msg.substring(index, index + MAX_LEN))
            index += MAX_LEN
        }
        if (index != len) {
            printSubMsg(type, tag, msg.substring(index, len))
        }
    } else {
        printSubMsg(type, tag, msg)
    }
}

private fun printSubMsg(type: Int, tag: String, msg: String) {
    if (!hasBorder) {
        print2Console(type, tag, msg)
        return
    }
    val lines = msg.split(LINE_SEP.toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()
    for (line in lines) {
        print2Console(type, tag, LEFT_BORDER + line)
    }
}
