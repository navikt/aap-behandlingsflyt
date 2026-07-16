package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.erFunksjoneltLik
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
    private val bistandRepository: BistandRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val refusjonkravRepository: RefusjonkravRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        refusjonkravRepository = repositoryProvider.provide(),
    )

    private val sjekker: Map<Definisjon, EndretSjekk<*>> = mapOf(
        // TODO: tilsvarende for alle avklaringsbehov som skal kvalitetssikres
        Definisjon.AVKLAR_SYKDOM to EndretSjekk(
            hentPåTidspunkt = sykdomRepository::hentSykdomsvurderingerPåTidspunkt,
            hentNåværende = { sykdomRepository.hentHvisEksisterer(it)?.sykdomsvurderinger },
            erLik = List<Sykdomsvurdering>::erFunksjoneltLik,
        ),
        Definisjon.AVKLAR_BISTANDSBEHOV to EndretSjekk(
            hentPåTidspunkt = bistandRepository::hentBistandsvurderingPåTidspunkt,
            hentNåværende = { bistandRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<Bistandsvurdering>::erFunksjoneltLik,
        ),
        Definisjon.SKRIV_SYKDOMSVURDERING_BREV to EndretSjekk(
            hentPåTidspunkt = sykdomsvurderingForBrevRepository::hentSykdomsvurderingForBrevPåTidspunkt,
            hentNåværende = sykdomsvurderingForBrevRepository::hent,
            erLik = SykdomsvurderingForBrev::erFunksjoneltLik,
        ),
        Definisjon.FRITAK_MELDEPLIKT to EndretSjekk(
            hentPåTidspunkt = meldepliktRepository::hentFritaksvurderingPåTidspunkt,
            hentNåværende = { meldepliktRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<Fritaksvurdering>::erFunksjoneltLik,
        ),
        Definisjon.REFUSJON_KRAV to EndretSjekk(
            hentPåTidspunkt = refusjonkravRepository::hentRefusjonkravPåTidspunkt,
            hentNåværende = refusjonkravRepository::hentHvisEksisterer,
            erLik = List<RefusjonkravVurdering>::erFunksjoneltLik,
        ),
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
) {
    fun harEndring(behandlingId: BehandlingId, tidspunkt: LocalDateTime): Boolean {
        val aktivPåTidspunkt = hentPåTidspunkt(behandlingId, tidspunkt)
        val aktivVurderingNå = hentNåværende(behandlingId)

        if (aktivVurderingNå == null && aktivPåTidspunkt == null) {
            // vurdering fantes verken sist eller nå -> ingen endring
            return false
        }

        if (aktivVurderingNå == null || aktivPåTidspunkt == null) {
            // vurdering fantes før men ikke nå, eller motsatt -> endring
            return true
        }

        return !erLik(aktivVurderingNå, aktivPåTidspunkt)
    }
}

