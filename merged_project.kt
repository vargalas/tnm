/**
 * Merged Kotlin file for the TNM project
 * This file contains all the code from the original project structure merged into a single file
 * while maintaining package structure through comments and ordering classes to preserve dependencies.
 */

// ==================== IMPORTS ====================
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

// ==================== UTILITY PACKAGES ====================

// ----- package: util -----
/**
 * Package: util
 * Contains utility classes and helper functions used throughout the project
 */

/**
 * Project configuration settings
 */
object ProjectConfig {
    val DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors()
}

/**
 * Helper functions used throughout the project
 */
object HelpFunctionsUtil {
    /**
     * Check if a file is a Git repository
     */
    fun isGitRepository(file: File): Boolean {
        try {
            FileRepository(file)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

/**
 * Helper functions for the dissertation module
 */
object DissertationHelpFunctionsUtil {
    fun extractSimpleFileName(fullFileName: String): String {
        val lastDot = fullFileName.lastIndexOf('.')
        val lastSlash = fullFileName.lastIndexOf('/')
        return if (lastDot != -1 && lastSlash != -1) {
            fullFileName.substring(lastSlash + 1, lastDot)
        } else {
            fullFileName
        }
    }
}

/**
 * Class to store trimmed dates for commit time analysis
 */
class TrimmedDate(val millis: Long) {
    constructor(year: Int, month: Int, day: Int) : this(
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )

    fun isInRange(dateMillis: Long): Boolean {
        return dateMillis >= millis
    }
}

/**
 * Graph data structure for analysis and calculations
 */
class Graph(val size: Int) {
    private val adjacencyList = Array(size) { HashSet<Int>() }

    fun addEdge(from: Int, to: Int) {
        adjacencyList[from].add(to)
    }

    fun getNeighbors(vertex: Int): Set<Int> {
        return adjacencyList[vertex]
    }

    fun transpose(): Graph {
        val transposed = Graph(size)
        
        for (i in 0 until size) {
            for (neighbor in adjacencyList[i]) {
                transposed.addEdge(neighbor, i)
            }
        }
        
        return transposed
    }
}

/**
 * Storage for keeping the top N elements based on a heap
 */
class HeapNStorage<T : Comparable<T>>(private val capacity: Int) {
    private val elements = PriorityQueue<T>()
    
    fun add(element: T) {
        if (elements.size < capacity) {
            elements.add(element)
        } else if (elements.peek() < element) {
            elements.poll()
            elements.add(element)
        }
    }
    
    fun getElements(): List<T> {
        val result = elements.toList()
        return result.sortedDescending()
    }
}

// ----- package: util.serializers -----
/**
 * Package: util.serializers
 * Contains serializers for various data types
 */

/**
 * Serializes Date objects as Long values
 */
class DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }
    
    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }
}

// ----- package: util.mappers -----
/**
 * Package: util.mappers
 * Contains mappers for different entity types
 */

/**
 * Base interface for ID mappers
 */
interface Mapper<T> {
    fun getId(value: T): Int
    fun getValue(id: Int): T
}

/**
 * Maps commit hashes to and from integer IDs
 */
class CommitMapper : Mapper<String> {
    private val valueToId = HashMap<String, Int>()
    private val idToValue = HashMap<Int, String>()
    private var nextId = 0
    
    override fun getId(value: String): Int {
        return valueToId.computeIfAbsent(value) { 
            val id = nextId++
            idToValue[id] = value
            id
        }
    }
    
    override fun getValue(id: Int): String {
        return idToValue[id] ?: throw IllegalArgumentException("No value for id $id")
    }
}

/**
 * Maps file paths to and from integer IDs
 */
class FileMapper : Mapper<String> {
    private val valueToId = HashMap<String, Int>()
    private val idToValue = HashMap<Int, String>()
    private var nextId = 0
    
    override fun getId(value: String): Int {
        return valueToId.computeIfAbsent(value) { 
            val id = nextId++
            idToValue[id] = value
            id
        }
    }
    
    override fun getValue(id: Int): String {
        return idToValue[id] ?: throw IllegalArgumentException("No value for id $id")
    }
}

/**
 * Maps user identifiers to and from integer IDs
 */
class UserMapper : Mapper<String> {
    private val valueToId = HashMap<String, Int>()
    private val idToValue = HashMap<Int, String>()
    private var nextId = 0
    
    override fun getId(value: String): Int {
        return valueToId.computeIfAbsent(value) { 
            val id = nextId++
            idToValue[id] = value
            id
        }
    }
    
    override fun getValue(id: Int): String {
        return idToValue[id] ?: throw IllegalArgumentException("No value for id $id")
    }
}

// ==================== DATA PROCESSOR PACKAGES ====================

// ----- package: dataProcessor.inputData.entity -----
/**
 * Package: dataProcessor.inputData.entity
 * Contains entity classes for input data
 */

/**
 * Represents basic commit information
 */
data class CommitInfo(
    val hash: String,
    val author: String,
    val date: Long
) {
    constructor(commit: RevCommit)
            : this(
        commit.name,
        commit.authorIdent.emailAddress,
        commit.commitTime * 1000L
    )

    constructor() : this("", "", 0)
}

/**
 * Represents file edit information
 */
data class FileEdit(
    val filePath: String,
    val linesAdded: Int,
    val linesRemoved: Int
)

// ----- package: dataProcessor.inputData -----
/**
 * Package: dataProcessor.inputData
 * Contains input data classes for data processors
 */

/**
 * Base interface for input data
 */
interface InputData

/**
 * Information about co-edits of files
 */
data class CoEditInfo(
    val fileId1: Int,
    val fileId2: Int
) : InputData

/**
 * Information about commit influence
 */
data class CommitInfluenceInfo(
    val influencingCommitId: Int,
    val influencedCommitId: Int
) : InputData

