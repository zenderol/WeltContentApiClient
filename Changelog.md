# Changelog

## 5.3 (2019-10-30)

### Changes
- [RAW] removed mandatory field `adIndicator` as part of `RawArticlePromotion`

## 5.2 (2019-10-25)

### Changes
- [RAW] added mandatory field `adIndicator` as part of `RawArticlePromotion`

## 5.1 (2019-10-23)

### Changes
- [CORE] Transform 401 to 502 in AbstractService to preserve Akamai and Varnish cache in case of misconfiguration

## 5.0 (2019-10-08)

### Changes
- [RAW] RawReads migrate old Header and Sponsoring to Sitebuilding
- [API] RawToApiConverter now does intelligent inheritance within all the Sitebuilding fields

## 4.7 (2019-09-23)

### Changes
- [RAW] added JSON serializer `allChildrenWrites` as part of `PartialRawChannelWrites`

## 4.6 (2019-09-18)

### Changes
- [RAW] added optional field `description` as part of `RawChannelStageConfiguredId`

## 4.5 (2019-08-15)
- changed the way configs are passed to the `AbstractService` to prevent lazy/null exceptions 
- Play 2.7.3
- Scala 2.12.9 and Scala 2.13.0

## 4.4 (2019-08-13)
- Added API method to find channels that have a particular commercial stage

## 4.3 (2019-07-30)
- add new field to `RawChannelConfiguration`: `articlePromotions: Option[Seq[RawArticlePromotion]]`
- make some Api fields mutable to be able to modify them in Cigger to migrate Content to Inline Elements for inline promotions

## 4.2 (2019-04-25)

- play 2.7.1
- using `com.kenshoo.metrics-play` instead of fork `de.welt.metrics-play`
- catching `IllegalArgumentException` when register `circuit_breaker` metric (only logging now)
- more version bumps
- clean up code

## 4.1 (2019-04-11)

- changed config for `CapiExecutionContext`

```hacon
// before
contexts.capi {
    fork-join-executor {
        parallelism-factor = 25
        parallelism-min = 25
        parallelism-max = 100
    }
}
// after
content_api {
    akka {
        fork-join-executor {
            parallelism-factor = 25
            parallelism-min = 25
            parallelism-max = 100
        }
    }
}
```

- change path for `frontend-overrides.conf` (incl. app name)
- allowed `prod` and `development` as a valid stage/mode name

## 4.0 (2019-03-01)

- Merged the `client` libs with the `core` libs reducing the lib and module count to `core`, `pressed` and `raw`
- removed the menu stuff since it is mainly used in cmcf
- Rewrote the `configuration` to use the _AWS Simple Systems Manager_ instead of Play's config mgmt
- Removed the `Env` (Live,Preview) since it has not been used anywhere 

## 3.4 (2019-02-26)
- Added API to find channels that have a particular stage configured from god mode

## 3.3 (2019-02-04)

### Changes
- Updated to Playframework 2.7.0

## 3.2 (2018-11-21)

### Changes
- [RAW] changed default `RawChannelSiteBuilding` configuration from *empty Optional* to *None*
- implemented master channel inheritance for `RawChannelSiteBuilding`

## 3.1 (2018-11-05)

### Changes
- [RAW] added optional field `logo` as part of `RawChannelStageCustomModule`

## 3.0 (2018-10-12)

*Changes*

- (breaking) changed signature of the `AbstractSerivce` to:
  - Allow other `methods` than get by setting the `method: "POST"` in the ServiceConfig
  - Allow other content types than `JSON` by changing the signature of the `validate` function 
  (see changes -> `ContentService` for an example implementation)
  - Made `basic auth credentials`/`Api key` optional: Its now possible to omit credentials 

## 2.3 (2018-10-04)

*Changes*

- added `RawChannelSiteBuilding` for configuring channel header, footer and sponsoring.

*Deprecations* 

- Deprecated `RawChannelHeader` and `RawSponsoringConfig`

