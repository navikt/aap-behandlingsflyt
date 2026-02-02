package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraKvalitetsikrer
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.compareTo
import kotlin.text.get

class KvalitetssikringsSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val trekkKlageService: TrekkKlageService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.KVALITETSSIKRING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, avklaringsbehovene) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(avklaringsbehovene) },
            tilbakestillGrunnlag = {},
            kontekst
        )

        if (avklaringsbehovene.skalTilbakeføresEtterKvalitetssikring()) {
            return TilbakeføresFraKvalitetsikrer
        }

        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder, avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(
                kontekst, type()
            ) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            return false
        }

        return when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Klage -> {
                avklaringsbehovene.harAvklaringsbehovSomKreverKvalitetssikring()
            }

            else -> false
        }
    }

    private fun erTilstrekkeligVurdert(avklaringsbehovene: Avklaringsbehovene): Boolean {
        if (avklaringsbehovene.alle()
                .filter { it.kreverKvalitetssikring() }
                .any { it.status() == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER || it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }
        ) {
            return false
        }

        val aktuelleAvklaringsbehovForKvalitetssikring = avklaringsbehovene.alle()
            .filter { it.kreverKvalitetssikring() }
            .filter { it.status() != Status.AVBRUTT }

        /**
         * Aldri tidligere blitt kvalitetssikret
         */
        if (!aktuelleAvklaringsbehovForKvalitetssikring.any { it.erKvalitetssikretTidligere() }) {
            return false
        }

        /**
         * Når kvalitetssikrer godkjenner, men beslutter underkjenner så skal steget sendes på nytt til kvalitetssikring
         */
        aktuelleAvklaringsbehovForKvalitetssikring.forEach { avklaringsbehov ->
            val sistReturnertFraBeslutter =
                avklaringsbehov.historikk.lastOrNull { historikk -> historikk.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
            val sistKvalitetssikret =
                avklaringsbehov.historikk.lastOrNull { historikk -> historikk.status == Status.KVALITETSSIKRET }

            if (sistReturnertFraBeslutter != null && sistKvalitetssikret != null) {
                return sistKvalitetssikret > sistReturnertFraBeslutter
            }
        }

        /**
         * Hvis stegets eget behov ("KVALITETSSIKRING") har status OPPRETTET, skal det alltid skje en ny kvalitetssikring
         */
        if (avklaringsbehovene.alle()
                .any { it.definisjon.kode == Definisjon.KVALITETSSIKRING.kode && it.status() == Status.OPPRETTET }
        ) {
            return false
        }

        /**
         * Dersom ett eller flere av avklaringsbehovene som krever kvalitetssikring blir åpnet opp på nytt etter at de er kvalitetssikret,
         * må kvalitetssikring gjøres på nytt. Dette må skje om behandlingen dras tilbake til før Sykdom og uten at kvalitetssikrer eller
         * beslutter faktisk har returnert. (Skjer ved ny startdato i 22-13)
         */
        if (avklaringsbehovene.alle()
            .filter { it.kreverKvalitetssikring() }
            .any { it.status() == Status.AVSLUTTET }
        ) {
            /**
             * Når beslutter returnerer et behov som ikke skal kvalitetssikres (22-13), blir alle behov reåpnet
             * og status KVALITETSSIKRET går tapt selv om det var kvalitetssikret tidligere.
             * Derfor må historikken sjekkes for å avgjøre om kvalitetssikring kan hoppes over.
             */
            val kvalitetssikretIForrigeRunde = avklaringsbehovene.alle()
                .filter { it.kreverKvalitetssikring() && it.status() == Status.AVSLUTTET }
                .any {
                    val aktivHistorikk = it.aktivHistorikk
                    val kvalitetssikretIForrigeRunde = it.aktivHistorikk.getOrNull(aktivHistorikk.size - 3)
                    kvalitetssikretIForrigeRunde?.status == Status.KVALITETSSIKRET
                }
            if (kvalitetssikretIForrigeRunde) {
                /* På dette tidspunktet kan 2 ting ha skjedd: */
                /* 1. Beslutter har returnert et behov som ikke krever kvalitetssikring (22-13) */
                /* 2. Flyten er dratt tilbake til 22-13 og ny startdao er satt */
                val sendtTilbakeFraBeslutter = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)
                    ?.aktivHistorikk
                    ?.let { history ->
                        history.size >= 2 &&
                                history[history.size - 2].status == Status.SENDT_TILBAKE_FRA_BESLUTTER
                    } ?: false
                if (sendtTilbakeFraBeslutter) {
                    return true
                }
            }
            return false
        }

        return true
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KvalitetssikringsSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }
    }
}
