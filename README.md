# Why
User engagement in mobile world is a tough nut to crack. To make intimate connections to users of your application you
will have to resort to more direct and personal communication channels. A personally engaging channel plays a major role
in creating a differentiating experience on the mobile application. Engaging users at mobile scale
(way bigger than internet itself) is a challenge. FlipCast is a building block for creating a user engagement platform
which keeps the users of the app come back for more!

# Flipcast
FlipCast is a scalable, multitenant, customizable push notification platform for mobile, devices & web.
FlipCast is built on high performance, high throughput fault tolerant infrastructure components like
spray.io, akka.io which can leverage modern multi-core hardware to support high concurrency.
Support for MongoDB datasource is provided out-of-the-box.

## Releases
| Release | Date | Description |
|:------------|:----------------|:------------|
| Version 1.0    | April 2014    |   Initial OSS release |
| Version 1.0.1  | May 2014      |   Upgrade to Spray 1.3 & Scala 2.11.0 |
| Version 2.0    | December 2014 |   Move to akka.io cluster & distributed pubsub |

## Changelog
Changelog can be viewed in [CHANGELOG.md](https://github.com/Flipkart/flipcast/blob/master/CHANGELOG.md) file

## Supported mobile platforms:
* iOS
* Android
* Windows Phone 8

## Supported Features:
* Device register/unregister API
* Configurable push message payloads
* Automatic housekeeping for invalid devices
* Automatic/Transparent retry and sidelining
* Message history management
* Pluggable data source (Default: MongoDB)
* Automatic backpressure management
* Unicast, Multicast & Broadcast Push API
* Auto Batching for multicast & broadcast push

## Library Dependencies
--------------------
* [spray](http://spray.io) 1.3.2
* [Scala](http://www.scala-lang.org) 2.11.0
* [akka.io](http://akka.io) 2.3.6
* [java-apns](https://github.com/notnoop/java-apns) - 0.2.3
* [casbah](http://mongodb.github.io/casbah) - 2.8.0-RC0

## Infrastructure Dependencies
* [MongoDB] (https://www.mongodb.org)

## Documentation
Please refer to [Wiki](https://github.com/Flipkart/flipcast/wiki) for FlipCast documentation

## License
FlipCast is licensed under : The Apache Software License, Version 2.0. Here is a copy of the license (http://www.apache.org/licenses/LICENSE-2.0.txt)

## Core contributors
* Phaneesh Nagaraja ([@phaneeshn](http://twitter.com/phaneeshn))

