package de.welt.contentapi.utils

import de.welt.contentapi.utils.Env.{Env, Live}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec

class EnvTest extends PlaySpec with TableDrivenPropertyChecks {


  "An Env" must {
    "be constructed by any notation/syntax (lower- and/or uppercase). This is mandatory in case of: 'S3' folder naming vs. URL argument" in {

      val testdata = Table(
        ("syntax", "Env"),
        Tuple2("live", Live),
        Tuple2("LIVE", Live),
        Tuple2("lIvE", Live)
      )

      forAll(testdata) { (syntax: String, env: Env) ⇒
        Env(syntax) mustBe env
      }
    }

  }

}
