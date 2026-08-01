package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.barnetillegg.Relasjon
import no.nav.aap.behandlingsflyt.steg.barnetillegg.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.steg.barnetillegg.VurdertBarnDto
import java.time.LocalDate

class ExtendedVurdertBarnDto(
    ident: String?,
    navn: String?,
    vurderinger: List<VurderingAvForeldreAnsvarDto>,
    fødselsdato: LocalDate?,
    dødsdato: LocalDate?,
    oppgittForeldreRelasjon: Relasjon? = null,
) : VurdertBarnDto(ident, navn, fødselsdato, dødsdato, vurderinger, oppgittForeldreRelasjon)