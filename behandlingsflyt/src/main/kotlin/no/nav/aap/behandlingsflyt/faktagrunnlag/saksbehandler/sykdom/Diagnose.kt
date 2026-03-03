package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

/**
 * @param kodeverk For å oppgi diagnose, typisk ICD-10 eller ICPC2.
 */
data class Diagnose(
    val kodeverk: String,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
)
