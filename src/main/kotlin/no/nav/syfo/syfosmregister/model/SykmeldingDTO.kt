package no.nav.syfo.syfosmregister.model

data class SykmeldingDTO(
    val id: String,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val medisinskVurdering: MedisinskVurderingDTO?,
    val behandlingsutfall: BehandlingsutfallDTO
)
