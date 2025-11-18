package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.samordning.EndringStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

data class UførePeriodeMedEndringStatus(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Prosent,
    val kilde: String = "PESYS",
    val endringStatus: EndringStatus,
)

fun Collection<Uføre>.medStatus(status: EndringStatus): List<UførePeriodeMedEndringStatus> {
    return this.map {
        UførePeriodeMedEndringStatus(
            virkningstidspunkt = it.virkningstidspunkt,
            uføregrad = it.uføregrad,
            kilde = it.kilde,
            endringStatus = status
        )
    }
}

class UførePeriodeSammenligner(private val uføreRepository: UføreRepository) {
    fun hentUføreGrunnlagMedEndretStatus(behandlingId: BehandlingId): List<UførePeriodeMedEndringStatus> {

        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)
        val eldsteUføreGrunnlag = uføreRepository.hentEldsteGrunnlag(behandlingId)

        return sammenlignGjeldendeMedTidligereUføreGrunnlag(uføreGrunnlag, eldsteUføreGrunnlag)
    }

    private fun sammenlignGjeldendeMedTidligereUføreGrunnlag(
        gjeldendeUføreGrunnlag: UføreGrunnlag?,
        eldsteUføreGrunnlag: UføreGrunnlag?
    ): List<UførePeriodeMedEndringStatus> {
        if (gjeldendeUføreGrunnlag == null && eldsteUføreGrunnlag == null) return emptyList()

        // Når gjeldende og tidligere er samme grunnlag så er alt nytt
        if (gjeldendeUføreGrunnlag != null && eldsteUføreGrunnlag == gjeldendeUføreGrunnlag) {
            return gjeldendeUføreGrunnlag.vurderinger.medStatus(EndringStatus.NY)
        }

        val gjeldendeVurderinger = gjeldendeUføreGrunnlag?.vurderinger.orEmpty()
        val eldsteVurderinger = eldsteUføreGrunnlag?.vurderinger.orEmpty()

        val slettede = eldsteVurderinger.filterNot { it in gjeldendeVurderinger }.medStatus(EndringStatus.SLETTET)
        val nye = gjeldendeVurderinger.filterNot { it in eldsteVurderinger }.medStatus(EndringStatus.NY)
        val uendrede = gjeldendeVurderinger.filter { it in eldsteVurderinger }.medStatus(EndringStatus.UENDRET)

        return (nye + uendrede + slettede).sortedBy { it.virkningstidspunkt }
    }

}