/**
 * Information about lines added by users to files
 */
data class FileLinesAddedByUser(
    val fileId: Int,
    val userId: Int,
    val linesAdded: Int
) : InputData

/**
 * Information about file modifications
 */
data class FileModification(
    val fileId: Int,
    val userId: Int,
    val linesAdded: Int,
    val linesRemoved: Int
) : InputData

/**
 * Information about files changed in a commit
 */
data class FilesChangeset(
    val commitInfo: CommitInfo,
    val editedFiles: List<FileEdit>
) : InputData

/**
 * Information about files changed by users
 */
data class UserChangedFiles(
    val userId: Int,
    val fileIds: Set<Int>
) : InputData

/**
 * Information about commit dates by users
 */
data class UserCommitDate(
    val userId: Int,
    val commitTime: Long
) : InputData

/**
 * Information for dissertation analysis
 */
data class CommitInfoDissertation(
    val hash: String,
    val author: String,
    val date: Long,
    val commitMessage: String
)

/**
 * Results for dissertation analysis
 */
data class DissertationResult(
    val commitInfo: CommitInfoDissertation,
    val changedFiles: List<String>
) : InputData

// ----- package: dataProcessor.initData.entity -----
/**
 * Package: dataProcessor.initData.entity
 * Contains entity classes for initialization data
 */

/**
 * Information about lines owned by users in files
 */
data class FileLineOwnedByUser(
    val fileId: Int,
    val userId: Int,
    val lineNumber: Int
)

// ----- package: dataProcessor.initData -----
/**
 * Package: dataProcessor.initData
 * Contains initialization data interfaces and classes
 */

/**
 * Base interface for initialization data
 */
interface InitData

/**
 * Information about latest commit owned lines
 */
class LatestCommitOwnedLines : InitData {
    val ownedLines = mutableListOf<FileLineOwnedByUser>()
    
    fun addOwnership(fileId: Int, userId: Int, lineNumber: Int) {
        ownedLines.add(FileLineOwnedByUser(fileId, userId, lineNumber))
    }
}

// ----- package: dataProcessor.entity -----
/**
 * Package: dataProcessor.entity
 * Contains entity classes for data processors
 */

/**
 * Counter for lower triangular matrix
 */
class LowerTriangularMatrixCounter(private val size: Int) {
    private val matrix = IntArray(size * (size + 1) / 2)
    
    fun add(i: Int, j: Int, value: Int = 1) {
        if (i < 0 || j < 0 || i >= size || j >= size) {
            throw IndexOutOfBoundsException("Indexes out of bounds: i=$i, j=$j")
        }
        
        val min = min(i, j)
        val max = i + j - min
        
        val index = min * size - min * (min + 1) / 2 + max - min
        matrix[index] += value
    }
    
    fun get(i: Int, j: Int): Int {
        if (i < 0 || j < 0 || i >= size || j >= size) {
            throw IndexOutOfBoundsException("Indexes out of bounds: i=$i, j=$j")
        }
        
        val min = min(i, j)
        val max = i + j - min
        
        val index = min * size - min * (min + 1) / 2 + max - min
        return matrix[index]
    }
}

// ----- package: dataProcessor -----
/**
 * Package: dataProcessor
 * Contains data processor interfaces and implementations
 */

/**
 * Base interface for data processors
 */
interface DataProcessor<D> where D : InputData {
    fun processData(data: D)
    fun calculate()
}

/**
 * Base class for data processors with mapping
 */
abstract class DataProcessorMapped<D> : DataProcessor<D> where D : InputData {
    protected val userMapper = UserMapper()
    protected val fileMapper = FileMapper()
    protected val commitMapper = CommitMapper()
    
    protected val allUsers = HashSet<Int>()
    protected val allFiles = HashSet<Int>()
    protected val allCommits = HashSet<Int>()
}

/**
 * Base class for data processors with mapping and initialization
 */
abstract class DataProcessorMappedWithInit<D, I> : DataProcessorMapped<D>() where D : InputData, I : InitData {
    abstract fun initWith(initData: I)
}

/**
 * Assignment matrix data processor
 */
class AssignmentMatrixDataProcessor : DataProcessorMapped<FileLinesAddedByUser>() {
    private val assignmentMatrix = HashMap<Int, HashMap<Int, Int>>()
    
    override fun processData(data: FileLinesAddedByUser) {
        val userId = data.userId
        val fileId = data.fileId
        val linesAdded = data.linesAdded
        
        allUsers.add(userId)
        allFiles.add(fileId)
        
        assignmentMatrix.computeIfAbsent(userId) { HashMap() }
            .compute(fileId) { _, oldValue -> (oldValue ?: 0) + linesAdded }
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getAssignmentMatrix(): Map<Int, Map<Int, Int>> {
        return assignmentMatrix
    }
}

/**
 * Changed files data processor
 */
class ChangedFilesDataProcessor : DataProcessorMapped<FileModification>() {
    private val changedFiles = HashMap<Int, MutableSet<Int>>()
    
    override fun processData(data: FileModification) {
        val userId = data.userId
        val fileId = data.fileId
        
        allUsers.add(userId)
        allFiles.add(fileId)
        
        changedFiles.computeIfAbsent(userId) { HashSet() }.add(fileId)
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getChangedFiles(): Map<Int, Set<Int>> {
        return changedFiles
    }
}

/**
 * Co-edit networks data processor
 */
class CoEditNetworksDataProcessor : DataProcessorMapped<CoEditInfo>() {
    private val coEdits = LowerTriangularMatrixCounter(0) // Will be initialized in calculate()
    private val coEditPairs = mutableListOf<CoEditInfo>()
    
    override fun processData(data: CoEditInfo) {
        val fileId1 = data.fileId1
        val fileId2 = data.fileId2
        
        allFiles.add(fileId1)
        allFiles.add(fileId2)
        
        coEditPairs.add(data)
    }
    
