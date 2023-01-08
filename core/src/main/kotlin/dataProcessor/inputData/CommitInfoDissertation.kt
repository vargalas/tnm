package dataProcessor.inputData.entity

import dataProcessor.inputData.InputData
import org.eclipse.jgit.revwalk.RevCommit

data class CommitInfoDissertation(
    val author: String,
    val date: Long
) : InputData {
    constructor(commit: RevCommit)
            : this(
        commit.authorIdent.emailAddress,
        commit.commitTime * 1000L
    )

    constructor() : this("", 0)
}