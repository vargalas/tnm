# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build/Test Commands
- Build: `./gradlew build`
- Run: `./run.sh`
- Test all: `./gradlew test`
- Test specific class: `./gradlew test --tests "full.package.ClassNameTest"`
- Test specific method: `./gradlew test --tests "full.package.ClassNameTest.methodName"`

## Code Style Guidelines
- **Imports**: Organize imports by standard library, then third-party, then project
- **Formatting**: Use 4-space indentation, no tabs
- **Types**: Specify explicit types for public interfaces, use type inference (`val` and `var`) internally
- **Naming**: Use camelCase for variables/functions, PascalCase for classes/interfaces/enums
- **Error Handling**: Use exceptions for exceptional cases, prefer nullable types over exceptions when appropriate
- **Functional Style**: Use idiomatic Kotlin functional style (map, filter, etc.)
- **Threading**: Use thread-local variables and thread pools for parallel processing operations
- **Testing**: Use JUnit and Kotlin's test library with descriptive test names
- **Comments**: Provide KDoc comments for public APIs explaining purpose and parameters