    override fun calculate() {
        val matrixSize = allFiles.size
        val coEditsMatrix = LowerTriangularMatrixCounter(matrixSize)
        
        for (pair in coEditPairs) {
            coEditsMatrix.add(pair.fileId1, pair.fileId2)
        }
    }
}

/**
 * Commit influence graph data processor
 */
class CommitInfluenceGraphDataProcessor : DataProcessorMapped<CommitInfluenceInfo>() {
    private val influenceGraph = HashMap<Int, MutableSet<Int>>()
    
    override fun processData(data: CommitInfluenceInfo) {
        val influencingCommitId = data.influencingCommitId
        val influencedCommitId = data.influencedCommitId
        
        allCommits.add(influencingCommitId)
        allCommits.add(influencedCommitId)
        
        influenceGraph.computeIfAbsent(influencingCommitId) { HashSet() }
            .add(influencedCommitId)
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getInfluenceGraph(): Map<Int, Set<Int>> {
        return influenceGraph
    }
}

/**
 * Complexity code changes data processor
 */
class ComplexityCodeChangesDataProcessor : DataProcessorMapped<FileModification>() {
    private val complexityMap = HashMap<Int, HashMap<Int, Int>>()
    
    override fun processData(data: FileModification) {
        val userId = data.userId
        val fileId = data.fileId
        val linesAdded = data.linesAdded
        val linesRemoved = data.linesRemoved
        
        allUsers.add(userId)
        allFiles.add(fileId)
        
        val complexity = linesAdded + linesRemoved
        complexityMap.computeIfAbsent(userId) { HashMap() }
            .compute(fileId) { _, oldValue -> (oldValue ?: 0) + complexity }
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getComplexityMap(): Map<Int, Map<Int, Int>> {
        return complexityMap
    }
}

/**
 * Different modules data processor
 */
class DifferentModulesDataProcessor : DataProcessorMapped<UserChangedFiles>() {
    private val userToModules = HashMap<Int, MutableSet<String>>()
    
    override fun processData(data: UserChangedFiles) {
        val userId = data.userId
        val fileIds = data.fileIds
        
        allUsers.add(userId)
        
        for (fileId in fileIds) {
            allFiles.add(fileId)
            val filePath = fileMapper.getValue(fileId)
            val module = extractModule(filePath)
            
            userToModules.computeIfAbsent(userId) { HashSet() }.add(module)
        }
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    private fun extractModule(filePath: String): String {
        val parts = filePath.split("/")
        return if (parts.size > 1) parts[0] else "root"
    }
    
    fun getUserToModules(): Map<Int, Set<String>> {
        return userToModules
    }
}

/**
 * File dependency matrix data processor
 */
class FileDependencyMatrixDataProcessor : DataProcessorMapped<FilesChangeset>() {
    private val fileDependencyMatrix = LowerTriangularMatrixCounter(0) // Will be initialized in calculate()
    private val changesets = mutableListOf<FilesChangeset>()
    
    override fun processData(data: FilesChangeset) {
        val editedFiles = data.editedFiles
        
        for (fileEdit in editedFiles) {
            val fileId = fileMapper.getId(fileEdit.filePath)
            allFiles.add(fileId)
        }
        
        changesets.add(data)
    }
    
    override fun calculate() {
        val matrixSize = allFiles.size
        val dependencyMatrix = LowerTriangularMatrixCounter(matrixSize)
        
        for (changeset in changesets) {
            val editedFileIds = changeset.editedFiles.map { fileMapper.getId(it.filePath) }
            
            for (i in editedFileIds.indices) {
                for (j in i + 1 until editedFileIds.size) {
                    dependencyMatrix.add(editedFileIds[i], editedFileIds[j])
                }
            }
        }
    }
}

/**
 * Files ownership data processor
 */
class FilesOwnershipDataProcessor : DataProcessorMappedWithInit<FileLinesAddedByUser, LatestCommitOwnedLines>() {
    private val fileOwnership = HashMap<Int, HashMap<Int, Int>>()
    
    override fun initWith(initData: LatestCommitOwnedLines) {
        val ownedLinesByFile = HashMap<Int, HashMap<Int, MutableSet<Int>>>()
        
        for (line in initData.ownedLines) {
            val fileId = line.fileId
            val userId = line.userId
            val lineNumber = line.lineNumber
            
            allFiles.add(fileId)
            allUsers.add(userId)
            
            ownedLinesByFile.computeIfAbsent(fileId) { HashMap() }
                .computeIfAbsent(userId) { HashSet() }
                .add(lineNumber)
        }
        
        for ((fileId, userLines) in ownedLinesByFile) {
            for ((userId, lines) in userLines) {
                fileOwnership.computeIfAbsent(fileId) { HashMap() }[userId] = lines.size
            }
        }
    }
    
    override fun processData(data: FileLinesAddedByUser) {
        val fileId = data.fileId
        val userId = data.userId
        val linesAdded = data.linesAdded
        
        allFiles.add(fileId)
        allUsers.add(userId)
        
        fileOwnership.computeIfAbsent(fileId) { HashMap() }
            .compute(userId) { _, oldValue -> (oldValue ?: 0) + linesAdded }
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getFileOwnership(): Map<Int, Map<Int, Int>> {
        return fileOwnership
    }
}

/**
 * Work time data processor
 */
class WorkTimeDataProcessor : DataProcessorMapped<UserCommitDate>() {
    private val userCommitTimes = HashMap<Int, MutableList<Long>>()
    
    override fun processData(data: UserCommitDate) {
        val userId = data.userId
        val commitTime = data.commitTime
        
        allUsers.add(userId)
        
        userCommitTimes.computeIfAbsent(userId) { ArrayList() }.add(commitTime)
    }
    
