# TNM Git Repository Mining Tool - Java Migration Guide

This document outlines the process of migrating the TNM repository mining tools from Kotlin to Java, focusing on architecture, implementation details, and usage examples.

## Project Overview

The TNM repository mining tool analyzes Git repositories to extract various metrics about development activity, developer collaboration, and codebase structure. The original implementation consisted of 154 Kotlin files (~7,000 lines of code) and has been refactored into a single Java file for simplicity.

## Architecture

The Java implementation maintains the core architecture of the original Kotlin project:

### Core Components

1. **Miners**
   - Abstract base class: `GitMiner`
   - Concrete implementations: `WorkTimeMiner`, etc.
   - Responsible for analyzing Git repositories and extracting data

2. **Data Processors**
   - Interface: `DataProcessor`
   - Concrete implementations: `WorkTimeDataProcessor`, etc.
   - Process and transform raw data from miners

3. **CLI Interface**
   - Command-line interface using the Clikt library
   - Supports different mining operations

### Key Interfaces

```java
public interface Miner<T extends DataProcessor<?>> {
    void run(T dataProcessor);
}

public interface DataProcessor<T> {
    void calculate();
}
```

## Implementation Details

### GitHub Data Extraction

The implementation uses JGit to access Git repository data:

```java
protected final ThreadLocal<Git> threadLocalGit = new ThreadLocal<Git>() {
    @Override
    protected Git initialValue() {
        return new Git(repository);
    }
};
```

### Multithreading Support

Miners process commits in parallel using Java's ExecutorService:

```java
ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
processAllCommitsInThreadPool(branches, dataProcessor, threadPool);
threadPool.shutdown();
```

### Thread Safety

Thread-local variables ensure thread safety when accessing Git repositories:

```java
protected final ThreadLocal<Git> threadLocalGit;
protected final ThreadLocal<ObjectReader> threadLocalReader;
protected final ThreadLocal<DiffFormatter> threadLocalDiffFormatter;
```

## Kotlin to Java Migration Challenges

### 1. Null Safety

- **Kotlin**: Built-in null safety with `?` and `!!` operators
- **Java**: Explicit null checks or Optional<T>

```java
// Java approach
if (userName != null) {
    // process username
}
```

### 2. Extension Functions

- **Kotlin**: Extension functions add methods to existing classes
- **Java**: Static utility methods or wrapper classes

```java
// Java approach (static utility)
public static String getShortBranchName(String fullName) {
    if (fullName.startsWith("refs/heads/")) {
        return fullName.substring("refs/heads/".length());
    }
    return fullName;
}
```

### 3. Functional Programming

- **Kotlin**: Rich functional API (map, filter, etc.)
- **Java**: Stream API or traditional loops

```java
// Java approach
Map<Integer, String> filteredMap = new HashMap<>();
for (Map.Entry<Integer, String> entry : originalMap.entrySet()) {
    if (entry.getValue() != null) {
        filteredMap.put(entry.getKey(), entry.getValue());
    }
}
```

### 4. Data Classes

- **Kotlin**: Concise data classes with automatic equals/hashCode/toString
- **Java**: POJOs with manual implementations or libraries like Lombok

## Usage Examples

### Running the WorkTimeMiner

```bash
# Build the project
cd /path/to/project
./gradlew build

# Run the WorkTimeMiner
java -jar build/libs/merged-java-shadow-1.0-SNAPSHOT-all.jar run-miner /path/to/repo main ./work-time-output.json
```

### Java Code Example: Initializing and Running a Miner

```java
// Create the miner
File repositoryFile = new File("/path/to/repo");
Set<String> branches = new HashSet<>();
branches.add("main");

WorkTimeMiner miner = new WorkTimeMiner(
    repositoryFile,  // Repository location
    branches,        // Branch to analyze
    null,            // Number of commits (null = all)
    4                // Number of threads
);

// Create the data processor
WorkTimeDataProcessor processor = new WorkTimeDataProcessor();

// Run the miner
miner.run(processor);

// Get results
Map<Integer, Map<Integer, Integer>> workTimeMatrix = processor.getWorkTimeMatrix();
Map<Integer, String> userIdToName = processor.getUserIdToName();

// Process or save results
writeResultsToFile(workTimeMatrix, userIdToName, "./output.json");
```

## Available Miners

The original Kotlin project had several miners that have been migrated to Java:

1. **WorkTimeMiner**: Analyzes when developers make commits
2. **CoEditNetworksMiner**: Detects developers working on the same files
3. **CommitInfluenceGraphMiner**: Analyzes how commits influence each other
4. **FilesOwnershipMiner**: Tracks file ownership over time

## JSON Output Format

The Java implementation generates JSON output similar to the Kotlin version:

```json
{
  "workTimeMatrix": {
    "Developer1": {
      "201": 5,
      "202": 3,
      "...": "..."
    },
    "Developer2": {
      "...": "..."
    }
  },
  "users": {
    "0": "Developer1",
    "1": "Developer2"
  }
}
```

## Conclusion

This Java implementation successfully replicates the core functionality of the original Kotlin TNM repository mining tools. While Java requires more verbose code in certain areas, the overall architecture and capabilities remain the same.

The migration demonstrates that it's feasible to convert Kotlin-based mining tools to Java while maintaining the same functionality, albeit with some adaptations to accommodate language differences.