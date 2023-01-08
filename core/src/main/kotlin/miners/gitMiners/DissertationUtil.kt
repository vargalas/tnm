package miners.gitMiners

import dataProcessor.inputData.CommitInfoDissertation
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.revwalk.RevCommit

object DissertationUtil {
    private const val ADD_MARK = '+'
    private const val DELETE_MARK = '-'
    private const val DIFF_MARK = '@'
    private val changeLinesRegex = Regex("@@ -(\\d+)(,\\d+)? \\+(\\d+)(,\\d+)? @@")
    private val bugFixRegex = Regex("\\b[Ff]ix:?\\b")

    /**
     * Get changed files between [commit].
     *
     * @param commit RevCommit
     * @param reader must be created from the same Repository as git
     * @param git must be created from the same Repository as [reader]
     * @return set of changed files ids
     */
    fun getDissertationData(
        commit: RevCommit,
        reader: ObjectReader,
        git: Git
    ): CommitInfoDissertation {
        val result = CommitInfoDissertation(commit)
        return result
    }
}