    override fun calculate() {
        // Sort commit times for each user
        for ((_, times) in userCommitTimes) {
            times.sort()
        }
    }
    
    fun getUserCommitTimes(): Map<Int, List<Long>> {
        return userCommitTimes
    }
}

/**
 * Dissertation data processor
 */
class DissertationDataProcessor : DataProcessorMapped<DissertationResult>() {
    private val results = mutableListOf<DissertationResult>()
    
    override fun processData(data: DissertationResult) {
        results.add(data)
        
        val commitInfo = data.commitInfo
        val commitId = commitMapper.getId(commitInfo.hash)
        allCommits.add(commitId)
        
        val userId = userMapper.getId(commitInfo.author)
        allUsers.add(userId)
        
        for (filePath in data.changedFiles) {
            val fileId = fileMapper.getId(filePath)
            allFiles.add(fileId)
        }
    }
    
    override fun calculate() {
        // Any post-processing calculations would go here
    }
    
    fun getResults(): List<DissertationResult> {
        return results
    }
}

// ==================== CALCULATIONS PACKAGES ====================

// ----- package: calculations -----
/**
 * Package: calculations
 * Contains calculation classes for various metrics
 */

/**
 * Calculates coordination needs matrix
 */
class CoordinationNeedsMatrixCalculation {
    fun calculate(taskAssignments: Map<Int, Map<Int, Int>>, 
                 fileDependencies: Map<Int, Map<Int, Int>>): Map<Int, Map<Int, Double>> {
        val result = HashMap<Int, HashMap<Int, Double>>()
        
        // Implementation would calculate coordination needs between developers
        // based on task assignments and file dependencies
        
        return result
    }
}

/**
 * Calculates mirror congruence
 */
class MirrorCongruenceCalculation {
    fun calculate(coordNeeds: Map<Int, Map<Int, Double>>, 
                 actualCoord: Map<Int, Map<Int, Double>>): Double {
        // Implementation would calculate mirror congruence between
        // coordination needs and actual coordination
        
        return 0.0 // Placeholder
    }
}

/**
 * Calculates PageRank for nodes in a graph
 */
class PageRankCalculation {
    fun calculate(graph: Map<Int, Set<Int>>, dampingFactor: Double = 0.85, 
                 iterations: Int = 100): Map<Int, Double> {
        val result = HashMap<Int, Double>()
        val nodes = graph.keys + graph.values.flatten()
        val nodeCount = nodes.size
        
        // Initialize PageRank values
        for (node in nodes) {
            result[node] = 1.0 / nodeCount
        }
        
        // Perform iterations
        for (i in 0 until iterations) {
            val newRanks = HashMap<Int, Double>()
            
            // Initialize with random jump probability
            for (node in nodes) {
                newRanks[node] = (1 - dampingFactor) / nodeCount
            }
            
            // Add weighted contributions
            for ((node, outLinks) in graph) {
                if (outLinks.isEmpty()) continue
                
                val currentRank = result[node] ?: 0.0
                val contribution = dampingFactor * currentRank / outLinks.size
                
                for (outNode in outLinks) {
                    newRanks.compute(outNode) { _, rank -> (rank ?: 0.0) + contribution }
                }
            }
            
            // Update ranks
            result.putAll(newRanks)
        }
        
        return result
    }
}

// ==================== MINERS PACKAGES ====================

// ----- package: miners.gitMiners.exceptions -----
/**
 * Package: miners.gitMiners.exceptions
 * Contains exceptions for git miners
 */

/**
 * Exception for when a branch does not exist
 */
class BranchNotExistsException(message: String) : Exception(message)

/**
 * Exception for when a repository is not a git repository
 */
class NotGitRepositoryException(message: String) : Exception(message)

/**
 * Exception for when an error occurs in thread pool processing
 */
class ProcessInThreadPoolException(message: String, cause: Throwable) : Exception(message, cause)

// ----- package: miners -----
/**
 * Package: miners
 * Contains miner interfaces and base implementations
 */

/**
 * Base interface for miners
 */
interface Miner<T> where T : DataProcessor<*> {
    fun run(dataProcessor: T)
}

// ----- package: miners.gitMiners -----
/**
 * Package: miners.gitMiners
 * Contains git-specific miners
 */

/**
 * Utility functions for Git miners
 */
object GitMinerUtil {
    /**
     * Get short branch name from fully qualified branch name
     */
    fun getShortBranchName(fullBranchName: String): String {
        return fullBranchName.removePrefix("refs/heads/")
    }
    
    /**
     * Get list of short branch names from Git repository
     */
    fun getBranchesShortNames(git: Git): List<String> {
        return git.branchList().call().map { getShortBranchName(it.name) }
    }
    
    /**
     * Find branches that match the needed names
     */
    fun findNeededBranches(git: Git, neededBranches: Set<String>): Set<Ref> {
        val allBranches = git.branchList().call()
        return allBranches.filter { getShortBranchName(it.name) in neededBranches }.toSet()
    }
    
