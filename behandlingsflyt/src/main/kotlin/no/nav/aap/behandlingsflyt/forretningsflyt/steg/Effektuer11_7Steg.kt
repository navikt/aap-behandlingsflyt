package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BESTILL_BREV
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.EFFEKTUER_11_7
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.SKRIV_BREV
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class Effektuer11_7Steg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    private val typeBrev = TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return Fullført

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val relevanteBrudd = underveisGrunnlag.perioder.filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
            .let { Tidslinje(it.map { Segment(it.periode, it)} ) }

        val forrigeUnderveisGrunnlag = behandling.forrigeBehandlingId?.let { underveisRepository.hent(it) }
        val forrigeBrudd = forrigeUnderveisGrunnlag?.perioder.orEmpty().filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
            .let { Tidslinje(it.map { Segment(it.periode, it ) })}

        val nyeBrudd = relevanteBrudd.kombiner(forrigeBrudd, StandardSammenslåere.minus())

        if (nyeBrudd.isEmpty()) {
            return Fullført
        }

        val eksisterendeBrevBestilling = brevbestillingService.hentBestillingForSteg(kontekst.behandlingId, typeBrev)

        if (eksisterendeBrevBestilling == null) {
            brevbestillingService.bestill(kontekst.behandlingId, typeBrev, "${behandling.referanse}-$typeBrev")
        }

        if (eksisterendeBrevBestilling == null || eksisterendeBrevBestilling.status != Status.FULLFØRT) {
            return FantVentebehov(Ventebehov(
                definisjon = BESTILL_BREV,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
            ))
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val skrivBrevAvklaringsbehov = avklaringsbehov.alle().singleOrNull { it.definisjon == SKRIV_BREV }
        if (skrivBrevAvklaringsbehov != null) {
            throw IllegalStateException("Brudd aktivitetsplikt-brev skal være helautomatisk (foreløpig)")
        }



        val frist = LocalDate.of(2025, 12, 1) // TODO

        if (LocalDate.now() <= frist /* TODO: og ikke fått svar (SpesifikkVentebehovEvaluerer) */) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = EFFEKTUER_11_7,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    frist = frist,
                )
            )
        }

        val effektuer117avklaringsbehov =  avklaringsbehov.alle().singleOrNull { it.definisjon == EFFEKTUER_11_7 }

        if (effektuer117avklaringsbehov == null || effektuer117avklaringsbehov.erÅpent()) {
            return FantAvklaringsbehov(EFFEKTUER_11_7)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            val brevbestillingRepository = repositoryProvider.provide(BrevbestillingRepository::class)

            val brevbestillingService =
                BrevbestillingService(
                    brevbestillingGateway = BrevGateway(),
                    brevbestillingRepository = brevbestillingRepository,
                    behandlingRepository = behandlingRepository,
                    sakRepository = sakRepository
                )

            val underveisRepository = repositoryProvider.provide(UnderveisRepository::class)
            val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)
            return Effektuer11_7Steg(
                underveisRepository,
                brevbestillingService,
                behandlingRepository = behandlingRepository,
                avklaringsbehovRepository = avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_11_7
        }
    }
}