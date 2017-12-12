# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.3.6]
### Fixed
- initial subscriptions did not work, fixed
### Removed
- wrap-subscriptions
### Changed
- updated dependencies

## [0.3.5]
### Added
- coeffects

### Changed
- middleware functions got two additional parameters

## [0.3.4]
### Changed
- forward-subs has more general forms

## [0.3.3]
### Added
- from-chan-fn-fx and from-promise-fn-fx generic effects

### Changed
- removed ITaggable, implementing IEffects got a bit easier (less boilerplate)

## [0.3.2]
### Added
- add new effects: http/post-fx and http/open-url-fx
- add delay option to dispatch-fx

### Changed
- effects that may fail, such as http/get-fx, will wrap the result in a map like {:ok response} or {:error error}
- updated dependencies

## [0.3.1]
### Added
- `navigate-fx` and `navigate` subscription

### Changed
- renamed `dom` namespace to `browser`
- updated dependencies

## [0.3.0]
### Added
- focus-fx - in case you really need it

### Changed
- going independent of re-frame

## [0.2.2]
### Added
- Eventbus - publishing and listening to messages, through topics
- DataStore - reading and writing to a global datastore, with notifications