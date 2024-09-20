package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.foreldrepenger.ForeldrepengerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.sykepenger.SykepengerRepository
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningService(
    private val samordningRepository: SamordningRepository,
    private val foreldrepengerRepository: ForeldrepengerRepository,
    private val sykepengerRepository: SykepengerRepository,
) {

    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val fpGrunnlag = foreldrepengerRepository.hentHvisEksisterer(behandlingId)
        val spGrunnlag = sykepengerRepository.hentHvisEksisterer(behandlingId)

        // TODO: Implementer regler for overnevnte og produser tidslinje, finne ut om vi skal bruke SamordningGradering eller SamordningGraderingDto
        val vurderRegler = vurderRegler()

        samordningRepository.lagre(
            behandlingId,
            vurderRegler.segmenter()
                .map {
                    SamordningPeriode(
                        it.periode,
                        it.verdi.gradering
                    )
                })

        return vurderRegler
    }

    fun vurderRegler() : Tidslinje<SamordningGradering> {
        return Tidslinje(listOf())
    }
}