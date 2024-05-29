# Template plugin for Epam Report Portal

## UI

Install the dependencies: `npm install`

Build the UI source code: `npm run build`

## Build the plugin

Preconditions:
- Install JDK version 11.
- Specify version number in gradle.properties file.

**Note:** Versions in the _develop_ branch are not release versions and must be postfixed with `NEXT_RELEASE_VERSION-SNAPSHOT-NUMBER_OF_BUILD (Example: 5.3.6-SNAPSHOT-1)`

Build the plugin: `gradlew build`