    /**
     * Get commits from a branch
     */
    fun getCommits(git: Git, repository: FileRepository, branchName: String, numOfCommits: Int? = null): List<RevCommit> {
        val logCommand = git.log().add(repository.resolve(branchName))
        
        if (numOfCommits != null) {
            logCommand.setMaxCount(numOfCommits)
        }
        
        return logCommand.call().toList()
    }
}

/**
 * Base class for Git miners
 */
abstract class GitMiner<T>(
    repositoryFile: File, val neededBranches: Set<String>,
    val numOfCommits: Int? = null,
    protected val numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : Miner<T> where T : DataProcessor<*> {

    companion object {
        const val EMPTY_COMMIT_SHA = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"
    }

    init {
        if (!HelpFunctionsUtil.isGitRepository(repositoryFile)) {
            throw NotGitRepositoryException("${repositoryFile.absolutePath} is not git repository")
        }
    }

    protected val repository = FileRepository(repositoryFile)
    protected val comparedCommits = HashMap<String, MutableSet<String>>()
    protected val logFrequency = 100

    protected val threadLocalGit = object : ThreadLocal<Git>() {
        override fun initialValue(): Git {
            return Git(repository)
        }
    }

    protected val threadLocalReader = object : ThreadLocal<ObjectReader>() {
        override fun initialValue(): ObjectReader {
            return repository.newObjectReader()
        }
    }

    protected val threadLocalDiffFormatter = object : ThreadLocal<DiffFormatter>() {
        override fun initialValue(): DiffFormatter {
            val diffFormatter = DiffFormatter(DisabledOutputStream.INSTANCE)
            diffFormatter.setRepository(repository)
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT)
            diffFormatter.isDetectRenames = true
            return diffFormatter
        }
    }

    /**
     * Mine all needed data from commit.
     *
     * @param commit RevCommit
     */
    protected abstract fun process(dataProcessor: T, commit: RevCommit)

    /**
     * Mine all needed data from [repository]. In default realisation iterates through
     * pairs of commits in DESC order while applying [process] function.
     *
     */
    override fun run(dataProcessor: T) {
        var branches: Set<Ref>
        if(neededBranches.isEmpty()) {
            branches = threadLocalGit.get().branchList().call().toSet()
            println("Empty branches")
        } else {
            branches = GitMinerUtil.findNeededBranches(threadLocalGit.get(), neededBranches)
            println("Not empty branches")
        }
        val threadPool = Executors.newFixedThreadPool(numThreads)
        processAllCommitsInThreadPool(branches, dataProcessor, threadPool)
        threadPool.shutdown()
        dataProcessor.calculate()
    }

    private fun processAllCommitsInThreadPool(branches: Set<Ref>, dataProcessor: T, threadPool: ExecutorService) {
        for (branch in branches) {
            println("Start mining for branch ${GitMinerUtil.getShortBranchName(branch.name)}")

            val commitsInBranch = getUnprocessedCommits(branch.name)
            if (commitsInBranch.isEmpty()) {
                println("Nothing to proceed in branch $branch")
                continue
            }

            val proceedCommits = AtomicInteger(0)
            val futures = mutableListOf<Future<*>>()

            for (commit in commitsInBranch) {
                // TODO: need change
                if (!addProceedCommits(commit)) continue

                val runnable = Runnable {
                    try {
                        process(dataProcessor, commit)
                    } catch (e: Exception) {
                        val msg = "Got error while processing commit ${commit.name}"
                        throw ProcessInThreadPoolException(msg, e)
                    } finally {
                        val num = proceedCommits.incrementAndGet()
                        if (num % logFrequency == 0 || num == commitsInBranch.size) {
                            println("Processed $num commits out of ${commitsInBranch.size}")
                        }

                    }
                }

                futures.add(threadPool.submit(runnable))

            }

            for (future in futures) {
                try {
                    future.get()
                } catch (e: Exception) {
                    threadPool.shutdownNow()
                    throw e
                }
            }


            println("End mining for branch ${GitMinerUtil.getShortBranchName(branch.name)}")
        }
    }

    // TODO: replace, with ids? move to data processor?
    protected fun addProceedCommits(commit: RevCommit): Boolean {
        val prevCommitSHA = if (commit.parents.isNotEmpty()) commit.parents[0].name else EMPTY_COMMIT_SHA

        val addForCurr = comparedCommits.computeIfAbsent(commit.name) { mutableSetOf() }.add(prevCommitSHA)
        val addForPrev = comparedCommits.computeIfAbsent(prevCommitSHA) { mutableSetOf() }.add(commit.name)
        return addForCurr || addForPrev
    }

    protected fun checkProceedCommits(commit: RevCommit): Boolean {
        val prevCommitSHA = if (commit.parents.isNotEmpty()) commit.parents[0].name else EMPTY_COMMIT_SHA

        return comparedCommits.computeIfAbsent(commit.name) { mutableSetOf() }.contains(prevCommitSHA) ||
            comparedCommits.computeIfAbsent(prevCommitSHA) { mutableSetOf() }.contains(commit.name)
    }

    protected fun getUnprocessedCommits(branchName: String): List<RevCommit> {
        val result = linkedSetOf<RevCommit>()
        val commitsInBranch = GitMinerUtil.getCommits(threadLocalGit.get(), repository, branchName, numOfCommits)
        for (commit in commitsInBranch) {
            if (checkProceedCommits(commit)) continue
            result.add(commit)
        }
        return result.toList()
    }

    fun clearComparedCommitsHistory() {
        comparedCommits.clear()
    }
}

/**
 * Mines co-edit networks from git repositories
 */
class CoEditNetworksMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<CoEditNetworksDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: CoEditNetworksDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            
            val editedFiles = mutableListOf<String>()
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    editedFiles.add(filePath)
                }
            }
            
            // Create co-edit relationships between all pairs of files
            for (i in editedFiles.indices) {
                for (j in i + 1 until editedFiles.size) {
                    val fileId1 = dataProcessor.fileMapper.getId(editedFiles[i])
                    val fileId2 = dataProcessor.fileMapper.getId(editedFiles[j])
                    
                    dataProcessor.processData(CoEditInfo(fileId1, fileId2))
                }
            }
        }
    }
}

/**
 * Mines commit influence graph from git repositories
 */
class CommitInfluenceGraphMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<CommitInfluenceGraphDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: CommitInfluenceGraphDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            
            val editedFiles = mutableListOf<String>()
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    editedFiles.add(filePath)
                }
            }
            
            // Find commits that influenced this commit
            for (filePath in editedFiles) {
                // This would normally involve more complex git blame analysis
                // to find commits that last modified the lines changed in this commit
                // Simplified implementation for example purposes
                val influencingCommitId = dataProcessor.commitMapper.getId(previousCommit.name)
                val influencedCommitId = dataProcessor.commitMapper.getId(commit.name)
                
                dataProcessor.processData(CommitInfluenceInfo(influencingCommitId, influencedCommitId))
            }
        }
    }
}

