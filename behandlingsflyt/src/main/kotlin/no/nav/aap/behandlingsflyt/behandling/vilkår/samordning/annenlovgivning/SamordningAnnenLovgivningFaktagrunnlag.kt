package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenlovgivning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.komponenter.type.Periode

data class SamordningAnnenLovgivningFaktagrunnlag(
    val rettighetsperiode: Periode,
    val sykestipendGrunnlag: SykestipendGrunnlag?
) : Faktagrunnlag