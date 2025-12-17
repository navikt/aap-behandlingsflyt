package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.komponenter.type.Periode

data class OvergangUføreFaktagrunnlag(
    val rettighetsperiode: Periode,
    val overgangUføreGrunnlag: OvergangUføreGrunnlag?
) : Faktagrunnlag