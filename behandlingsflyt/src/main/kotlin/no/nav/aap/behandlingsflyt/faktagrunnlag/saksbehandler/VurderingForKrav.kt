package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

interface VurderingForKravGrunnlag<T : VurderingForKrav> {
    val vurderinger: List<T>

    fun gjeldendeVurderinger(): List<T> {
        return this.vurderinger.gjeldendeVurderinger()
    }

    fun tilTidslinje(kravGrunnlag: KravGrunnlag?): Tidslinje<T> {
        return this.vurderinger.tilTidslinje(kravGrunnlag)
    }
}

interface VurderingForKrav {
    val referanse: Kravreferanse
    val vurdertIBehandling: BehandlingId
    val vurdertAv: Bruker
    val opprettet: Instant
}

fun <T : VurderingForKrav> List<T>.gjeldendeVurderinger(): List<T> {
    return this
        .groupBy { it.referanse }
        .values
        .map { vurderingerForKrav -> vurderingerForKrav.maxBy { it.opprettet } }
}

fun <T : VurderingForKrav> List<T>.tilTidslinje(kravGrunnlag: KravGrunnlag?): Tidslinje<T> {
    val nyesteVurderingPerKrav = gjeldendeVurderinger().associateBy { it.referanse }

    return kravGrunnlag?.kravtidslinje()
        ?.map { krav -> nyesteVurderingPerKrav[krav.referanse] }
        ?.komprimer()
        ?.filterNotNull()
        .orEmpty()
}