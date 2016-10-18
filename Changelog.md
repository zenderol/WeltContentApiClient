Changelog
=========

0.4.0 (2016-10-18)
------------------

Bug fixes:

- added missing default case to `ApiChannel`

Features:

- `ContentApiQuery`
  - `flags` will be *deprecated* in favour of the new search parameter `flag`: In our API we currently only have two flags: `highlight` and `premium` and the negation of those. With the new `flag` search parameter you can combine/exclude those flags, e.g. `flag: [premium, no_highlight]` or `flag: [no_premium, no_highlight]`. The results are a conjunction of the flags (the terms are *and*ed -> `!premium && !highlight`.
  - `subTypeExcludes` (some of them formerly available through the `flags`) are moving into the `subType` search parameter, use the negated version of the corresponding subType
  - `subType` search parameter will now allow list inputs, allowing you to do things as `subType: [no_broadcast, no_video, no_printImport, no_oembed]`

version <= 0.3.x
----------------

- initial version