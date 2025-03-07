package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
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

        val samordingsperioder = samordningRepository.hentHvisEksisterer(behandlingId)!!

        val samordningTidslinje =
            samordingsperioder.samordningPerioder.map { Segment(it.periode, it.gradering) }.let(::Tidslinje)

        val r = tilkjentTidslinje.kombiner(samordningTidslinje, JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
            if (høyre == null) {
                Segment(periode, Pair(venstre.verdi, Prosent.`0_PROSENT`))
            } else {
                Segment(periode, Pair(venstre.verdi, høyre.verdi))
            }
        })

        val underveisTidslinje =
            underveisRepository.hent(behandlingId).perioder.map { Segment(it.periode, it) }.let(::Tidslinje)


        val rx = Tidslinje.zip3(samordningTidslinje, tilkjentTidslinje, underveisTidslinje)

        require(rx.isNotEmpty())

        print(rx.segmenter().size)
        print(rx)
        return rx.filter {
            it.verdi.second != null && it.verdi.second!!.redusertDagsats().verdi.toDouble() > 0 && (it.verdi.first?.prosentverdi()
                ?: 0) < 100
        }
            .minDato()
    }
}