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

        val overlapperMedSosialRefusjon = sosialRefusjonskrav != null && sosialRefusjonskrav.harKrav
                && tilkjentYtelseHelePerioden.overlapper(tilPeriode(sosialRefusjonskrav.fom, sosialRefusjonskrav.tom))

        val overlapperMedTjenestepensjonRefusjon = tpRefusjonskrav != null && tpRefusjonskrav.harKrav
                && tilkjentYtelseHelePerioden.overlapper(tilPeriode(tpRefusjonskrav.fom, tpRefusjonskrav.tom))

        val (frist, fom, tom) = when {
            overlapperMedTjenestepensjonRefusjon -> Triple(42L, tpRefusjonskrav.fom!!, tpRefusjonskrav.tom ?: vedtakstidspunkt.toLocalDate().minusDays(1).coerceAtLeast(tpRefusjonskrav.fom))
            overlapperMedSosialRefusjon -> Triple(21L, sosialRefusjonskrav.fom!!, sosialRefusjonskrav.tom ?: vedtakstidspunkt.toLocalDate().minusDays(1).coerceAtLeast(sosialRefusjonskrav.fom))
            else -> Triple(null, null, null)
        }

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