# TNM Repository Mining Tool - Java Version

This project is a Java port of the original Kotlin-based TNM repository mining tools. It's designed to analyze Git repositories to extract metrics about development activity, collaboration patterns, and codebase structure.

## Features

- **Git Repository Analysis**: Extracts data from Git commit history
- **Developer Activity Tracking**: Identifies when and how developers work
- **Collaboration Analysis**: Detects patterns of developer collaboration
- **File Dependency Analysis**: Maps dependencies between files
- **Single-File Implementation**: All functionality in one Java file

## Build Instructions

```bash
# Clone the repository
git clone https://github.com/yourusername/tnm-java.git
cd tnm-java

# Build with Gradle
./gradlew build

# Run the application
java -jar build/libs/merged-java-shadow-1.0-SNAPSHOT-all.jar
```

## Example: Running WorkTimeMiner

The WorkTimeMiner analyzes when developers commit code to identify working patterns.

```bash
java -jar build/libs/merged-java-shadow-1.0-SNAPSHOT-all.jar run-miner /path/to/git/repo main ./work-time-output.json
```

This will:
1. Analyze the `main` branch of the repository
2. Extract commit timestamps for each developer
3. Generate a work time matrix showing activity patterns
4. Save the results to `work-time-output.json`

## Documentation

For detailed information on the Java implementation and Kotlin to Java migration process, see [JAVA_MIGRATION.md](JAVA_MIGRATION.md).

## Related Projects

- [Original Kotlin TNM Tool](https://github.com/yourusername/tnm): The original Kotlin implementation

## License

Same as the original project.