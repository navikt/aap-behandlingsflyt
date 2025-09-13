package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.komponenter.type.Periode

data class OvergangArbeidFaktagrunnlag(
    val rettighetsperiode: Periode,
    val overgangArbeidGrunnlag: OvergangArbeidGrunnlag,
) : Faktagrunnlag