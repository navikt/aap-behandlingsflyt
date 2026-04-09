package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

class StansOpphørService(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val stansOpphørRepository: StansOpphørRepository
) {
    fun vedtattStansOpphør(behandlingId: BehandlingId): List<GjeldendeStansEllerOpphør> {
        val opphørGrunnlag = stansOpphørRepository.hentHvisEksisterer(behandlingId) ?: return emptyList()

        val sluttDato = hentSluttDato(behandlingId) ?: LocalDate.MAX

        val gjeldendeStansEllerOpphør = opphørGrunnlag.gjeldendeStansOgOpphør()

        return gjeldendeStansEllerOpphør.filter { it.fom.isBefore(sluttDato.plusDays(1)) }
    }

    private fun hentSluttDato(behandlingId: BehandlingId): LocalDate? {
        val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandlingId)!!
        val gjeldendeVarighet = vedtakslengdeGrunnlag.gjeldendeVurdering()
        val sluttDato = gjeldendeVarighet?.sluttdato
        return sluttDato
    }

}