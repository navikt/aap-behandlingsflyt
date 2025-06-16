package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import java.time.LocalDate
import java.time.LocalDateTime

class AvventUtbetalingService(
    private val refusjonskravRepository: RefusjonkravRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
) {

     fun finnEventuellAvventUtbetaling(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime, tilkjentYtelseHelePerioden: Periode): TilkjentYtelseAvventDto? {
        val sosialRefusjonskrav = refusjonskravRepository.hentHvisEksisterer(behandlingId)
        val tpRefusjonskrav = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandlingId)

        val overlapperMedSosialRefusjon = sosialRefusjonskrav != null && sosialRefusjonskrav.any { refusjonsKrav ->
            refusjonsKrav.harKrav && tilkjentYtelseHelePerioden.overlapper(tilPeriode(refusjonsKrav.fom, refusjonsKrav.tom))
        } == true

        val overlapperMedTjenestepensjonRefusjon = tpRefusjonskrav != null && tpRefusjonskrav.harKrav
                && tilkjentYtelseHelePerioden.overlapper(tilPeriode(tpRefusjonskrav.fom, tpRefusjonskrav.tom))

         val perioder = sosialRefusjonskrav
             ?.filter { it.harKrav && it.fom != null }
             ?.mapNotNull { vurdering ->
                 vurdering.fom?.let { fom ->
                     val tom = vurdering.tom ?: vedtakstidspunkt.toLocalDate().minusDays(1).coerceAtLeast(fom)
                     fom to tom
                 }
             }

         val tripleList = perioder?.takeIf { it.isNotEmpty() }?.let {
             val minFom = it.minOf { p -> p.first }
             val maxTom = it.maxOf { p -> p.second }
             Triple(21L, minFom, maxTom)
         }

         val triple = when {
             overlapperMedTjenestepensjonRefusjon -> Triple(
                 42L,
                 tpRefusjonskrav.fom!!,
                 tpRefusjonskrav.tom ?: vedtakstidspunkt.toLocalDate().minusDays(1).coerceAtLeast(tpRefusjonskrav.fom)
             )
             overlapperMedSosialRefusjon && tripleList != null -> tripleList
             else -> null
         }

         val frist = triple?.first
         val fom = triple?.second
         val tom = triple?.third

        return if (frist != null) {
            TilkjentYtelseAvventDto(
                fom = fom!!,
                tom = tom!!,
                overføres = tom.plusDays(frist),
                årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
                feilregistrering = false
            )
        } else {
            null
        }
    }

    private fun tilPeriode(fom: LocalDate?, tom: LocalDate?) = Periode(fom ?: LocalDate.MIN, tom ?: LocalDate.MAX)

}