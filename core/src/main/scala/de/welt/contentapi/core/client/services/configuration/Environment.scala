package de.welt.contentapi.core.client.services.configuration

import com.typesafe.config.{Config, ConfigFactory}
import de.welt.contentapi.utils.Loggable
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Success, Try}

object Environment extends Loggable {
  type Provider = Map[String, String]

  private[configuration] def extract(provider: Provider, key: String, default: String): String = provider.getOrElse(key, default)

  private[configuration] def extract(provider: Provider, key: String): Option[String] = provider.get(key)

  /**
    * We expect a mode to be provided via the resource:config.resources, which
    * may accept overrides from environment variables (ENV.MODE)
    *
    * content_api {
    * mode = "dev"
    * mode = ${?MODE}
    * }
    *
    * This is required to allow loading env-dependent configs from ssm.
    *
    * Also, there should be a dedicated test config that will be loaded during testing, provide it
    * via: `-Dconfig.resource=application.test.conf` to the JVM
    */
  private val envFromConfigFile: Provider ⇒ String = provider ⇒
    extract(provider, "config.resource")
      .orElse(Some("application.conf"))
      .map(resourceName ⇒ ConfigFactory.parseResourcesAnySyntax(resourceName).resolve())
      .map(Configuration(_))
      .flatMap(config ⇒ config.getOptional[String]("content_api.mode"))
      .getOrElse(extract(sys.env, "MODE", "test"))
      .toLowerCase()

  protected[configuration] val parseVersionConfFile: Config ⇒ Try[(String, mutable.Buffer[String])] = rootConfig ⇒ {
    for {
      config ← Try(rootConfig.getConfig("build_info"))
      key ← Try(config.getString("module"))
      value ← Try(config.getList("dependencies").asScala.map(_.unwrapped())
        .collect { case s: String ⇒ s }.sorted)
        .orElse(Success(mutable.Buffer.empty[String]))
    } yield key → value
  }

  protected[configuration] val findCurrentModule: Map[String, mutable.Buffer[String]] ⇒ Option[String] = sbtModules ⇒ sbtModules.find {
    case (name, _) ⇒ !sbtModules.exists {
      case (_, dependencies) ⇒ dependencies.contains(name)
    }
  }.map(_._1)

  val stage: Mode = envFromConfigFile(System.getProperties.asScala.toMap) match {
    case "production" ⇒ Production
    case "staging" ⇒ Staging
    case "dev" ⇒ Development
    case _ ⇒ Test
  }

  /**
    * Detect the app currently running. In production mode there should always be exactly one `version.conf` file
    * in the classpath. This is because sub-projects are bundled as jars an their `version.conf` must be accessed
    * differently by the class loader.
    *
    * However, in dev-mode a flat project structure is created that will contain all the `version.conf` files for
    * all the projects that this module depends on. So we need to calculate the currently running app.
    */
  val app: String = {
    if (!stage.isDev) {
      // every module should contain a `version.conf` from which we infer the running app
      Try(ConfigFactory.load("version.conf")
        .getConfig("build_info")
        .getString("module")
      ).getOrElse("local")
    } else {
      // all modules are flat in dev mode, so we need some logic here to figure out the current running app:
      // 1. find all `version.conf` files visible by the class loader
      // 2. Try to parse them as one of our `parseVersionConfFile` files (there may be version.conf files from 3rd party jars as well)
      // 3. Each of our version conf files exposes their sub-project-dependencies, we're searching for the root of this
      //      dependency tree
      val sbtModules = (getClass.getClassLoader.getResources("version.conf").asScala
        .map(res ⇒
          Try(ConfigFactory.parseURL(res)).flatMap(parseVersionConfFile)
        ) collect {
        case Success(value) ⇒ value
      }).toMap

      val currentModule = findCurrentModule(sbtModules)
      // find the module that does not appear in any other module's dependencies (meaning: it has not parent)
      log.debug(s"visible modules: ${sbtModules.keys.mkString(",")}. detected module: $currentModule")
      currentModule.getOrElse("local")
    }
  }
  log.info(s"Environment loaded: app=$app stage=$stage")
}
