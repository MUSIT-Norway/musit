#!/usr/bin/env bash
sbt clean scalastyle
sbt coverage test
sbt coverageReport
sbt coverageAggregate
sbt codacyCoverage