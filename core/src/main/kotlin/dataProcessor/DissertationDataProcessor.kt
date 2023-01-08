package dataProcessor

import dataProcessor.inputData.CommitInfoDissertation
import dataProcessor.inputData.DissertationResult
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class DissertationDataProcessor : DataProcessorMapped<CommitInfoDissertation>() {

    private val _result = TreeMap<String, DissertationResult>()

    val dissertationResult: Map<String, DissertationResult>
        get() = _result

    override fun processData(data: CommitInfoDissertation) {
        val localDate: LocalDate = Date(data.date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val year = localDate.year
        val month = localDate.monthValue

        val dateString = "$year.$month"
        _result.computeIfPresent(dateString) { _, v ->
            DissertationResult(
                v.sizeOfCommits + data.commitSize,
                v.numberOfCommits + 10
            )
        }
        _result.computeIfAbsent(dateString) { DissertationResult(data.commitSize) };

    }

    override fun calculate() {}
}