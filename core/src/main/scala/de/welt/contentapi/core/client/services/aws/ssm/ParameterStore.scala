package de.welt.contentapi.core.client.services.aws.ssm

import com.amazonaws.regions.Region
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.{GetParameterRequest, GetParametersByPathRequest}
import de.welt.contentapi.core.client.services.configuration.ApiConfiguration
import de.welt.contentapi.utils.Loggable

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class ParameterStore(region: Region) extends Loggable {

  private lazy val ssm = ApiConfiguration.aws.credentials.map { credentials ⇒
    AWSSimpleSystemsManagementClientBuilder.standard()
      .withCredentials(credentials)
      .withRegion(region.getName)
      .build()
  } match {
    case Success(value) ⇒ value
    case Failure(th) ⇒
      log.error("Could not initialize AWS SSM Client.", th)
      throw th
  }

  def parameterByKey(key: String): Try[String] = {
    val parameterRequest = new GetParameterRequest()
      .withWithDecryption(true)
      .withName(key)
    Try(ssm.getParameter(parameterRequest).getParameter.getValue)
  }

  def getPath(path: String): Map[String, String] = {

    @tailrec
    def pagination(acc: Map[String, String], nextToken: Option[String]): Map[String, String] = {
      val req = new GetParametersByPathRequest()
        .withPath(path)
        .withWithDecryption(true)
        .withRecursive(false)

      val reqWithToken = nextToken.map(req.withNextToken).getOrElse(req)
      val result = ssm.getParametersByPath(reqWithToken)
      val resultMap = acc ++ result.getParameters.asScala.map { param ⇒
        param.getName → param.getValue
      }

      Option(result.getNextToken) match {
        case Some(next) ⇒ pagination(resultMap, Some(next))
        case None ⇒ resultMap
      }
    }

    pagination(Map.empty, None)
  }
}