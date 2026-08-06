package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.meldekort.PdfgenGateway
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnderveisRegel
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.dokumentasjon.VedtakDokumentGenerator
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytAktivitetspliktbehandlingService: AvbrytAktivitetspliktbehandlingService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val unleashGateway: UnleashGateway,
    private val pdfgenGateway: PdfgenGateway,
    private val vedtakDokumentGenerator: VedtakDokumentGenerator,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val vedtakBehøverVurdering = vedtakBehøverVurdering(kontekst, avklaringsbehovene)
        val erTilstrekkeligVurdert = erTilstrekkeligVurdert(kontekst, avklaringsbehovene)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.FATTE_VEDTAK,
            vedtakBehøverVurdering = { vedtakBehøverVurdering },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        val vedtakstidspunkt = if (vedtakBehøverVurdering)
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
                ?.historikk
                ?.filter { it.status == Status.AVSLUTTET }
                ?.maxOrNull()
                ?.tidsstempel
        else
            LocalDateTime.now(ZoneId.of("Europe/Oslo"))

        if (erTilstrekkeligVurdert && vedtakstidspunkt != null && skalLagreYtelsesvedtak(kontekst)) {
            vedtakService.lagreVedtak(
                behandlingId = kontekst.behandlingId,
                vedtakstidspunkt = vedtakstidspunkt,
                virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId),
            )
            if (unleashGateway.isEnabled(BehandlingsflytFeature.GenererVilkarsvurderingOppsummeringPDF)) {
                lokalUtskriftsmappe()?.let { utskriftsmappe ->
                    val dokument = vedtakDokumentGenerator.genererDokument(
                        behandlingId = kontekst.behandlingId,
                        sakId = kontekst.sakId,
                        vedtakstidspunkt = vedtakstidspunkt,
                        forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    )
                    val pdf = pdfgenGateway.genererGeneriskDokument(dokument)
                    skrivPdfLokalt(utskriftsmappe, kontekst, pdf)
                }
            }
        }

        return Fullført
    }

    private fun lokalUtskriftsmappe(): File? {
        val utskriftssti = System.getenv("AAP_VEDTAK_LOKAL_UTSKRIFT_STI")
            ?: System.getProperty("aap.vedtak.lokal.utskrift.sti")
            ?: return null
        return File(utskriftssti).also { it.mkdirs() }
    }

    private fun skrivPdfLokalt(utskriftsmappe: File, kontekst: FlytKontekstMedPerioder, pdf: ByteArray) {
        FileOutputStream(File(utskriftsmappe, "${kontekst.behandlingId.id}.pdf")).use { it.write(pdf) }
    }

    private fun skalLagreYtelsesvedtak(kontekst: FlytKontekstMedPerioder): Boolean {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling -> {
                return !trukketSøknadService.søknadErTrukket(kontekst.behandlingId)
            }

            TypeBehandling.Revurdering -> {
                return !avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)
            }

            TypeBehandling.Tilbakekreving,
            TypeBehandling.Klage,
            TypeBehandling.SvarFraAndreinstans,
            TypeBehandling.OppfølgingsBehandling,
            TypeBehandling.Aktivitetsplikt,
            TypeBehandling.Aktivitetsplikt11_9 -> {
                return false
            }
        }
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
            trekkKlageService.klageErTrukket(kontekst.behandlingId) ||
            avbrytAktivitetspliktbehandlingService.behandlingErAvbrutt(kontekst.behandlingId)
        ) {
            return false
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Opprettholdes) {
                return false
            }
        }

        return avklaringsbehovene.harAvklaringsbehovSomKreverToTrinn()
    }

    private fun erTilstrekkeligVurdert(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val erTrukketEllerIngenGrunnlag =
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
                    trekkKlageService.klageErTrukket(kontekst.behandlingId)

        return when {
            erTrukketEllerIngenGrunnlag -> true
            avklaringsbehovene.harAvklaringsbehovSomKreverToTrinnMenIkkeErGodkjent() -> false
            else -> true
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FatteVedtakSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                trekkKlageService = TrekkKlageService(repositoryProvider),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                avbrytAktivitetspliktbehandlingService = AvbrytAktivitetspliktbehandlingService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
                vedtakService = VedtakService(repositoryProvider, gatewayProvider),
                virkningstidspunktUtleder = VirkningstidspunktUtleder(repositoryProvider),
                unleashGateway = gatewayProvider.provide(),
                pdfgenGateway = gatewayProvider.provide(),
                vedtakDokumentGenerator = VedtakDokumentGenerator(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}