# MUSIT 
Norwegian university museums IT organization (MUSIT); cultural history and natural history database with store integration.

Code status for master branch:
* [![Build Status](https://travis-ci.org/MUSIT-Norway/musit.svg?branch=master)](https://travis-ci.org/MUSIT-Norway/musit)
* [![Codacy Badge](https://api.codacy.com/project/badge/Grade/09d679eb62f64a87ad7a9bfc90c643cc)](https://www.codacy.com/app/musit-project/musit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MUSIT-Norway/musit&amp;utm_campaign=Badge_Grade)
* [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/09d679eb62f64a87ad7a9bfc90c643cc)](https://www.codacy.com/app/musit-project/musit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MUSIT-Norway/musit&amp;utm_campaign=Badge_Coverage)

Status badges is retrieved directly from the services monitoring the code.

The Norwegian university museums IT organization (MUSIT) is a cooperation agreement between the University of Oslo, University of Bergen, Norwegian University of Science and Technology, University of Tromsø - Norway's Arctic university, and the University of Stavanger. 
The University museums of Norway hold in trust regional and global scientific collections of natural and cultural history. The cooperative aims to ensure the operation, maintenance and development of the university museums' joint collection databases, and to facilitate sharing dissemination of data for research, education, management and the public. 

MUSIT strives for greater integration between its databases and is an open source initiative cooperating with other projects such as DINA (National History museum of Stockholm - Sweden), and Kotka (National History museum of Helsinki - Finland).

## License
All code is protected under the [GPL v2 or later](http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) and copyright holder is [MUSIT](http://musit.uio.no) as a part of [University of Oslo](http://www.uio.no).

## Contents
**gui** - ReactJS w/redux frontend and NodeJS api gateway implemetation. ([See Frontend](#Frontend))

**common** - A directory for common code for all service implementations.

**service**_* - Microservice implementations in scala and Playframework. ([See Middleware](#Middleware))


### Middleware
All middeware service* projects are a Scala 2.11.x implementation of a microservice architecture using Playframework 2.4.x.
The base structure for the application is made from Lightbends activator example on Playframework microservices and customised for the projects requirements.

The microservices is the service support for the ReactJS frontend to store informastion about all objects collected for both Naturalhistory and Culturalhistory as well as how and where these are stored.

The microservices will be database transparent when the project is at version 1, but during the development process we are gradualy migrating an old Oracle database. This makes us dependant on Oracle in the start.

The project use Slick to abstract the new database.

The REST endpoints follow best practices with HATEOS linking to keep datapackets small. This enables us to support mobile devices easier.

All development has been done on Redhat Linux Enterprise 7 and Apple OSX. If you find any issues do not hesitate posting an issue or make a pull request on GitHub for the project so we can look closer at this and get it sorted.

Notice that all files in this directory and hereunder is subject to the GPL License and the full copyright is owned by MUSIT as part of UIO.

#### Installation

To get started you need to install the following components on your computer:
* [SBT](http://www.scala-sbt.org) latest version.
* [Java 8](http://java.oracle.com) lates patchlevel of java 8 SE JDK.
* [Docker](http://www.docker.com) latest version for your OS.

After Java 8 and sbt is installed.

1. sudo service docker start
2. sbt compile

This will initialize the environment and make the first build.

Project is runnable from the code or through docker containers. The development process is done outside docker to make compile roundtrip as fast as possible. Deployment is done in docker container to make automation of the deployment and infrastructure easy and robust.

#### Configuration

##### Microservices

To externalize configuration for different environments you need to use the playframework override functionality.
When starting the microservice binary you need to call it using:
```
$ path/to/bin/<project_name> -Dconfig.file=/etc/musit/production.conf
```
The Production.conf file should override all applicable values from the application.conf defaults and reference.conf.

You can also override configured values using value overrides when starting the project, see example:
```
$ path/to/bin/<project_name> -Dplay.crypto.secret=mamamia
```

To override logging for the microservice you can force the application to read a different logback.xml file using the
following property override when starting the application:
```
$ path/to/bin/<project_name> -Dlogger.file=/etc/musit/logback.xml
```

To give JVM parameters when starting the microservice, you use -J on the command line, see the example:
```
$ path/to/bin/<project_name> -J-Xms128m -J-Xmx512m -J-server
```
This equals a java command with:
```
$ java -Xms128m -Xmx512m -server -jar path/to/jar/jarfile.jar
```

#### Running

To run a spesific microservice you need to use sbt or build a docker container and start this. We recomend using sbt to run the microservices in the development process outside of docker.

```
sbt "project service_<name>" "run <port>"
```
#### Scalastyle : Check the code quality

To check code quality of all the modules
```
$ sbt clean compile scalastyle
```

#### Scoverage : Check code coverage of test cases

To check code coverage of test cases for all modules
```
$ sbt clean coverage test
```
By default, scoverage will generate reports for each project seperately. You can merge them into an aggregated report by invoking
```
$ sbt coverageAggregate
```

#### References

* [Play Framework](http://www.playframework.com/)
* [Microservices](http://martinfowler.com/articles/microservices.html)
* [Microservices: Decomposing Applications](http://www.infoq.com/articles/microservices-intro)

### Frontend

This is a NodeJS frontend project written in ReactJS with Redux for interacting with MUSIT-Backend.
The node instance code also contains the API Gateway and router implementation to connect to the microservices.
The frontend Redux code communicate over WebSockets with the API gateway to make the client more connected and the
communication protocol as tight and compact as possible.

_The API Gateway will be expanded with a rest interface proxy when the project gets more mature and the microservices
start stabilizing._

#### Installation

To get started you need to install the following components on your computer:
* [NodeJS](https://nodejs.org/en/download) v5+ with NPM for your operating system

After NodeJS and NPM is installed you need to install some global libraries through NPM to be able to run the project.

1. cd gui
2. npm install webpack -g
3. npm install webpack-dev-server -g
4. npm install babel -g
5. npm install phantomjs-prebuilt -g
6. npm install

(_For your convenience a initialize.sh script has been added to the gui directory that execute these commands_)

#### Building

While standing in the gui directory you can build the project by using the npm run build command.

#### Running

You use npm to run the application while standing in the gui directory and the following targets are available:
TBD

## Contact

If you are interested in participating in the project please contact opensource@musit.uio.no (not active yet)
