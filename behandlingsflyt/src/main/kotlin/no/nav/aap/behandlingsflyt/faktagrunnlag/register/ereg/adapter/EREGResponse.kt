package no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter


data class EnhetsregisterOrganisasjonResponse(
    val navn: EnhetsregisterOrganisasjonsNavn
)

data class EnhetsregisterOrganisasjonsNavn(
    val sammensattnavn: String
)