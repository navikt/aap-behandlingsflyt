package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselBruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Varsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
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
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    private val brevBehov = ForhåndsvarselBruddAktivitetsplikt
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.Aktivitetsplikt11_7)) {
            throw IllegalStateException(
                "Steg ${StegType.VURDER_AKTIVITETSPLIKT_11_7} er deaktivert i unleash, kan ikke utføre steg."
            )
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val vurderingForBehandling = aktivitetsplikt11_7Repository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger?.firstOrNull { it.vurdertIBehandling == kontekst.behandlingId }
        val venteBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.VURDER_BRUDD_11_7,
            vedtakBehøverVurdering = { vedtakBehøver11_7Vurdering(kontekst) },
            erTilstrekkeligVurdert = { er11_7TilstrekkeligVurdert(kontekst, vurderingForBehandling, venteBehov) },
            tilbakestillGrunnlag = { },
            kontekst = kontekst
        )

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
            vedtakBehøverVurdering = { vedtakBehøverForhåndsvarselVurdering(vurderingForBehandling, kontekst) },
            erTilstrekkeligVurdert = { erForhåndsvarselTilstrekkeligVurdert(kontekst) },
            tilbakestillGrunnlag = { avbrytBrevbestilling(kontekst.behandlingId)},
            kontekst = kontekst
        )

        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        if (erForhåndsvarselSendt(brevbestilling)) {
            val varsel = aktivitetsplikt11_7Repository.hentVarselHvisEksisterer(kontekst.behandlingId)

            if (venteFristIkkepassert(varsel, vurderingForBehandling)) {
                if (erVentebehovÅpentOgFristIkkePassert(venteBehov, varsel)) {
                    return FantVentebehov(
                        Ventebehov(
                            definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                            frist = varsel?.svarfrist ?: error("Mangler svarfrist i varsel"),
                        )
                    )
                }
            }

        }

        return Fullført
    }

    private fun erForhåndsvarselTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Boolean {
        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        return erForhåndsvarselSendt(brevbestilling)

    }

    private fun erForhåndsvarselSendt(brevbestilling: Brevbestilling?): Boolean =
        brevbestilling?.let { it.status in listOf(Status.SENDT, Status.FULLFØRT) } ?: false

    private fun vedtakBehøver11_7Vurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return Vurderingsbehov.AKTIVITETSPLIKT_11_7 in kontekst.vurderingsbehovRelevanteForSteg
    }

    private fun vedtakBehøverForhåndsvarselVurdering(
        vurderingForBehandling: Aktivitetsplikt11_7Vurdering?,
        kontekst: FlytKontekstMedPerioder
    ): Boolean {
        if (vurderingForBehandling == null || vurderingForBehandling.erOppfylt) {
            return false
        } else {
            /**
             * Bestill forhåndsvarsel eller reaktiver forhåndsvarsel dersom det er deaktivert
             */
            val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
            if (brevbestilling == null) {
                val brevReferanse = bestillForhåndsvarselBrev(kontekst.behandlingId)
                aktivitetsplikt11_7Repository.lagreVarsel(kontekst.behandlingId, brevReferanse)
            } else if (brevbestilling.status == Status.AVBRUTT) {
                brevbestillingService.oppdaterStatus(
                    kontekst.behandlingId,
                    brevbestilling.referanse,
                    Status.FORHÅNDSVISNING_KLAR
                )
            }
            return true
        }
    }

    private fun er11_7TilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder, vurderingForBehandling: Aktivitetsplikt11_7Vurdering?, venteBehov: Avklaringsbehov?): Boolean {
        if(vurderingForBehandling == null) {
            return false
        } else if (vurderingForBehandling.erOppfylt) {
            return true
        } else if (venteBehov != null) {

            val varsel = aktivitetsplikt11_7Repository.hentVarselHvisEksisterer(kontekst.behandlingId)
            if (venteFristIkkepassert(varsel, vurderingForBehandling)) {
                if (!erVentebehovÅpentOgFristIkkePassert(venteBehov, varsel)) {
                    return false
                }
            }
        }
        return true

    }

    private fun venteFristIkkepassert(
        varsel: Aktivitetsplikt11_7Varsel?,
        vurderingForBehandling: Aktivitetsplikt11_7Vurdering?
    ): Boolean {
        val frist = varsel?.svarfrist ?: throw IllegalStateException("Fant ikke frist")
        return  LocalDate.now() <= frist && vurderingForBehandling?.skalIgnorereVarselFrist != true
    }


    private fun erVentebehovÅpentOgFristIkkePassert(venteBehov: Avklaringsbehov?, varsel: Aktivitetsplikt11_7Varsel?): Boolean {
        if (venteBehov == null) {
            return true
        }
        val varselDato = varsel?.sendtDato ?: throw IllegalStateException("Fant ikke varslingstidspunkt")

        val erSisteVarselTattAvVent =
            venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > varselDato.atStartOfDay() }
        return !erSisteVarselTattAvVent
    }

    private fun avbrytBrevbestilling(behandlingId: BehandlingId) {
        brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
            .filter { it.status == Status.FORHÅNDSVISNING_KLAR }
            .forEach { brevbestillingService.avbryt(behandlingId, it.referanse) }
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
        ): VurderAktivitetsplikt11_7Steg {
            return VurderAktivitetsplikt11_7Steg(
                unleashGateway = gatewayProvider.provide(),
                avklaringsbehovRepository = repositoryProvider.provide(),
                aktivitetsplikt11_7Repository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),

            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AKTIVITETSPLIKT_11_7
        }
    }
}