## 2.2 (2018-09-18)

- `MenuService`: removed auto sync cron job. It's now part of the consumer.
- `MenuService`: add `refresh()` for sync data from S3.
- `MenuService`: add new `Configuration` value `mode` to override the S3 env folder. This is for testing prod data on localhost.

## 2.1 (2018-08-29)

All WC/AC models must include a prefix for better identification in code reviews.
Bring your own models (BYOM) => consume the api + api models but create new models for your application logic / templates.

- renamed case classes: `Menu` -> `ApiMenu`, `MenuMetadata` -> `ApiMenuMetadata` and `MenuLink` -> `ApiMenuLink`
- removed `MenuReference` (replaced by `ApiReference`)
- refactoring of `MenuService` and `AdminMenuService`

## 2.0 (2018-08-09)

- added new project called `menu`
- added `MenuService` for retrieving menu data from S3
- added case classes `Menu`, `MenuMetadata`, `MenuLink` and `MenuReference`

#### Breaking Changes

- Moved `AdminMenuService` to `menu` project
- Removed case classes `RawMenu` and `RawMenuLink`

## 1.8 (2018-08-02)

- added `RawChannelStageConfiguredId` as a new main stage type
- this allows single ids to be configured on section pages without the need of Papyrus Curation
- example usecases: Advertorial Footers, oEmbeds (WM, Podcasts,...)


## 1.7 (2018-06-08)

- added `AdminMenuService` for storing menu data on S3
- added case classes `RawMenu` and `RawMenuLink`

## 1.6 (2018-06-04)

- Added optional sponsoring to `ApiStageConfiguration`

## 1.5 (2018-05-31)

*Changes*

- introduced new `TreeService` as an abstraction to the `RawTreeService` in the front end
- Migrated the `AuthorService` from _funkotron_ into the capi to allow using it within cigger (why: _author augmenting_)
- `RawTreeService` will not access s3 when app is started in `Mode.Test`
- Play Framework version bump to `2.6.15`

*Deprecations*

- Deprecated large parts of `ApiAuthor` in favor of augmenting authors json into the `related` array of the `ApiPressedContent`

## 1.4 (2018-03-28)

- Changed signature of _cigger_ to `ApiPressedContentResponse`

## 1.3 (2018-03-27)

- added `ApiPressedContentResponse`
- bumped to Play 2.6.12

## 1.3 (2018-03-20)

- add optional http headers in AbstractService

## 1.2 bugfix (2018-03-16)

- definition which Exceptions will cause the breaker to stay open or allow close. e.g.
    - `HttpClientErrorException` and `HttpRedirectException` will keep the breaker closed (this is good)
    - whereas `HttpServerErrorException` and any other exception may open the breaker

## 1.2 (2017-11-27)

- introduced circuit breaker pattern in AbstractService
- Playframework 2.6.6
- Added Cigger Support

#### Breaking Changes

- AbstractService is now an `abstract class` (instead of a `trait`)
- `ExecutionContexts` should now be `@Injected` everywhere instead of being passed as `implicit` parameters 

## 1.1 (2017-07-21)

- Upgraded to Playframework 2.6.2 

## 1.0 (2017-06-20)

#### Breaking Changes

- Playframework 2.6 (dropped 2.4 & 2.5)
- Scala 2.12 (dropped 2.11)

## 0.22.X (2017-10-26)

### Changes
- [CORE-CLIENT] add Service `ContentBatchService` to allow digger to resolve all Ids of a stage with one call

## 0.21.X (2017-10-19) 

### Changes
- [CORE] add field `validFromDate` to `ApiMetadata`

## 0.20.X (2017-10-17)

### Changes
- [RAW] added field `hideCuratedStageLabel` as part of `RawChannelStageCurated`

## 0.19.0 (2017-???)
### Changes
- [MISSING DATA]

## 0.18.0 (2017-09-19)

### Changes
- [RAW] added param `brandstation` as part of `RawChannelSponsoring`
- [PRESSED] added param `brandstation` as part of `ApiSponsoringConfiguration`

