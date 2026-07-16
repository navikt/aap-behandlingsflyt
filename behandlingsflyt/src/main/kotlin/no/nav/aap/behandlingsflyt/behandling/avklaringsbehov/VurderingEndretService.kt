package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class VurderingEndretService(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide()
    )

    private val sjekker: Map<Definisjon, EndretSjekk<*>> = mapOf(
        // TODO: tilsvarende for alle avklaringsbehov som skal kvalitetssikres
        Definisjon.AVKLAR_SYKDOM to EndretSjekk(
            hentPåTidspunkt = sykdomRepository::hentSykdomsvurderingerPåTidspunkt,
            hentNåværende = { sykdomRepository.hentHvisEksisterer(it)?.sykdomsvurderinger },
            erLik = List<Sykdomsvurdering>::erFunksjoneltLik,
            beskrivelse = "sykdomsvurdering"
        ),
        Definisjon.AVKLAR_BISTANDSBEHOV to EndretSjekk(
            hentPåTidspunkt = bistandRepository::hentBistandsvurderingPåTidspunkt,
            hentNåværende = { bistandRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<Bistandsvurdering>::erFunksjoneltLik,
            beskrivelse = "bistandsvurdering"
        ),
        Definisjon.SKRIV_SYKDOMSVURDERING_BREV to EndretSjekk(
            hentPåTidspunkt = sykdomsvurderingForBrevRepository::hentAktivPåTidspunkt,
            hentNåværende = sykdomsvurderingForBrevRepository::hent,
            erLik = SykdomsvurderingForBrev::erFunksjoneltLik,
            beskrivelse = "sykdomsvurderingForBrev"
        )
    )

    fun endretSidenTidspunkt(
        behandlingId: BehandlingId,
        avklaringsbehov: Avklaringsbehov,
        tidspunkt: LocalDateTime
    ): Boolean? {
        val sjekk = sjekker[avklaringsbehov.definisjon] ?: return null
        return sjekk.harEndring(behandlingId, tidspunkt)
    }
}

private class EndretSjekk<T>(
    val hentPåTidspunkt: (BehandlingId, LocalDateTime) -> T?,
    val hentNåværende: (BehandlingId) -> T?,
    val erLik: (T, T) -> Boolean,
    val beskrivelse: String
)

private fun <T> EndretSjekk<T>.harEndring(
    behandlingId: BehandlingId,
    tidspunkt: LocalDateTime
): Boolean {
    val aktivPåTidspunkt = requireNotNull(hentPåTidspunkt(behandlingId, tidspunkt)) {
        "Fant ingen $beskrivelse på tidspunkt $tidspunkt for behandling $behandlingId"
    }
    val aktivVurderingNå = requireNotNull(hentNåværende(behandlingId)) {
        "Fant ingen aktiv $beskrivelse for behandling $behandlingId"
    }
    return !erLik(aktivVurderingNå, aktivPåTidspunkt)
}

