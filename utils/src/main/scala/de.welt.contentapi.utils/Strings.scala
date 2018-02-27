package de.welt.contentapi.utils

import com.google.common.base.CharMatcher

trait Strings {

  private val charMatcher = CharMatcher.whitespace()

  /**
    * remove all whitespaces from the string (such as {{{"\u00A0", " ", "\t", "\n"}}})
    */
  val stripWhiteSpaces: (String) ⇒ String = (s: String) ⇒ charMatcher.removeFrom(s)

  /**
    * `true` if `s` contains only whitespaces
    * <br/> `false` otherwise
    */
  val containsOnlyWhitespaces: (String) ⇒ Boolean = (s: String) ⇒ charMatcher.matchesAllOf(s)

  /**
    * `true` if `s` contains any non-whitespaces
    * <br/> `false` otherwise
    */
  val containsTextContent: (String) ⇒ Boolean = (s: String) ⇒ !containsOnlyWhitespaces(s)

}

object Strings extends Strings
