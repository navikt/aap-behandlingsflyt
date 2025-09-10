package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import java.time.LocalDate

data class OvergangArbeidFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurderinger: List<OvergangArbeidVurdering>
) : Faktagrunnlag