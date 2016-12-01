//package de.welt.contentapi.core_client.services.contentapi
//
//import javax.inject.{Inject, Singleton}
//
//import de.welt.contentapi.core.models.{ApiContent, ApiSectionData}
//import de.welt.contentapi.core_client.services.configuration.ContentClientConfig
//import de.welt.contentapi.core_client.services.s3.S3Client
//import de.welt.contentapi.utils.Env.{Env, Live}
//import de.welt.contentapi.utils.Loggable
//import play.api.cache.CacheApi
//import play.api.libs.json.{JsError, JsSuccess, Json}
//import play.api.{Environment, Mode}
//
//import scala.concurrent.duration._
//
//trait SectionService {
//
//  def findChannel(path: String)(implicit env: Env): Option[ApiChannel]
//
//  def enrich(apiContent: ApiContent): ApiEnrichedContent
//
//}
//
//@Singleton
//class SectionServiceImpl @Inject()(config: ContentClientConfig,
//                                   cache: CacheApi,
//                                   s3: S3Client,
//                                   environment: Environment)
//  extends SectionService with Loggable {
//
//  override def findChannel(path: String)(implicit env: Env): Option[ApiConfigfaceChannel] = root.flatMap(_.findByPath(path))
//
//  override def enrich(apiContent: ApiContent): ApiEnrichedContent = {
//
//    val maybeApiSectionData: Option[ApiSectionData] = apiContent.sections.flatMap {
//      _.home.flatMap { home ⇒ {
//        val maybeHomeSection: Option[ApiChannel] = root(Live).flatMap(_.findByPath(home))
//        maybeHomeSection.map { homeSection ⇒
//          ApiConfigfaceChannel.fromChannel(homeSection)
//        }
//      }
//      }
//    }
//    ApiEnrichedContent(apiContent, maybeApiSectionData)
//  }
//
//  protected def breadcrumb(home: ApiChannel): Seq[ApiChannel] = {
//    val breadcrumbPath = home.id.path.split('/').filter(_.nonEmpty).toList match {
//      case Nil ⇒ Nil
//      case head :: tail ⇒ tail.scanLeft(s"/$head/")((path, s) ⇒ path + s + "/")
//    }
//
//    val breadcrumbChannels: List[ApiChannel] = breadcrumbPath.flatMap(segment ⇒ root(Live).flatMap(_.findByPath(segment)))
//    SectionConfigurationServiceImpl.fakeRoot :: breadcrumbChannels
//  }
//
//
//  protected def root(implicit env: Env): Option[ApiChannel] = cache.getOrElse(env.toString, 10.minutes) {
//
//    s3.get(config.aws.s3.janus.bucket, objectKeyForEnv(env))
//      .map { data ⇒ Json.parse(data).validate[ApiChannel] }
//      .flatMap {
//        case JsSuccess(v, _) ⇒
//          v.updateParentRelations()
//          Some(v)
//        case err@JsError(_) ⇒
//          log.warn(err.toString)
//          None
//      }
//
//  }
//
//  protected def objectKeyForEnv(env: Env) = environment.mode match {
//    case Mode.Prod ⇒ s"${config.aws.s3.janus.file}/prod/${env.toString}/config.json"
//    case _ ⇒ s"${config.aws.s3.janus.file}/dev/${env.toString}/config.json"
//  }
//}
//
//object SectionConfigurationServiceImpl {
//  // One Slash for root node is needed for traversing the channels (e.g. in def breadcrumb()).
//  val fakeRoot = ApiChannel(ChannelId("/"), ApiChannelData("Startseite"))
//}
