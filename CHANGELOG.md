# Changelog for ceph3us-features branch fork of Rollbar for Android 

**0.1.3bc1**
- initial release.
- add allow to customize Rollbar thread name to be more friendly and  better recognizable as default one
 
**0.1.3bc4**
- add predefined levels eg: Rollbar.Level.DEBUG
- rewrite RollbarThread include: 
    - concurrency logic first step to minimize deadlocking  
    - more verbosity for debugging     