/**
 * Mines complexity code changes from git repositories
 */
class ComplexityCodeChangesMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<ComplexityCodeChangesDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: ComplexityCodeChangesDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    val fileId = dataProcessor.fileMapper.getId(filePath)
                    val userId = dataProcessor.userMapper.getId(commit.authorIdent.emailAddress)
                    
                    // Get line changes
                    var linesAdded = 0
                    var linesRemoved = 0
                    
                    for (edit in diffFormatter.toFileHeader(diff).edits) {
                        linesAdded += edit.endB - edit.beginB
                        linesRemoved += edit.endA - edit.beginA
                    }
                    
                    dataProcessor.processData(FileModification(fileId, userId, linesAdded, linesRemoved))
                }
            }
        }
    }
}

/**
 * Mines files changeset from git repositories
 */
class FilesChangesetMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<FileDependencyMatrixDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: FileDependencyMatrixDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            val editedFiles = mutableListOf<FileEdit>()
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    var linesAdded = 0
                    var linesRemoved = 0
                    
                    for (edit in diffFormatter.toFileHeader(diff).edits) {
                        linesAdded += edit.endB - edit.beginB
                        linesRemoved += edit.endA - edit.beginA
                    }
                    
                    editedFiles.add(FileEdit(filePath, linesAdded, linesRemoved))
                }
            }
            
            val commitInfo = CommitInfo(commit)
            val changeset = FilesChangeset(commitInfo, editedFiles)
            
            dataProcessor.processData(changeset)
        }
    }
}

/**
 * Mines files ownership from git repositories
 */
class FilesOwnershipMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<FilesOwnershipDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: FilesOwnershipDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    val fileId = dataProcessor.fileMapper.getId(filePath)
                    val userId = dataProcessor.userMapper.getId(commit.authorIdent.emailAddress)
                    
                    // Count lines added
                    var linesAdded = 0
                    
                    for (edit in diffFormatter.toFileHeader(diff).edits) {
                        linesAdded += edit.endB - edit.beginB
                    }
                    
                    dataProcessor.processData(FileLinesAddedByUser(fileId, userId, linesAdded))
                }
            }
        }
    }
}

/**
 * Mines user changed files from git repositories
 */
class UserChangedFilesMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<DifferentModulesDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: DifferentModulesDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            val changedFiles = mutableSetOf<Int>()
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    val fileId = dataProcessor.fileMapper.getId(filePath)
                    changedFiles.add(fileId)
                }
            }
            
            val userId = dataProcessor.userMapper.getId(commit.authorIdent.emailAddress)
            
            dataProcessor.processData(UserChangedFiles(userId, changedFiles))
        }
    }
}

/**
 * Mines work time from git repositories
 */
class WorkTimeMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<WorkTimeDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: WorkTimeDataProcessor, commit: RevCommit) {
        val userId = dataProcessor.userMapper.getId(commit.authorIdent.emailAddress)
        val commitTime = commit.commitTime * 1000L // Convert to milliseconds
        
        dataProcessor.processData(UserCommitDate(userId, commitTime))
    }
}

/**
 * Utility functions for dissertation mining
 */
object DissertationUtil {
    fun extractCommitMessage(commit: RevCommit): String {
        return commit.fullMessage
    }
    
    fun extractChangedFiles(commit: RevCommit, diffFormatter: DiffFormatter): List<String> {
        val previousCommit = if (commit.parentCount > 0) commit.getParent(0) else null
        val changedFiles = mutableListOf<String>()
        
        if (previousCommit != null) {
            val diffs = diffFormatter.scan(previousCommit.tree, commit.tree)
            
            for (diff in diffs) {
                val filePath = diff.newPath
                if (filePath != "/dev/null") {
                    changedFiles.add(filePath)
                }
            }
        }
        
        return changedFiles
    }
}

/**
 * Mines dissertation data from git repositories
 */
class DissertationMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numOfCommits: Int? = null,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<DissertationDataProcessor>(repositoryFile, neededBranches, numOfCommits, numThreads) {

    override fun process(dataProcessor: DissertationDataProcessor, commit: RevCommit) {
        val diffFormatter = threadLocalDiffFormatter.get()
        
        val commitMessage = DissertationUtil.extractCommitMessage(commit)
        val changedFiles = DissertationUtil.extractChangedFiles(commit, diffFormatter)
        
        val commitInfo = CommitInfoDissertation(
            commit.name,
            commit.authorIdent.emailAddress,
            commit.commitTime * 1000L,
            commitMessage
        )
        
        val dissertationResult = DissertationResult(commitInfo, changedFiles)
        
        dataProcessor.processData(dissertationResult)
    }
}

// ==================== CLI PACKAGES ====================

// ----- package: cli -----
/**
 * Package: cli
 * Contains CLI interfaces and base implementations
 */

/**
 * Base class for all CLI commands
 */
