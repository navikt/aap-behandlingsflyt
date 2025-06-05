package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.APITag

enum class Tags(override val description: String) : APITag {
    Sak("Endepunkter kun relatert til sak."),
    Behandling(
        "Endepunkter relatert til behanddling."
    ),
    MottaHendelse("Endepunkter relatert til innsending av dokumenter."),
    Grunnlag(""),
    Dokumenter("Endepunkter relatert til uthenting og behandling av dokumenter."),
    Aktivitetsplikt("Relatert til aktivitetsplikt")
}