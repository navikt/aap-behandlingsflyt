package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselBruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class VurderAktivitetsplikt11_7Steg(
    private val unleashGateway: UnleashGateway,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val behandlingRepository: BehandlingRepository,
    private val brevbestillingService: BrevbestillingService,
) : BehandlingSteg {
    private val brevBehov = ForhåndsvarselBruddAktivitetsplikt
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.Aktivitetsplikt11_7)) {
            throw IllegalStateException(
                "Steg ${StegType.VURDER_AKTIVITETSPLIKT_11_7} er deaktivert i unleash, kan ikke utføre steg."
            )
        }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(kontekst.behandlingId)
        if (!avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.VURDER_BRUDD_11_7) || grunnlag == null) {
            return FantAvklaringsbehov(Definisjon.VURDER_BRUDD_11_7)
        }

        // Hvis aktivitetsplikten er oppfyllt skal vi bare gå videre til neste steg
        if (grunnlag.vurdering.erOppfylt) {
            slettForhåndsvarselbrevSomIkkeErSendt(kontekst.behandlingId)
            return Fullført
        }

        // Hvis grunnlaget ikke er oppfyllt så skal det sendes forhåndsvarsel om det ikke allerede er sendt
        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        if (brevbestilling == null) {
            val brevReferanse = bestillForhåndsvarselBrev(kontekst.behandlingId)
            aktivitetsplikt11_7Repository.lagreVarsel(kontekst.behandlingId, brevReferanse)
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
        }

        // Vi har bestilt et brev, men det er ikke sendt. Dette kan oppstå om man lagrer grunnlaget på nytt og det fortsatt
        // ikke er gyldig, eller man har endret i et tidligere steg
        if (brevbestilling.status == Status.FORHÅNDSVISNING_KLAR) {
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
        }

        // Brevet av avbrutt, dette skjer om det er besillt et brev og så endrer man avklaringsbehovene så det ikke trengs
        // før det ble sendt. Vi må da bare gjenåpne brevet.
        if (brevbestilling.status == Status.AVBRUTT) {
            brevbestillingService.oppdaterStatus(
                kontekst.behandlingId,
                brevbestilling.referanse,
                Status.FORHÅNDSVISNING_KLAR
            )
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
        }

        // Er vi innenfor fristen på forhåndsvarselet og aktivitetsplikten ikke er oppfylt har bruker rett til
        // å uttale seg fram til fristen. Saken settes derfor på vent. Om man er etter fristen går vi videre til neste steg
        val venteBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
        val varslingstidpunkt = grunnlag.varsel?.sendtDato ?: throw IllegalStateException("Fant ikke varslingstidspunkt")
        val frist = grunnlag.varsel.svarfrist ?: throw IllegalStateException("Fant ikke frist")

        if (!grunnlag.vurdering.erOppfylt && LocalDate.now() <= frist && grunnlag.varsel.overstyrtVarsel == null) {
            if (skalVentePåSvar(venteBehov, varslingstidpunkt)) {
                return FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                        frist = frist,
                    )
                )
            }
            return FantAvklaringsbehov(Definisjon.VURDER_BRUDD_11_7)
        }


        return Fullført
    }

    private fun skalVentePåSvar(venteBehov: Avklaringsbehov?, varselDato: LocalDate): Boolean {
        if (venteBehov == null) {
            return true
        }

        val erSisteVarselTattAvVent =
            venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > varselDato.atStartOfDay() }
        return !erSisteVarselTattAvVent
    }

    private fun slettForhåndsvarselbrevSomIkkeErSendt(behandlingId: BehandlingId) {
        brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
            .filter { it.status == Status.FORHÅNDSVISNING_KLAR }
            .map { brevbestillingService.avbryt(behandlingId, it.referanse) }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringForhåndsvarsel =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)

        if (avklaringForhåndsvarsel != null && avklaringForhåndsvarsel.status().erÅpent()) {
            avklaringsbehovene.løsAvklaringsbehov(
                Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
                "",
                SYSTEMBRUKER.ident
            )
        }
    }

    private fun hentBrevbestilling(behandlingId: BehandlingId): Brevbestilling? {
        return brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
            .maxByOrNull { it.opprettet }
    }

    private fun bestillForhåndsvarselBrev(behandlingId: BehandlingId): BrevbestillingReferanse {
        val behandling = behandlingRepository.hent(behandlingId)
        val vårReferanse = "${behandling.referanse}-${brevBehov.typeBrev}"

        val brevReferanse = brevbestillingService.bestillV2(
            behandlingId,
            brevBehov = brevBehov,
            unikReferanse = vårReferanse,
            ferdigstillAutomatisk = false
        )

        return BrevbestillingReferanse(brevReferanse)

    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAktivitetsplikt11_7Steg(
                unleashGateway = gatewayProvider.provide(),
                avklaringsbehovRepository = repositoryProvider.provide(),
                aktivitetsplikt11_7Repository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider)

            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AKTIVITETSPLIKT_11_7
        }
    }
}