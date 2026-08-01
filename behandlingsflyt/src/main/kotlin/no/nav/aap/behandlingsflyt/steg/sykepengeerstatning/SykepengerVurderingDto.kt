package no.nav.aap.behandlingsflyt.steg.sykepengeerstatning

import java.time.LocalDate
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.sykepengererstatning.SykepengerGrunn
import no.nav.aap.verdityper.dokument.JournalpostId

data class PeriodisertSykepengerVurderingDto(
    override val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val grunn: SykepengerGrunn? = null,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
): LøsningForPeriode