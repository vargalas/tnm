package dataProcessor.inputData

import kotlinx.serialization.*
import dataProcessor.inputData.InputData

@Serializable
data class DissertationResult(
    var sizeOfCommits: Long= 0,
    var numberOfCommits: Long = 0
) : InputData {
    constructor(size: Long) : this(size, 1)
    constructor() : this(0, 0)
}