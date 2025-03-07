package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.time.LocalDate

class VirkningstidspunktUtleder(
    private val underveisRepository: UnderveisRepository,
    private val samordningRepository: SamordningRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {

    fun utledVirkningsTidspunkt(behandlingId: BehandlingId): LocalDate {
        val tilkjentTidslinje = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)!!
            .map { Segment(it.periode, it.tilkjent) }
            .let(::Tidslinje)

        val samordningTidslinje =
            samordningRepository.hentHvisEksisterer(behandlingId)!!.samordningPerioder.map {
                Segment(
                    it.periode,
                    it.gradering
                )
            }.let(::Tidslinje)


        val underveisTidslinje =
            underveisRepository.hent(behandlingId).perioder.map { Segment(it.periode, it) }.let(::Tidslinje)


        val kombinertTidslinje = Tidslinje.zip3(samordningTidslinje, tilkjentTidslinje, underveisTidslinje)

        require(kombinertTidslinje.isNotEmpty())

        return kombinertTidslinje.filter {
            it.verdi.second != null && it.verdi.second!!.redusertDagsats().verdi.toDouble() > 0 && (it.verdi.first?.prosentverdi()
                ?: 0) < 100
        }
            .minDato()
    }
}