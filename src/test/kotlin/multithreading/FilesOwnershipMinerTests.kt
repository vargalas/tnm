package multithreading

import GitMinerTest
import GitMinerTest.Companion.repository
import dataProcessor.FilesOwnershipDataProcessor
import gitMiners.FilesOwnershipMiner
import org.junit.Test
import util.ProjectConfig
import kotlin.test.assertTrue

internal class FilesOwnershipMinerTests : GitMinerTest {

    @Test
    fun `test one thread and multithreading`() {
        val mapOneThread = runMiner()
        val mapMultithreading = runMiner()
        compareMapsOfMapsDouble(mapOneThread, mapMultithreading)
    }

    private fun runMiner(
        numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
    ): Map<String, Map<String, Double>> {
        val dataProcessor = FilesOwnershipDataProcessor()
        val miner = FilesOwnershipMiner(repository, numThreads = numThreads)
        miner.run(dataProcessor)

        assertTrue(dataProcessor.developerKnowledge.isNotEmpty())

        return changeIdsToValuesInMapOfMaps(
            dataProcessor.developerKnowledge,
            dataProcessor.userMapper.idToUser,
            dataProcessor.fileMapper.idToFile
        )
    }
}
