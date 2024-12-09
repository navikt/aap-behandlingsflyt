package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.APITag

enum class Tags(override val description: String) : APITag {
    Behandling(
        "Endepunkter relatert til behanddling."
    ),
}