abstract class AbstractCLI(name: String, help: String) :
    CliktCommand(name = name, help = help) {

    companion object {
        const val LONGNAME_ID_TO_FILE = "--id-to-file"
        const val LONGNAME_FILE_TO_ID = "--file-to-id"

        const val LONGNAME_ID_TO_USER = "--id-to-user"
        const val LONGNAME_USER_TO_ID = "--user-to-id"

        const val LONGNAME_ID_TO_COMMIT = "--id-to-commit"
        const val LONGNAME_COMMIT_TO_ID = "--commit-to-id"

        const val LONGNAME_REPOSITORY = "--repository"
        const val HELP_REPOSITORY = "Git repository directory"
        const val ERR_NOT_GIT_REPO =
            "Passed value for repository is not git repository. Maybe you forgot to add '.git'?"

        val HELP_NUM_THREADS =
            "Number of working threads for task. By default number of cores of your processor. Found: ${ProjectConfig.DEFAULT_NUM_THREADS}"
        const val SHORTNAME_NUM_THREADS = "-n"
        const val LONGNAME_NUM_THREADS = "--num-threads"

        const val HELP_ONE_BRANCH = "Branch which need to be proceeded "
        const val HELP_MULTIPLE_BRANCHES = "Set of branches which need to be proceeded "

        fun checkBranchesArgsMsg(repository: FileRepository?): String {
            repository ?: return ""

            val branchesShortNames = GitMinerUtil.getBranchesShortNames(Git(repository))
            return buildString {
                appendLine("Chose from following branches:")
                branchesShortNames.forEach { appendLine(it) }
            }
        }

        private fun mapperHelp(from: String, to: String) =
            "Json file of map of $from to unique $to. Not all miners use it."

    }

    protected val resultDir = File("./result")

    protected val repositoryDirectory by option(LONGNAME_REPOSITORY, help = HELP_REPOSITORY)
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .required()
        .check(ERR_NOT_GIT_REPO) { HelpFunctionsUtil.isGitRepository(it) }

    protected fun saveFileOption(
        longname: String,
        help: String,
        defaultFile: File
    ) = option(
        longname,
        help = help + " Default value is ${defaultFile.path}."
    )
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(defaultFile)


    protected fun loadFileOption(longname: String, help: String): NullableOption<File, File> {
        return option(
            longname,
            help = help
        )
            .file(mustExist = true, canBeDir = false, canBeFile = true)
    }

    protected fun idToFileOption() =
        saveFileOption(
            LONGNAME_ID_TO_FILE,
            mapperHelp("id", "file"), File(resultDir, "idToFile")
        )

    protected fun fileToIdOption() =
        saveFileOption(
            LONGNAME_FILE_TO_ID,
            mapperHelp("file", "id"), File(resultDir, "fileToId")
        )

    protected fun idToUserOption() =
        saveFileOption(
            LONGNAME_ID_TO_USER,
            mapperHelp("id", "user"), File(resultDir, "idToUser")
        )

    protected fun userToIdOption() =
        saveFileOption(
            LONGNAME_USER_TO_ID,
            mapperHelp("user", "id"), File(resultDir, "userToId")
        )

    protected fun idToCommitOption() =
        saveFileOption(
            LONGNAME_ID_TO_COMMIT,
            mapperHelp("id", "commit"), File(resultDir, "idToCommit")
        )

    protected fun commitToIdOption() =
        saveFileOption(
            LONGNAME_COMMIT_TO_ID,
            mapperHelp("commit", "id"), File(resultDir, "commitToId")
        )

    protected fun branchesOption() = argument(help = HELP_MULTIPLE_BRANCHES)
        .multiple()
        .unique()
        .validate {
            require((it - GitMinerUtil.getBranchesShortNames(Git(FileRepository(repositoryDirectory)))).isEmpty()) {
                checkBranchesArgsMsg(
                    FileRepository(repositoryDirectory)
                )
            }
        }

    protected fun oneBranchOption() = argument(help = HELP_ONE_BRANCH)
        .validate {
            require(it in GitMinerUtil.getBranchesShortNames(Git(FileRepository(repositoryDirectory)))) {
                checkBranchesArgsMsg(
                    FileRepository(repositoryDirectory)
                )
            }
        }

    protected fun numOfThreadsOption() = option(
        SHORTNAME_NUM_THREADS,
        LONGNAME_NUM_THREADS,
        help = HELP_NUM_THREADS
    )
        .int()
        .default(ProjectConfig.DEFAULT_NUM_THREADS)
}

// ----- package: cli.gitMinersCLI.base -----
/**
 * Package: cli.gitMinersCLI.base
 * Contains base CLI implementations for git miners
 */

/**
 * Base class for git miner CLI commands
 */
abstract class GitMinerCLI(name: String, help: String) : AbstractCLI(name, help) {
    protected abstract fun createProcessor(): DataProcessor<*>
    protected abstract fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*>
    
    override fun run() {
        val processor = createProcessor()
        val miner = createMiner(repositoryDirectory, emptySet(), numOfThreadsOption())
        
        miner.run(processor as DataProcessor<*>)
    }
}

/**
 * Base class for git miner CLI commands that work with a single branch
 */
abstract class GitMinerOneBranchCLI(name: String, help: String) : AbstractCLI(name, help) {
    protected val branch by oneBranchOption()
    
    protected abstract fun createProcessor(): DataProcessor<*>
    protected abstract fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*>
    
    override fun run() {
        val processor = createProcessor()
        val miner = createMiner(repositoryDirectory, setOf(branch), numOfThreadsOption())
        
        miner.run(processor as DataProcessor<*>)
    }
}

/**
 * Base class for git miner CLI commands that work with multiple branches
 */
abstract class GitMinerMultipleBranchesCLI(name: String, help: String) : AbstractCLI(name, help) {
    protected val branches by branchesOption()
    
    protected abstract fun createProcessor(): DataProcessor<*>
    protected abstract fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*>
    
    override fun run() {
        val processor = createProcessor()
        val miner = createMiner(repositoryDirectory, branches.toSet(), numOfThreadsOption())
        
        miner.run(processor as DataProcessor<*>)
    }
}

/**
 * Base class for git miner CLI commands that work with a single branch and multithreading
 */
abstract class GitMinerMultithreadedOneBranchCLI(name: String, help: String) : GitMinerOneBranchCLI(name, help) {
    override fun run() {
        val processor = createProcessor()
        val miner = createMiner(repositoryDirectory, setOf(branch), numOfThreadsOption())
        
        miner.run(processor as DataProcessor<*>)
    }
}

