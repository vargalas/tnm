package dataProcessor.inputData

import dataProcessor.inputData.InputData
import org.eclipse.jgit.revwalk.RevCommit

data class CommitInfoDissertation(
    val author: String,
    val date: Long,
    val commitSize: Long
) : InputData {
    constructor(commit: RevCommit)
            : this(
        commit.authorIdent.emailAddress,
        commit.commitTime * 1000L, commit.rawBuffer.size.toLong()
    )

    constructor() : this("", 0, 0)
}