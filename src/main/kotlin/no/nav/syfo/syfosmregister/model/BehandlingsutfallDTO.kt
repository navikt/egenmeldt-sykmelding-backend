package no.nav.syfo.syfosmregister.model

data class BehandlingsutfallDTO(
    val ruleHits: List<RegelinfoDTO>,
    val status: BehandlingsutfallStatusDTO
)

data class RegelinfoDTO(
    val messageForSender: String,
    val messageForUser: String,
    val ruleName: String,
    val ruleStatus: BehandlingsutfallStatusDTO?
)

enum class BehandlingsutfallStatusDTO {
    OK, MANUAL_PROCESSING, INVALID
}
