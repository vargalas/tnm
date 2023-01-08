package cli.gitMinersCLI

import cli.gitMinersCLI.base.GitMinerMultithreadedMultipleBranchesCLI
import dataProcessor.DissertationDataProcessor
import miners.gitMiners.DissertationMiner
import util.DissertationHelpFunctionsUtil
import util.HelpFunctionsUtil
import java.io.File

class DissertationMinerCLI : GitMinerMultithreadedMultipleBranchesCLI(
    "DissertationMiner",
    "Miner yields a $dissertationHelp"
) {

    companion object {
        const val dissertationHelp =
            "JSON file with a map, where key is the user id of a developer, " +
                    "and value is the list of file ids for the files edited by a developer."
        const val LONGNAME_CHANGED_FILES_BY_USER = "--changed-files-by-users"
    }

    private val changedFilesByUsersJsonFile by saveFileOption(
        LONGNAME_CHANGED_FILES_BY_USER,
        dissertationHelp,
        File(resultDir, "Dissertation.csv")
    )

    override fun run() {
        val dataProcessor = DissertationDataProcessor()


        val miner = DissertationMiner(repositoryDirectory, branches, numThreads = 1)
        miner.run(dataProcessor)
        DissertationHelpFunctionsUtil.saveToCsv(changedFilesByUsersJsonFile, dataProcessor.dissertationResult)

    }
}
