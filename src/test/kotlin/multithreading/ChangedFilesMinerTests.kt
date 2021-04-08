package multithreading

import GitMinerTest
import GitMinerTest.Companion.repository
import dataProcessor.ChangedFilesDataProcessor
import miners.gitMiners.ChangedFilesMiner
import org.junit.Test
import util.ProjectConfig
import kotlin.test.assertTrue

internal class ChangedFilesMinerTests : GitMinerTest {
    @Test
    fun `test one thread and multithreading`() {
        val mapOneThread = runMiner(1)
        val mapMultithreading = runMiner()

        compareMapOfSets(mapOneThread, mapMultithreading)
    }

    private fun runMiner(numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS): Map<String, Set<String>> {
        val dataProcessor = ChangedFilesDataProcessor()
        val miner = ChangedFilesMiner(repository, numThreads = numThreads)
        miner.run(dataProcessor)

        assertTrue(dataProcessor.userFilesIds.isNotEmpty())

        return changeIdsToValuesInMapOfSets(
            dataProcessor.userFilesIds,
            dataProcessor.idToUser,
            dataProcessor.idToFile
        )
    }
}