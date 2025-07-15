package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
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
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class FormkravSteg (
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val formkravRepository: FormkravRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trekkKlageService: TrekkKlageService,
    private val brevbestillingService: BrevbestillingService,
): BehandlingSteg {
    private val typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        // For trukkede klager hopper vi over steget
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        // Om vi ikke har lagret en vurdering eller definisjonen iokke er løst må vi be brukeren gjøre en første vurdering
        val grunnlag = formkravRepository.hentHvisEksisterer(kontekst.behandlingId)
        if (avklaringsbehov.harIkkeBlittLøst(Definisjon.VURDER_FORMKRAV) || grunnlag == null) {
            return FantAvklaringsbehov(Definisjon.VURDER_FORMKRAV)
        }

        // Hvis formkravet er oppfyllt eller fristen ikke er overholdt skal vi bare gå videre til neste steg
        if (grunnlag.vurdering.erOppfylt() || grunnlag.vurdering.erFristIkkeOverholdt()) {
            slettForhåndsvarselbrevSomIkkeErSendt(kontekst.behandlingId)
            return Fullført
        }

        // Hvis grunnlaget ikke er oppfyllt men fristen er overholdt så skal det sendes fårhåndsvarsel om det ikke allerede er sendt
        val brevbestilling = hentBrevbestilling(kontekst.behandlingId)
        if (brevbestilling == null) {
            val brevReferanse = bestillFårhåndsvarselBrev(kontekst.behandlingId)
            formkravRepository.lagreVarsel(kontekst.behandlingId, brevReferanse)
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
        }

        // Brevet av avbrutt, dette skjer om det er besillt et brev og så endrer man avklaringsbehovene så det ikke trengs
        // før det ble sendt. Vi må da bare gjenåpne brevet.
        if (brevbestilling.status == Status.AVBRUTT) {
            brevbestillingService.oppdaterStatus(kontekst.behandlingId, brevbestilling.referanse, Status.FORHÅNDSVISNING_KLAR)
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
        }

        // Er vi innenfor fristen på forhåndsvarselet og man formkravet fortsatt ikke er oppfyllt har bruker rett til
        // å rette opp i formkravene fram til fristen. Saken settes derfor på vent. Om man er etter fristen går vi videre
        // til neste steg|
        val venteBehov = avklaringsbehov.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)
        val varslingstidpunkt = grunnlag.varsel?.sendtDato ?: throw IllegalStateException(
            "Fant ikke varslingstidspunkt"
        )
        val frist = grunnlag.varsel.svarfrist ?: throw IllegalStateException(
            "Fant ikke frist"
        )

        if (grunnlag.vurdering.erIkkeOppfylt() && LocalDate.now() <= frist) {
            if (skalVentePåSvar(venteBehov, varslingstidpunkt)) {
                return FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                        frist = frist,
                    )
                )
            }
            return FantAvklaringsbehov(Definisjon.VURDER_FORMKRAV)
        }

        return Fullført
    }

    private fun hentBrevbestilling(behandlingId: BehandlingId): Brevbestilling? {
        return brevbestillingService.hentBestillinger(behandlingId, typeBrev)
            .maxByOrNull { it.opprettet }
    }

    private fun slettForhåndsvarselbrevSomIkkeErSendt(behandlingId: BehandlingId) {
        brevbestillingService.hentBestillinger(behandlingId, typeBrev)
            .filter { it.status == Status.FORHÅNDSVISNING_KLAR }
            .map { brevbestillingService.avbryt(behandlingId, it.referanse) }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringForhåndsvarsel = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)

        if (avklaringForhåndsvarsel != null && avklaringForhåndsvarsel.status().erÅpent()) {
            avklaringsbehovene.løsAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV, "", SYSTEMBRUKER.ident)
        }

    }


    private fun bestillFårhåndsvarselBrev(behandlingId: BehandlingId): BrevbestillingReferanse {
        val behandling = behandlingRepository.hent(behandlingId)
        val vårReferanse = "${behandling.referanse}-$typeBrev"

        val brevReferanse = brevbestillingService.bestillV2(
            behandlingId,
            typeBrev = typeBrev,
            unikReferanse = vårReferanse,
            ferdigstillAutomatisk = false
        )

        return BrevbestillingReferanse(brevReferanse)

    }

    private fun skalVentePåSvar(venteBehov: Avklaringsbehov?, varselDato: LocalDate): Boolean {
        if (venteBehov == null) {
            return true
        }

        val sisteVarselTattAvVent = venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > varselDato.atStartOfDay() }
        return !sisteVarselTattAvVent
    }
    
    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return FormkravSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                BrevbestillingService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.FORMKRAV
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == AVSLUTTET }
    }
}
