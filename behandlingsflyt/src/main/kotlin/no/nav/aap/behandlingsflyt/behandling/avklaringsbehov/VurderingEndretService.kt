package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.erFunksjoneltLik
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.erFunksjoneltLik
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
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        arbeidsevneRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide()
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
        Definisjon.ARBEIDSOPPTRAPPING to EndretSjekk(
            hentPåTidspunkt = arbeidsopptrappingRepository::hentArbeidsopptrappingVurderingPåTidspunkt,
            hentNåværende = { arbeidsopptrappingRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<ArbeidsopptrappingVurdering>::erFunksjoneltLik,
        ),
        Definisjon.AVKLAR_OVERGANG_UFORE to EndretSjekk(
            hentPåTidspunkt = overgangUføreRepository::hentOvergangUføreVurderingPåTidspunkt,
            hentNåværende = { overgangUføreRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<OvergangUføreVurdering>::erFunksjoneltLik
        ),
        Definisjon.ETABLERING_EGEN_VIRKSOMHET to EndretSjekk(
            hentPåTidspunkt = etableringEgenVirksomhetRepository::hentEtableringEgenVirksomhetVurderingPåTidspunkt,
            hentNåværende = { etableringEgenVirksomhetRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<EtableringEgenVirksomhetVurdering>::erFunksjoneltLik
        ),
        Definisjon.FASTSETT_ARBEIDSEVNE to EndretSjekk(
            hentPåTidspunkt = arbeidsevneRepository::hentArbeidsevneVurderingPåTidspunkt,
            hentNåværende = { arbeidsevneRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<ArbeidsevneVurdering>::erFunksjoneltLik
        ),
        Definisjon.AVKLAR_OVERGANG_ARBEID to EndretSjekk(
            hentPåTidspunkt = overgangArbeidRepository::hentOvergangArbeidVurderingPåTidspunkt,
            hentNåværende = { overgangArbeidRepository.hentHvisEksisterer(it)?.vurderinger },
            erLik = List<OvergangArbeidVurdering>::erFunksjoneltLik
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

internal class EndretSjekk<T>(
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

