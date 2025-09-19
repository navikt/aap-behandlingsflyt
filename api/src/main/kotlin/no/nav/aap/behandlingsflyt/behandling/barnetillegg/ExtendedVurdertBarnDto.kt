package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import java.time.LocalDate

class ExtendedVurdertBarnDto(
    ident: String?,
    navn: String?,
    vurderinger: List<VurderingAvForeldreAnsvarDto>,
    fødselsdato: LocalDate?,
    oppgittForeldreRelasjon: OppgitteBarn.Relasjon? = null,
) : VurdertBarnDto(ident, navn, fødselsdato, vurderinger, oppgittForeldreRelasjon)