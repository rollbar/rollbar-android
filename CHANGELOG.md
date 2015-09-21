# Change Log

**0.1.3**
- Add pom.xml with metadata for Maven Central

**0.1.2**
- Added flag to `Rollbar.init()` to enable/disable the registration of the uncaught exception handler. (pr#17)

**0.1.1**
- Added reportMessage(message, level, params) to send extra params with messages. (pr#10)

**0.1.0**
- Fixed build.xml, changed `code_version` parameter. (pr#11)
- `code_version` now properly refers to the [versionName](http://developer.android.com/reference/android/content/pm/PackageInfo.html#versionName) attribute from the manifest file.

**0.0.6**
- Added ability to provide a description for caught exceptions. Usage: [https://github.com/rollbar/rollbar-android#usage](https://github.com/rollbar/rollbar-android#usage)
- Added isInit() method to check to see if Rollbar is initialized.

**0.0.5**
- Exceptions with causing exceptions will now be sent with all the causes chained together in a single item. These multiple exceptions will all appear in the proper order in the Rollbar interface.
- Uncaught exceptions are now saved to disk instead of immediately being reported, to prevent any blocking of the app shutdown process. A new configuration option exists to revert to the old behavior.

**0.0.4**
- Ability to include logcat output for every item reported.
- Ability to set person data.
- Caught exception level now defaults to warning, new configuration options available to change this and uncaught exception level.

**0.0.3**
- New persistence system to support retries of item payloads not sent due to network connectivity issues.

**0.0.2**
- Use JSON objects and arrays instead of HashMaps and ArrayLists for payload construction.
- Set reportUncaughtExceptions to true by default.

**0.0.1**
- Initial release.
