#!/usr/bin/env bash
sbt clean scalastyle
sbt coverage test it:test
sbt coverageReport
sbt coverageAggregate
sbt codacyCoverage
