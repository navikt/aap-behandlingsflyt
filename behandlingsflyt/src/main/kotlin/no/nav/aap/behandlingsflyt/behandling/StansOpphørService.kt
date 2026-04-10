package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

class StansOpphørService(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val underveisRepository: UnderveisRepository,
    private val stansOpphørRepository: StansOpphørRepository
) {
    /**
     * Antas å kjøre etter at behandlingen er vedtatt (status IVERKSETTES eller status AVSLUTTET).
     */
    fun vedtattStansOpphør(behandlingId: BehandlingId): List<GjeldendeStansEllerOpphør> {
        val opphørGrunnlag =
            requireNotNull(stansOpphørRepository.hentHvisEksisterer(behandlingId)) { "Stans-opphør skal være lagret når denne metoden kjøres." }

        val sluttDato = hentSluttDato(behandlingId)

        val gjeldendeStansEllerOpphør = opphørGrunnlag.gjeldendeStansOgOpphør()

        /*
        Siden stans-datoen er første dag uten rett, for å få med vedtatte stans/opphør, inkluderer vi sluttdatoer innenfor
        én dag ekstra.
         */
        return gjeldendeStansEllerOpphør.filter { it.fom <= sluttDato.plusDays(1) }
    }

    private fun hentSluttDato(behandlingId: BehandlingId): LocalDate {
        val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandlingId)
        val gjeldendeVarighet = vedtakslengdeGrunnlag?.gjeldendeVurdering()
        val sluttDato = gjeldendeVarighet?.sluttdato

        val underveisMaksdato = underveisRepository.hentHvisEksisterer(behandlingId)
            ?.somTidslinje()
            ?.filter { it.verdi.utfall == Utfall.OPPFYLT }
            ?.helePerioden()?.tom

        return listOfNotNull(sluttDato, underveisMaksdato).max()
    }

}