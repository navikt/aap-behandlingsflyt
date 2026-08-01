package no.nav.aap.samordning.annenlovgivning

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.komponenter.type.Periode

data class SamordningAnnenLovgivningFaktagrunnlag(
    val rettighetsperiode: Periode,
    val sykestipendGrunnlag: SykestipendGrunnlag?
) : Faktagrunnlag