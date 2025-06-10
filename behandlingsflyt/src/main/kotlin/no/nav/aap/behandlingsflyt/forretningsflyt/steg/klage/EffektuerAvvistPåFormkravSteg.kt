package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Ufullstendig
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.ÅrsakTilUfullstendigResultat
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
import java.time.Clock
import java.time.LocalDate

class EffektuerAvvistPåFormkravSteg private constructor(
    private val formkravRepository: FormkravRepository,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val effektuerAvvistPåFormkravRepository: EffektuerAvvistPåFormkravRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val clock: Clock = Clock.systemDefaultZone()
) : BehandlingSteg {
    private val typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV


    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return EffektuerAvvistPåFormkravSteg(
                repositoryProvider.provide(),
                BrevbestillingService(repositoryProvider),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                KlageresultatUtleder(repositoryProvider),
                clock = Clock.systemDefaultZone(),
            )
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_AVVIST_PÅ_FORMKRAV
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val formkravVurdering = formkravRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
        requireNotNull(formkravVurdering)

        if (formkravVurdering.erOppfylt() || !formkravVurdering.erFristOverholdt()) {
            return Fullført
        }

        val eksisterendeBrevBestilling =
            brevbestillingService.hentBestillinger(kontekst.behandlingId, typeBrev)
                .maxByOrNull { it.opprettet }

        if (eksisterendeBrevBestilling == null) {
            val behandling = behandlingRepository.hent(kontekst.behandlingId)

            val vårReferanse = "${behandling.referanse}-$typeBrev" // TODO: Støtte flere varsler per behandling?

            val brevReferanse = brevbestillingService.bestillV2(
                kontekst.behandlingId,
                typeBrev = typeBrev,
                unikReferanse = vårReferanse,
                ferdigstillAutomatisk = false
            )

            // Frist og datovarslet utledes fra brevbestillingen
            effektuerAvvistPåFormkravRepository.lagreVarsel(
                kontekst.behandlingId,
                BrevbestillingReferanse(brevReferanse)

            )
            return FantAvklaringsbehov(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
        }

        check(eksisterendeBrevBestilling.status == FULLFØRT) {
            "Brevet er ikke fullført, men brev-service har ikke opprettet SKRIV_BREV-behov"
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val venteBehov = avklaringsbehov.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)

        val effektuerGrunnlag = effektuerAvvistPåFormkravRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?: throw IllegalStateException("Fant ikke grunnlag for effektuer avvist på formkrav for behandling ${kontekst.behandlingId}")
        val varslingstidpunkt = effektuerGrunnlag.varsel.datoVarslet ?: throw IllegalStateException(
            "Fant ikke varslingstidspunkt"
        )
        val frist = effektuerGrunnlag.varsel.frist ?: throw IllegalStateException(
            "Fant ikke frist"
        )

        if (skalVentePåSvar(venteBehov, varslingstidpunkt) && LocalDate.now(clock) <= frist) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    frist = frist,
                )
            )
        }

        if (!avklaringsbehov.erVurdertTidligereIBehandlingen(Definisjon.EFFEKTUER_AVVIST_PÅ_FORMKRAV)
            || måVurderesPåNytt(kontekst.behandlingId, klageresultatUtleder)
        ) {
            return FantAvklaringsbehov(Definisjon.EFFEKTUER_AVVIST_PÅ_FORMKRAV)
        }

        return Fullført
    }


    private fun skalVentePåSvar(venteBehov: Avklaringsbehov?, varselDato: LocalDate): Boolean {
        if (venteBehov == null) {
            return true
        }

        val sisteVarselTattAvVent =
            venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > varselDato.atStartOfDay() }
        return !sisteVarselTattAvVent
    }

    private fun måVurderesPåNytt(behandlingId: BehandlingId, klageresultatUtleder: KlageresultatUtleder): Boolean {
        val klageResultat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
        return when (klageResultat) {
            is Ufullstendig -> klageResultat.årsak == ÅrsakTilUfullstendigResultat.INKONSISTENT_FORMKRAV_VURDERING
            else -> false
        }
    }

}