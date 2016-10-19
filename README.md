# ceph3us fork of Rollbar for Android

<!-- RemoveNext -->
Java library for reporting exceptions, errors, and log messages to [Rollbar](https://rollbar.com).

## Setup ##

### Maven

Install from Maven central:

```
<dependencies>
  <dependency>
    <groupId>pl.ceph3us.projects.android</groupId>
     <artifactId>rollbar</artifactId>
     <version>0.1.3-bc</version>
  </dependency>
</dependencies>
```

or

```
compile 'pl.ceph3us.projects.android.rollbar:rollbar:0.1.3-bc'
```

### Jar

Download [rollbar.jar](https://github.com/c3ph3us/rollbar/releases/latest) and place it in your Android project's `libs` directory.

Add the following line in your custom Application subclass's `onCreate()` to initialize Rollbar:

```java
Rollbar.init(this, "MyCustomRollbarThreadName", "POST_CLIENT_ITEM_ACCESS_TOKEN", "production");
```
if you don't want to customize rollbar thread name just omit it.

Make sure your `AndroidManifest.xml` contains the `android.permission.INTERNET` permission.

## Usage ##

By default the notifier will report all uncaught exceptions to Rollbar.

To report caught exceptions, call `reportException()`:

```java
Rollbar.reportException(new Exception("Test exception")); // default level is "warning"

try {
    amount = Integer.parseInt(data);
} except (NumberFormatException e) {
    // Providing a description for this exception
    Rollbar.reportException(e, "critical", "Unexpected data from server");
}
```

To report your own messages, call `reportMessage()`:

```java
Rollbar.reportMessage("A test message", "debug"); // default level is "info"
```

## Configuration reference ##

The following configuration methods are available:

 * **Rollbar.setPersonData(String id, String username, String email)**
    
    Sets the properties of the current user (called a "person" in Rollbar parlance) to be sent along with every report.
    
    Default: all `null`


 * **Rollbar.setReportUncaughtExceptions(boolean report)**

    Sets whether or not to report uncaught exceptions to Rollbar.
    
    Default: `true`


 * **Rollbar.setIncludeLogcat(boolean includeLogcat)**

    Sets whether or not to include logcat output in reports to Rollbar.
    
    Note: For devices with API level 15 and lower, you will need to include the `android.permission.READ_LOGS` permission in your app's `AndroidManifest.xml` for logcat collection to work.
    
    Default: `false`


 * **Rollbar.setDefaultCaughtExceptionLevel(String level)**

    Sets the level caught exceptions are reported as by default.
    
    Default: `"warning"`


 * **Rollbar.setUncaughtExceptionLevel(String level)**

    Sets the level uncaught exceptions are reported as.
    
    Default: `"error"`


 * **Rollbar.setSendOnUncaughtException(boolean send)**

    If true, uncaught exceptions will immediately be sent to Rollbar, blocking the process shutdown sequence. If false, the exception will just be saved to disk so that it can be sent next time the process starts.

    Default: `false`


 * **Rollbar.setEndpoint(String endpoint)**

    If you are using Rollbar Enterprise, call `setEndpoint` to route events to your Rollbar instance. (Most users will not need to do this.)

    Default: `"https://api.rollbar.com/api/1/items/"`



## Deobfuscation ##

If you use [ProGuard](http://developer.android.com/tools/help/proguard.html) to obfuscate your code in production, reported stack traces will not be very useful in most cases.

Rollbar provides a way for you to upload the `mapping.txt` file generated in the obfuscation process so that stack traces can be deobfuscated as they are reported.

Here is an example cURL command to upload a `mapping.txt` file:

```bash
curl 'https://api.rollbar.com/api/1/proguard' \
  -F access_token=POST_SERVER_ITEM_ACCESS_TOKEN \
  -F version=0.0.10 \
  -F mapping=@path/to/mapping.txt
```

Where `version` matches the `android:versionName` in your app's `AndroidManifest.xml`, corresponding to the version the `mapping.txt` was generated for.

After uploading, any future reported exceptions for the specified version will automatically be deobfuscated using the mapping file.

By default, file names and line numbers are removed by ProGuard. To preserve this information to make debugging easier, add the following to your `proguard-project.txt`:

```
-keepattributes SourceFile,LineNumberTable
```

## Help / Support

If you run into any problem please fill issue.

## Build

1. Define property `sdk.dir` which contains path to Android SDK inside `local.properties`
2. Make sure you have installed the platform specified in `project.properties`
  - E.g. `sdk.dir = /Users/coryvirok/Development/adt-bundle-mac-x86_64-20140702/sdk/`
3. Run `ant` from command line

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
