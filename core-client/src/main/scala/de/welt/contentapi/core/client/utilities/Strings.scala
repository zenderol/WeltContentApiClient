package de.welt.contentapi.core.client.utilities

import com.google.common.base.CharMatcher

trait Strings {

  private val charMatcher = CharMatcher.WHITESPACE

  /**
    * remove all whitespaces from the string (such as {{{"\u00A0", " ", "\t", "\n"}}})
    */
  val stripWhiteSpaces = (s: String) ⇒ charMatcher.removeFrom(s)

  /**
    * `true` if `s` contains only whitespaces
    * <br/> `false` otherwise
    */
  val containsOnlyWhitespaces = (s: String) ⇒ charMatcher.matchesAllOf(s)

  /**
    * `true` if `s` contains any non-whitespaces
    * <br/> `false` otherwise
    */
  val containsTextContent = (s: String) ⇒ !containsOnlyWhitespaces(s)

}

object Strings extends Strings
