package no.nav.aap.barnetillegg

import java.time.LocalDate

class ExtendedVurdertBarnDto(
    ident: String?,
    navn: String?,
    vurderinger: List<VurderingAvForeldreAnsvarDto>,
    fødselsdato: LocalDate?,
    dødsdato: LocalDate?,
    oppgittForeldreRelasjon: Relasjon? = null,
) : VurdertBarnDto(ident, navn, fødselsdato, dødsdato, vurderinger, oppgittForeldreRelasjon)