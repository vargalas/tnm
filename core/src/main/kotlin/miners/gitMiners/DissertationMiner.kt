package miners.gitMiners

import dataProcessor.DataProcessor
import dataProcessor.inputData.CommitInfoDissertation
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.revwalk.RevCommit
import util.ProjectConfig
import java.io.File

class DissertationMiner(
    repositoryFile: File,
    neededBranches: Set<String>,
    numThreads: Int = ProjectConfig.DEFAULT_NUM_THREADS
) : GitMiner<DataProcessor<CommitInfoDissertation>>(repositoryFile, neededBranches, numThreads = numThreads) {

    override fun process(dataProcessor: DataProcessor<CommitInfoDissertation>, commit: RevCommit) {
        val git = threadLocalGit.get()
        // new Git(repository).branchList().setListMode(ListMode.ALL).call();
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        val reader = threadLocalReader.get()

        val dissertationData =
            reader.use {
                DissertationUtil.getDissertationData(commit, it, git)
            }
        dataProcessor.processData(dissertationData)

    }

}
