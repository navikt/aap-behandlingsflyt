package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
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
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class FormkravSteg (
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val formkravRepository: FormkravRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trekkKlageService: TrekkKlageService,
    private val brevbestillingService: BrevbestillingService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway
): BehandlingSteg {
    private val brevBehov = ForhåndsvarselKlageFormkrav

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val venteBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)
        val grunnlag = formkravRepository.hentHvisEksisterer(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.VURDER_FORMKRAV,
            vedtakBehøverVurdering = { vedtakBehøverVurderingFormkrav(kontekst) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdertFormkrav(kontekst, grunnlag, venteBehov)},
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV,
            vedtakBehøverVurdering = { vedtakBehøverVurderingForhåndsvarsel(kontekst, grunnlag?.vurdering)},
            erTilstrekkeligVurdert = { erTilstrekkeligVurdertForhåndsvarsel(kontekst) },
            tilbakestillGrunnlag = { avbrytBrevbestilling(kontekst.behandlingId)},
            kontekst = kontekst
        )

        val varsel = grunnlag?.varsel
        if(varsel != null && skalVentePåFristFraForhåndsvarsel(kontekst, varsel, grunnlag.vurdering, venteBehov )) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    frist = varsel?.svarfrist,
                )
            )
        }

        return Fullført

    }

    private fun skalVentePåFristFraForhåndsvarsel(
        kontekst: FlytKontekstMedPerioder,
        varsel: FormkravVarsel?,
        vurderingForBehandling: FormkravVurdering?,
        venteBehov: Avklaringsbehov?
    ): Boolean {
        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        return erForhåndsvarselSendt(brevbestilling)
                && venterPåVarselFrist(varsel, vurderingForBehandling)
                && harIkkeLøstVenteBehov(venteBehov, varsel)

    }

    private fun vedtakBehøverVurderingFormkrav(kontekst: FlytKontekstMedPerioder): Boolean {
        return !trekkKlageService.klageErTrukket(kontekst.behandlingId)
    }

    private fun erTilstrekkeligVurdertFormkrav(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: FormkravGrunnlag?,
        venteBehov: Avklaringsbehov?,
    ): Boolean {
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return true
        }
        if (grunnlag == null) {
            return false
        } else if (grunnlag.vurdering.erOppfylt()) {
            return true
        } else if (erTattAvVentFørFristenPåForhåndsvarsel( grunnlag, venteBehov)) {
            return false
        }
        return true
    }

    private fun erTattAvVentFørFristenPåForhåndsvarsel(grunnlag: FormkravGrunnlag, ventebehov: Avklaringsbehov?): Boolean {
       val varsel = grunnlag.varsel
        return varsel?.let {
            venterPåVarselFrist(varsel, grunnlag.vurdering) && !harIkkeLøstVenteBehov(ventebehov, varsel)
        } ?: false
    }

    private fun venterPåVarselFrist( varsel: FormkravVarsel?, vurdering: FormkravVurdering?): Boolean {
        val frist = varsel?.svarfrist ?: throw IllegalStateException(
        "Fant ikke frist"
        )
        val erFørEllerPåFrist = LocalDate.now() <= frist
        return  erFørEllerPåFrist && vurdering?.likevelBehandles != true
    }

    private fun harIkkeLøstVenteBehov(venteBehov: Avklaringsbehov?, varsel: FormkravVarsel?): Boolean {
        if (venteBehov == null) {
            return true
        }
        val varselDato = varsel?.sendtDato ?: throw IllegalStateException("Fant ikke varslingstidspunkt")

        val erSisteVarselTattAvVent =
            venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > varselDato.atStartOfDay() }
        return !erSisteVarselTattAvVent

    }

    private fun vedtakBehøverVurderingForhåndsvarsel(kontekst: FlytKontekstMedPerioder, vurdering: FormkravVurdering?): Boolean {
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return false
        }
       if(vurdering == null || vurdering.erOppfylt() || vurdering.erFristIkkeOverholdt()) {
           return false
       } else {
           bestillEllerGjenopprettFårhåndsvarselBrev(kontekst.behandlingId)
           return true
       }
    }

    private fun erTilstrekkeligVurdertForhåndsvarsel(
        kontekst: FlytKontekstMedPerioder
    ): Boolean {
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return true
        }
        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        return erForhåndsvarselSendt(brevbestilling)
    }

    private fun erForhåndsvarselSendt(brevbestilling: Brevbestilling?): Boolean =
        brevbestilling?.let { it.status in listOf(Status.SENDT, Status.FULLFØRT) } ?: false

    private fun hentBrevbestilling(behandlingId: BehandlingId): Brevbestilling? {
        return brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
            .maxByOrNull { it.opprettet }
    }


    private fun bestillEllerGjenopprettFårhåndsvarselBrev(behandlingId: BehandlingId) {
        val brevbestilling = hentBrevbestilling(behandlingId)
        if (brevbestilling == null) {
            val behandling = behandlingRepository.hent(behandlingId)
            val vårReferanse = "${behandling.referanse}-${brevBehov.typeBrev}"

            val brevReferanse = brevbestillingService.bestill(
                behandlingId,
                brevBehov = brevBehov,
                unikReferanse = vårReferanse,
                ferdigstillAutomatisk = false
            )
            formkravRepository.lagreVarsel(behandlingId, BrevbestillingReferanse(brevReferanse))
        } else if (brevbestilling.status == Status.AVBRUTT) {
            brevbestillingService.oppdaterStatus(behandlingId, brevbestilling.referanse, Status.FORHÅNDSVISNING_KLAR)
        }
    }

    private fun avbrytBrevbestilling(behandlingId: BehandlingId) {
        brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
            .filter { it.status == Status.FORHÅNDSVISNING_KLAR }
            .forEach { brevbestillingService.avbryt(behandlingId, it.referanse) }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return FormkravSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                BrevbestillingService(repositoryProvider, gatewayProvider),
                AvklaringsbehovService(repositoryProvider),
                gatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.FORMKRAV
        }
    }
}
