package vespa.models

import java.util.OptionalInt

/**
 * Text utility functions.
 *
 * PORT: https://github.com/vespa-engine/vespa/blob/f8dfd639931905a9d6dae95f5b4e0ae812042117/vespajlib/src/main/java/com/yahoo/text/Text.java
 */
object TextHelper {
  private val allowedAsciiChars = new Array[Boolean](0x80)
  allowedAsciiChars(0x0) = false
  allowedAsciiChars(0x1) = false
  allowedAsciiChars(0x2) = false
  allowedAsciiChars(0x3) = false
  allowedAsciiChars(0x4) = false
  allowedAsciiChars(0x5) = false
  allowedAsciiChars(0x6) = false
  allowedAsciiChars(0x7) = false
  allowedAsciiChars(0x8) = false
  allowedAsciiChars(0x9) = true //tab

  allowedAsciiChars(0xA) = true //nl

  allowedAsciiChars(0xB) = false
  allowedAsciiChars(0xC) = false
  allowedAsciiChars(0xD) = true //cr
  for (i <- 0xE until 0x20) {
    allowedAsciiChars(i) = false
  }
  for (i <- 0x20 until 0x7F) {
    allowedAsciiChars(i) = true //printable ascii chars

  }
  allowedAsciiChars(0x7F) = true //del - discouraged, but allowed


  def stripIfInvalid(s: String): String = Option(s)
    .filter(f => validateTextString(f).isPresent).map(f => stripInvalidCharacters(s)).getOrElse(s)

  /**
   * Returns whether the given codepoint is a valid text character, potentially suitable for
   * purposes such as indexing and display, see http://www.w3.org/TR/2006/REC-xml11-20060816/#charsets
   */
  def isTextCharacter(codepoint: Int): Boolean = { // The link above notes that 0x7F-0x84 and 0x86-0x9F are discouraged, but they are still allowed -
    // see http://www.w3.org/International/questions/qa-controls
    if (codepoint < 0x80) return allowedAsciiChars(codepoint)
    if (codepoint < 0xFDD0) return true
    if (codepoint <= 0xFDDF) return false
    if (codepoint < 0x1FFFE) return true
    if (codepoint <= 0x1FFFF) return false
    if (codepoint < 0x2FFFE) return true
    if (codepoint <= 0x2FFFF) return false
    if (codepoint < 0x3FFFE) return true
    if (codepoint <= 0x3FFFF) return false
    if (codepoint < 0x4FFFE) return true
    if (codepoint <= 0x4FFFF) return false
    if (codepoint < 0x5FFFE) return true
    if (codepoint <= 0x5FFFF) return false
    if (codepoint < 0x6FFFE) return true
    if (codepoint <= 0x6FFFF) return false
    if (codepoint < 0x7FFFE) return true
    if (codepoint <= 0x7FFFF) return false
    if (codepoint < 0x8FFFE) return true
    if (codepoint <= 0x8FFFF) return false
    if (codepoint < 0x9FFFE) return true
    if (codepoint <= 0x9FFFF) return false
    if (codepoint < 0xAFFFE) return true
    if (codepoint <= 0xAFFFF) return false
    if (codepoint < 0xBFFFE) return true
    if (codepoint <= 0xBFFFF) return false
    if (codepoint < 0xCFFFE) return true
    if (codepoint <= 0xCFFFF) return false
    if (codepoint < 0xDFFFE) return true
    if (codepoint <= 0xDFFFF) return false
    if (codepoint < 0xEFFFE) return true
    if (codepoint <= 0xEFFFF) return false
    if (codepoint < 0xFFFFE) return true
    if (codepoint <= 0xFFFFF) return false
    if (codepoint < 0x10FFFE) return true
    if (codepoint <= 0x10FFFF) return false
    true
  }

  /**
   * Validates that the given string value only contains text characters and
   * returns the first illegal code point if one is found.
   */
  def validateTextString(string: String): OptionalInt = {
    var i = 0
    while ( {
      i < string.length
    }) {
      val codePoint = string.codePointAt(i)
      if (!TextHelper.isTextCharacter(codePoint)) return OptionalInt.of(codePoint)
      val charCount = Character.charCount(codePoint)
      if (Character.isHighSurrogate(string.charAt(i))) if (charCount == 1) return OptionalInt.of(string.codePointAt(i))
      else if (!Character.isLowSurrogate(string.charAt(i + 1))) return OptionalInt.of(string.codePointAt(i + 1))
      i += charCount
    }
    OptionalInt.empty
  }




  /** Returns a string where any invalid characters in the input string is replaced by spaces */
  def stripInvalidCharacters(string: String): String = {
    var stripped: java.lang.StringBuilder = null // lazy, as most string will not need stripping
    var i = 0
    while ( {
      i < string.length
    }) {
      val codePoint = string.codePointAt(i)
      val charCount = Character.charCount(codePoint)
      if (!TextHelper.isTextCharacter(codePoint)) stripped = mkLazy(stripped, string, i)
      else if (Character.isHighSurrogate(string.charAt(i))) if (charCount == 1) stripped = mkLazy(stripped, string, i)
      else if (!Character.isLowSurrogate(string.charAt(i + 1))) stripped = mkLazy(stripped, string, i)
      else if (stripped != null) stripped.appendCodePoint(codePoint)
      else if (stripped != null) stripped.appendCodePoint(codePoint)
      i += charCount
    }
    if (stripped != null) stripped.toString
    else string
  }

  private def mkLazy(sb0: java.lang.StringBuilder, s: String, i: Int) = {
    var sb = sb0
    if (sb == null) sb = new java.lang.StringBuilder(s.substring(0, i))
    sb.append(' ')
    sb
  }
}