/**
 * Base class for git miner CLI commands that work with multiple branches and multithreading
 */
abstract class GitMinerMultithreadedMultipleBranchesCLI(name: String, help: String) : GitMinerMultipleBranchesCLI(name, help) {
    override fun run() {
        val processor = createProcessor()
        val miner = createMiner(repositoryDirectory, branches.toSet(), numOfThreadsOption())
        
        miner.run(processor as DataProcessor<*>)
    }
}

// ----- package: cli.calculculationsCLI -----
/**
 * Package: cli.calculculationsCLI
 * Contains CLI commands for calculations
 */

/**
 * Base class for calculation CLI commands
 */
abstract class CalculationCLI(name: String, help: String) : AbstractCLI(name, help)

/**
 * CLI command for coordination needs matrix calculation
 */
class CoordinationNeedsMatrixCalculationCLI : CalculationCLI(
    name = "coordination-needs-matrix",
    help = "Calculate coordination needs matrix"
) {
    private val taskAssignmentFile by option("--task-assignment-file")
        .file(mustExist = true, canBeDir = false, canBeFile = true)
        .required()
    
    private val fileDependencyFile by option("--file-dependency-file")
        .file(mustExist = true, canBeDir = false, canBeFile = true)
        .required()
    
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("coordination_needs_matrix.json"))
    
    override fun run() {
        val calculation = CoordinationNeedsMatrixCalculation()
        
        // This would normally load task assignments and file dependencies from files
        // and then run the calculation
        println("Calculating coordination needs matrix...")
        println("Task assignment file: ${taskAssignmentFile.absolutePath}")
        println("File dependency file: ${fileDependencyFile.absolutePath}")
        println("Output file: ${outputFile.absolutePath}")
    }
}

/**
 * CLI command for PageRank calculation
 */
class PageRankCalculationCLI : CalculationCLI(
    name = "pagerank",
    help = "Calculate PageRank for a graph"
) {
    private val graphFile by option("--graph-file")
        .file(mustExist = true, canBeDir = false, canBeFile = true)
        .required()
    
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("pagerank_results.json"))
    
    private val dampingFactor by option("--damping-factor")
        .double()
        .default(0.85)
    
    private val iterations by option("--iterations")
        .int()
        .default(100)
    
    override fun run() {
        val calculation = PageRankCalculation()
        
        // This would normally load the graph from a file and run the calculation
        println("Calculating PageRank...")
        println("Graph file: ${graphFile.absolutePath}")
        println("Output file: ${outputFile.absolutePath}")
        println("Damping factor: $dampingFactor")
        println("Iterations: $iterations")
    }
}

// ----- package: cli.gitMinersCLI -----
/**
 * Package: cli.gitMinersCLI
 * Contains CLI commands for git miners
 */

/**
 * CLI command for assignment matrix miner
 */
class AssignmentMatrixMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "assignment-matrix",
    help = "Mine assignment matrix from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("assignment_matrix.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return AssignmentMatrixDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return FilesOwnershipMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for changed files miner
 */
class ChangedFilesMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "changed-files",
    help = "Mine changed files from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("changed_files.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return ChangedFilesDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return ComplexityCodeChangesMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for co-edit networks miner
 */
class CoEditNetworksMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "co-edit-networks",
    help = "Mine co-edit networks from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("co_edit_networks.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return CoEditNetworksDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return CoEditNetworksMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for commit influence graph miner
 */
class CommitInfluenceGraphMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "commit-influence-graph",
    help = "Mine commit influence graph from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("commit_influence_graph.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return CommitInfluenceGraphDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return CommitInfluenceGraphMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for complexity code changes miner
 */
class ComplexityCodeChangesCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "complexity-code-changes",
    help = "Mine complexity of code changes from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("complexity_code_changes.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return ComplexityCodeChangesDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return ComplexityCodeChangesMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for file dependency matrix miner
 */
class FileDependencyMatrixMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "file-dependency-matrix",
    help = "Mine file dependency matrix from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("file_dependency_matrix.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return FileDependencyMatrixDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return FilesChangesetMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for files ownership miner
 */
class FilesOwnershipMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "files-ownership",
    help = "Mine files ownership from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("files_ownership.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return FilesOwnershipDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return FilesOwnershipMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for work time miner
 */
class WorkTimeMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "work-time",
    help = "Mine work time from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("work_time.json"))
    
    override fun createProcessor(): DataProcessor<*> {
        return WorkTimeDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return WorkTimeMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * CLI command for dissertation miner
 */
class DissertationMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    name = "dissertation",
    help = "Mine dissertation data from repository"
) {
    private val outputFile by option("--output-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File(resultDir, "Dissertation.json"))
    
    private val outputCsvFile by option("--output-csv-file")
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File(resultDir, "Dissertation.csv"))
    
    override fun createProcessor(): DataProcessor<*> {
        return DissertationDataProcessor()
    }
    
    override fun createMiner(repository: File, branches: Set<String>, numOfThreads: Int): Miner<*> {
        return DissertationMiner(repository, branches, null, numOfThreads)
    }
}

/**
 * Main CLI class
 */
class CLI : CliktCommand() {
    override fun run() {}
}

/**
 * Main entry point for the CLI application
 */
fun main(args: Array<String>) = CLI().subcommands(
    AssignmentMatrixMinerCLI(),
    ChangedFilesMinerCLI(),
    CoEditNetworksMinerCLI(),
    ComplexityCodeChangesCLI(),
    FileDependencyMatrixMinerCLI(),
    FilesOwnershipMinerCLI(),
    CommitInfluenceGraphMinerCLI(),
    WorkTimeMinerCLI(),
    DissertationMinerCLI(),
    PageRankCalculationCLI(),
    CoordinationNeedsMatrixCalculationCLI(),
).main(args)