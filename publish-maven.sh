#!/bin/sh

mvn clean deploy -P release -Dmaven.test.skip=true -e
