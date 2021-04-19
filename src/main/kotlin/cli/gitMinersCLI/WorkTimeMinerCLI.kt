package cli.gitMinersCLI

import cli.InfoCLI
import cli.gitMinersCLI.base.GitMinerMultithreadedMultipleBranchesCLI
import dataProcessor.WorkTimeDataProcessor
import miners.gitMiners.WorkTimeMiner
import util.ProjectConfig
import util.UtilFunctions
import java.io.File

class WorkTimeMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    InfoCLI(
        "WorkTimeMiner",
        "Miner yields a JSON file ${ProjectConfig.WORKTIME_DISTRIBUTION} with a map of maps, where the outer " +
                "key is user id, the inner key is the minutes passed from the beginning of the week, and the value is " +
                "the number of commits made by user at that minute in week. The first day of the week is SUNDAY."
    )
) {
    override fun run() {
        val dataProcessor = WorkTimeDataProcessor()
        val miner = WorkTimeMiner(repository, branches, numThreads = numThreads)
        miner.run(dataProcessor)

        UtilFunctions.saveToJson(
            File(resources, ProjectConfig.WORKTIME_DISTRIBUTION),
            dataProcessor.workTimeDistribution
        )
        UtilFunctions.saveToJsonDataProcessorMaps(resources, dataProcessor)
    }
}