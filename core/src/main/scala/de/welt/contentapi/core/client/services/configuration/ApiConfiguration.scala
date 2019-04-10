package de.welt.contentapi.core.client.services.configuration

import java.io.File

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.profile.internal._
import com.amazonaws.auth.profile.internal.securitytoken.STSProfileCredentialsServiceLoader
import com.amazonaws.auth.{AWSCredentialsProvider, AWSCredentialsProviderChain, EC2ContainerCredentialsProviderWrapper}
import com.amazonaws.profile.path.AwsProfileFileLocationProvider
import com.amazonaws.regions.{Region, Regions}
import com.google.common.base.Stopwatch
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import de.welt.contentapi.core.client.services.aws.ssm.ParameterStore
import de.welt.contentapi.utils.Loggable

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

//noinspection ScalaStyle
case class ConfigurationException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

/**
  * provide static configuration required by the WCAPI itself
  *
  * - Services accessing AWS/S3 that are part of the WCAPI
  * - API (ServiceConfigurations)
  */
class ApiConfiguration extends Loggable {

  private lazy val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_WEST_1))

  private def configFromResource(resourceName: String): Config = ConfigFactory.parseResourcesAnySyntax(resourceName).resolve()

  private def configFromFile(path: String): Config = ConfigFactory.parseFileAnySyntax(new File(path)).resolve()

  private def configFromParameterStore(path: String): Config = {
    val params = parameterStore.getPath(path)
    log.debug(s"[SSM] Loading config. path='$path'.")
    val configMap = params.map {
      // remove the path prefix from ssm
      case (key, value) ⇒ key.replaceFirst(path, "") -> value
    }
    ConfigFactory.parseMap(configMap.asJava, s"SSM param store @$path").resolve()
  }

  private lazy val parameterStore = new ParameterStore(awsRegion)

  import Environment._

  lazy val configuration: Config = {
    if (Environment.stage.isTest) {
      val testConfigResource = Environment.extract(System.getProperties.asScala.toMap, "config.resource")
        .getOrElse("application.test.conf")
      log.debug(s"Loading test configuration from $testConfigResource")
      val config = configFromResource(testConfigResource)
      log.debug(ApiConfiguration.configuration.root().render(ConfigRenderOptions.concise()))
      config
    } else {

      val sw = Stopwatch.createStarted()

      log.debug("Started loading Configuration.")
      val userPrivate = configFromFile(s"${System.getProperty("user.home")}/.welt/frontend-overrides.conf")
      val frontendConfig = configFromParameterStore("/frontend/")
      val frontendStageConfig = configFromParameterStore(s"/frontend/${stage.toString.toLowerCase()}/")
      val frontendAppConfig = configFromParameterStore(s"/frontend/${stage.toString.toLowerCase()}/${app.toLowerCase}/")

      val config = Try(userPrivate
        .withFallback(frontendAppConfig)
        .withFallback(frontendStageConfig)
        .withFallback(frontendConfig)
      )

      config match {
        case Success(value) ⇒
          log.debug(s"Finished loading Configuration in ${sw.stop().toString}.")
          log.debug(value.root().render(ConfigRenderOptions.concise()))
          value
        case Failure(exception) ⇒
          log.error("Could not load config.", exception)
          throw exception
      }
    }

  }

  //noinspection ScalaStyle
  def reportError(path: String, userMessage: String, th: Throwable = null, c: Config = configuration): Nothing = {
    val message = Option(if (c.hasPath(path)) c.getValue(path).origin else c.root.origin)
      .map(origin ⇒ s"Configuration Error. $userMessage [config source: ${origin.toString}]")
      .getOrElse(s"Configuration Error. $userMessage")

    reportError(message, ConfigurationException(message, th))
  }

  private def reportError(message: String, th: Throwable): Nothing = {
    log.error(message, th)
    throw th
  }

  object aws {

    //noinspection ScalaStyle
    val loadConfig: File ⇒ Try[mutable.Map[String, BasicProfile]] = file ⇒ if (null != file && file.exists()) {
      log.info(s"Trying to read file ${file.getName} for AWS credentials.")
      Try(
        BasicProfileConfigLoader.INSTANCE.loadProfiles(file).getProfiles.asScala.map {
          case (k, v) ⇒ k.replaceFirst("^profile ", "") → v
        }).recover {
        case th: Throwable ⇒
          log.info(s"Could not load ${file.getName}", th)
          mutable.Map.empty[String, BasicProfile]
      }
    } else {
      Success(mutable.Map.empty[String, BasicProfile])
    }

    def credentials: Try[AWSCredentialsProvider] = {
      //    lazy val credentials: Try[AWSCredentialsProvider] = {
      log.debug("Providing aws credentials chain: profile[frontend] -> InstanceProfile")

      // ProfileAssumeRoleCredentialsProvider
      // issue: https://github.com/aws/aws-sdk-java/issues/803
      // workaround: https://gist.github.com/adrian-baker/81ec8e7cd8f8e15d343157ac9116faac

      val allProfiles: mutable.Map[String, BasicProfile] = (for {
        configs ← loadConfig(AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER.getLocation)
        profiles ← loadConfig(AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation)
      } yield configs ++ profiles).getOrElse(mutable.Map.empty)

      val maybeProfileCredentialsProvider = allProfiles.get("frontend").map { frontendProfile ⇒
        if (frontendProfile.isRoleBasedProfile) {
          new ProfileAssumeRoleCredentialsProvider(
            STSProfileCredentialsServiceLoader.getInstance(),
            new AllProfiles(allProfiles.asJava),
            frontendProfile)
        } else {
          new ProfileStaticCredentialsProvider(frontendProfile)
        }
      }
      // InstanceProfileCredentialsProvider is used on production
      // non-AWS-envs must provide the 'frontend' aws profile
      val chain: List[AWSCredentialsProvider] = List(maybeProfileCredentialsProvider).flatten ++
        List(new EC2ContainerCredentialsProviderWrapper())

      val provider = new AWSCredentialsProviderChain(chain.asJava)

      // this is a bit of a convoluted way to check whether we actually have credentials.
      // I guess in an ideal world there would be some sort of isConfigued() method...
      try {
        provider.getCredentials
        log.debug("Returning Provider")
        Success(provider)
      } catch {
        case ex: AmazonClientException =>
          log.error(ex.getMessage, ex)
          throw ex
      }
    }

    object s3 {

      lazy val region: String = configuration.getString("s3.region")

      object raw {
        lazy val bucket: String = configuration.getString("s3.raw_tree.bucket")
        lazy val file: String = configuration.getString("s3.raw_tree.file")
      }

      object author {
        lazy val bucket: String = configuration.getString("s3.author.bucket")
        lazy val file: String = configuration.getString("s3.author.file")
      }

    }

  }

}

// create one "static" instance of this configuration to be used by the CAPI and later on by the consumers of the CAPI
object ApiConfiguration extends de.welt.contentapi.core.client.services.configuration.ApiConfiguration