## 0.14.0 (2017-04-12)

#### Breaking Changes
- [CORE]    Delete `PapyrusRepository` and `CurationService (in-sourced to Digger)
- [LEGACY]  Delete Module `Legacy` - Models and Client (da-hood was 100% replaced by Digger)

## 0.13.0 (2017-03-31)

#### Changes
- [RAW] bugfix filter empty map values (overrides)
- [RAW] bugfix correct json.writes of stages
- [CORE] move `Strings.scala` to [UTILS]

#### Breaking

##### Change package for `Strings`

**before**

```
de.welt.contentapi.core.client.utilities.Strings
```

**after**

```
de.welt.contentapi.utils.Strings
```


## 0.12.0 (2017-03-09)

#### Changes
- [RAW] added case class property `master: Boolean` to `RawChannelConfiguration`
- [PRESSED] change impl. of master-channel (inheritance) calculation

#### Breaking
/

## 0.11.0 (2017-02-03)
- Future proof JSON parsing
- Digger can ignore unknown new modules from CMCF
- CMCF can be rolled out with new Modules that are not yet known to Digger will be ignored

#### Changes
- added case class `RawChannelStageIgnored`

## 0.10.0 (2017-02-03)

#### Changes

- `PressedSectionService` now wraps its responses
  - the wrapper contains information about `time`, `status`, `source`, .. of the pressed result
  - those wrapped models are uploaded to S3
  - alternate treatment of backend calls when in `Dev Mode` 
- Changed logging to typesafe's scala-logging (should increase performance and reduce memory)
- minor fixes like typos  

#### Breaking

- the models in S3 and the `PressedSectionService` are now wrapped (JSON changed)

#### Migration

- the Logger automatically does the `log.isLevelEnabled()` and therefore permits calling this directly. Just remove the `if`.
- As for the wrapped JSON: You could simply unwrap it to use the previous models.

## 0.9.0 (2017-01-26)

#### Changes

- use Guice directly instead of Play's DI abstraction.

#### Breaking

- Play's DI and Guice are compatible to some extend, but you should consider using Guice's interfaces if you access the WC/AC DI-Modules directly 

#### Migration

Follow DI as described here https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection

- migrate your code from `play.api.inject.Module` to `com.google.inject.AbstractModule`
- change from `def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]]
` to `protected abstract void configure()`
- for module-dependencies, use `install(m: Module)`

## 0.8.0 (2017-01-25)

#### Changes

- migrated to wrapped search endpoint that allows paginated searches

#### Breaking

- none, if migration is applied

#### Migration

- just call `searchResponse.map(_.results)` to unwrap the search result and use the old models
- update the _search_ `endpoint` in your configuration to `/content/wrapped/_search`

## 0.7.0 (2017-01-13)

#### Changes

- params for the `AbstractService` are getting trimmed now (remove leading and trailing white space) Fixes #42

#### Breaking

- type safe rewrite of the `ApiContentSearch`
- made the `RequestHeader` non `Optional`, but initialize with `Seq.empty` instead

## 0.4.0 (2016-10-18)

#### Changes

- `ContentApiQuery`
  - `flags` will be *deprecated* in favour of the new search parameter `flag`: In our API we currently only have two flags: `highlight` and `premium` and the negation of those. With the new `flag` search parameter you can combine/exclude those flags, e.g. `flag: [premium, no_highlight]` or `flag: [no_premium, no_highlight]`. The results are a conjunction of the flags (the terms are *and*ed -> `!premium && !highlight`.
  - `subTypeExcludes` (some of them formerly available through the `flags`) are moving into the `subType` search parameter, use the negated version of the corresponding subType
  - `subType` search parameter will now allow list inputs, allowing you to do things as `subType: [no_broadcast, no_video, no_printImport, no_oembed]`
- added missing default case to `ApiChannel`


## <= 0.3.x

Initial Version
