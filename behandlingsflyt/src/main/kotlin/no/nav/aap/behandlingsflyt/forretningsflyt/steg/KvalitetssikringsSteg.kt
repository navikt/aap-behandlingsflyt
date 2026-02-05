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
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikringsSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val trekkKlageService: TrekkKlageService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
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
         * Dersom flyten blir dratt tilbake til et steg før kvalitetssikring, og det allerede er gjort en kvalitetssikring,
         * så skal dette potensielt trigge en ny kvalitetssikring. Dette kan skje selv om kvalitetssikrer og beslutter ikke har returnert,
         * men f. eks. ved at nytt starttidspunkt i 22-13 blir satt. Dette igjen vil løfte avklaringsbehovene under Sykdom.
         */
            val avsluttedeBehov = avklaringsbehovene.alle()
                .filter { it.kreverKvalitetssikring() && it.status() == Status.AVSLUTTET }

            if (avsluttedeBehov.isNotEmpty()) {
                /**
                 * Når flyten blir dratt tilbake eller beslutter returnerer et behov som ikke skal kvalitetssikres (22-13),
                 * blir alle behov reåpnet og gjeldende status "KVALITETSSIKRET" går tapt, selv om kvalitetssikring har skjedd.
                 * Derfor må historikken sjekkes for å avgjøre om det er skjedd en tidligere kvalitetssikring.
                 */
                val erKvalitetssikretFørRetur = avsluttedeBehov
                    .any {
                        val aktivHistorikk = it.aktivHistorikk
                        /**
                         * De tre siste statusene i historikken skal være følgende etter reåpning:
                         * "KVALITETSSIKRET"
                         * "OPPRETTET"
                         * "AVSLUTTET" (gjeldende status)
                         */
                        val endring = it.aktivHistorikk.getOrNull(aktivHistorikk.size - 3)
                        endring?.status == Status.KVALITETSSIKRET
                    }
                if (erKvalitetssikretFørRetur) {
                    /**
                     * På dette tidspunktet kan to ting ha skjedd:
                     * 1. Beslutter har kun returnert behov som ikke krever kvalitetssikring (f. eks. 22-13)
                     * 2. Flyten er dratt tilbake til 22-13 og nytt starttidspunkt er satt
                     */
                    val behovSomIkkeKreverKvalitetssikring = avklaringsbehovene.alle()
                        .filter { !it.kreverKvalitetssikring() }

                    val sendtTilbakeFraBeslutter =
                        behovSomIkkeKreverKvalitetssikring.any { behov ->
                            /**
                             * Dersom beslutter har returnert vil de to siste statusene i historikken være følgende etter reåpning:
                             * "SENDT_TILBAKE_FRA_BESLUTTER"
                             * "AVSLUTTET" (gjeldende status)
                             */
                            val endringer = behov.aktivHistorikk
                            endringer.size >= 2 && endringer[endringer.size - 2].status == Status.SENDT_TILBAKE_FRA_BESLUTTER
                        }

                    if (sendtTilbakeFraBeslutter) {
                        return true
                    }
                    return false
                }
            }

        return true
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KvalitetssikringsSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }
    }
}
