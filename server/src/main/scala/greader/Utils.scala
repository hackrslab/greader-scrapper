package greader

import java.security.MessageDigest
import java.util.Date
import java.net.URL
import java.nio.ByteBuffer

import greader.domain._

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import org.owasp.html._

object Utils {
  private val chars = Array[Char](
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
    'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'
  )
  private val rchars = Map(
    (0 until chars.size).zip(chars).map(i => (i._2 -> i._1)): _*
  )
  val epoch = 725641200
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def encodeURL(url: String) = {
    val u = new URL(url)
    val port = if (u.getPort == -1) "" else ":%d".format(u.getPort)
    val path = if (u.getPath == "") "/" else u.getPath
    val query = if (u.getQuery == null) "" else "?%s".format(u.getQuery)

    "%s%s%s%s".format(u.getHost.split("\\.").reverse.mkString("."), port, path, query)
  }

  def now() = System.currentTimeMillis()

  def md5(value: String) = MessageDigest.getInstance("MD5").digest(value.getBytes)

  def signature(item: Item) = {
    val categories = if (item.categories == null) "" else item.categories.mkString(",")
    val s = "%s:%s:%s".format(categories, item.title, item.content.content)
    val sb = new StringBuilder()
    for (b <- MessageDigest.getInstance("MD5").digest(s.getBytes)) {
      val hex = Integer.toHexString(0xFF & b)
      if (hex.length == 1) {
        sb.append(0)
      }
      sb.append(hex)
    }
    sb.toString
  }

  // "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "blockquote"
  // "b", "i", "font", "s", "u", "o", "sup", "sub", "ins", "del", "strong", "strike", "tt", "code", "big", "small", "br", "span"

  val policy = new HtmlPolicyBuilder().
      allowElements("a", "table", "tr", "th", "td", "thead", "tbody").
      allowCommonBlockElements(). 
      allowCommonInlineFormattingElements().
      allowAttributes("href").onElements("a").
      allowStandardUrlProtocols().
      toFactory()

  def sanitize(text: String): String = {
    val sb = new java.lang.StringBuilder()
    HtmlSanitizer.sanitize(text, policy.apply(HtmlStreamRenderer.create(sb, null)))
    sb.toString()
  }

  def unique(first: String): Integer = {
    val fbytes = md5(first)
    val buffer = ByteBuffer.allocate(4)
    buffer.put((fbytes(0)&0x7f).asInstanceOf[Byte]).put(fbytes, 1, 3).rewind
    buffer.getInt
  }

  def unique(first: String, second: String): Long = {
    val fbytes = md5(first)
    val sbytes = md5(second)
    val buffer = ByteBuffer.allocate(8)
    buffer.put((fbytes(0)&0x7f).asInstanceOf[Byte]).put(fbytes, 1, 3).put(sbytes, 4, 4).rewind
    buffer.getLong
  }

  def unique(first: Long, second: String): Long = {
    val bytes = md5(second)
    val buffer = ByteBuffer.allocate(8)
    buffer.putInt(first.toInt - epoch).put(bytes, 4, 4).rewind
    buffer.getLong
  }

  def encode(value: Int): Seq[Char] = encode(value.toLong)

  def encode(value: String): Seq[Char] = encode(value.toLong)

  def encode(value: Long, result: Seq[Char] = Seq()): Seq[Char] = {
    if (value == 0) result
    else (value / 62) match {
      case v if v > 0 => encode(v, chars((value%62).toInt) +: result)
      case v => encode(0, chars((value%62).toInt) +: result)
    }
  }

  def pow(n: Long, c: Int, acc: Long = 1): Long = {
    c match {
      case 0 => 1L
      case 1 => acc*n
      case x => pow(n, x-1, acc*n)
    }
  }

  def decode(chars: Seq[Char], acc: Long = 0L): Long = {
    chars match {
      case Nil => acc
      case head :: Nil => decode(Nil, rchars(head) + acc)
      case head :: tail => decode(tail, rchars(head) * pow(62, tail.size) + acc)
    }
  }

  def loads[T](value: String, `type`: Class[T]): T = {
    mapper.readValue(value, `type`)
  }

  def dumps(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def timestamp(value: Date, defaultValue: Date = null): Long = {
    if (value == null || value.getTime() == 0L) {
      if (defaultValue == null) {
        0L
      } else {
        timestamp(defaultValue)
      }
    } else {
      value.getTime() / 1000
    }
  }

}
