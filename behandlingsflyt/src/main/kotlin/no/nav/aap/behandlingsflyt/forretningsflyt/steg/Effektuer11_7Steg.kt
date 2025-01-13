package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
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
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.FORHÅNDSVARSEL_AKTIVITETSPLIKT
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
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate

class Effektuer11_7Steg(
    private val underveisRepository: UnderveisRepository,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    internal var clock: Clock = Clock.systemDefaultZone(),
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(Effektuer11_7Steg::class.java)
    private val typeBrev = TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val relevanteBrudd =
            underveisGrunnlag.perioder.filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
                .let { Tidslinje(it.map { Segment(it.periode, it) }) }

        val forrigeUnderveisGrunnlag = behandling.forrigeBehandlingId?.let { underveisRepository.hent(it) }
        val forrigeBrudd = forrigeUnderveisGrunnlag?.perioder.orEmpty()
            .filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
            .let { Tidslinje(it.map { Segment(it.periode, it) }) }

        val nyeBrudd = relevanteBrudd.kombiner(forrigeBrudd, StandardSammenslåere.minus())

        if (nyeBrudd.isEmpty()) {
            return Fullført
        }

        val eksisterendeBrevBestilling = brevbestillingService.hentBestillingForSteg(kontekst.behandlingId, typeBrev)

        if (eksisterendeBrevBestilling == null || false /* TODO: eksisterende varsel er ikke dekkende */) {
            // XXX: skulle gjerne "avbrutt" tidligere bestilling av brev, men det er ikke mulig i dag.
            brevbestillingService.bestill(kontekst.behandlingId, typeBrev, "${behandling.referanse}-$typeBrev")
            return FantVentebehov(
                Ventebehov(
                    definisjon = BESTILL_BREV,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
                )
            )
        }


        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        // XXX: her ønsker vi egentlig en form for fall-back hvis brev ikke er automatisk?
        // TODO: blir "forlatte" SKRIV_BREV-avklarings-behov automatisk lukket?
        val skrivBrevAvklaringsbehov = avklaringsbehov.åpne().any { it.definisjon == SKRIV_BREV }
        if (skrivBrevAvklaringsbehov) {
            throw IllegalStateException("Brudd aktivitetsplikt-brev skal være helautomatisk (foreløpig)")
        }

        val brev = brevbestillingService.hentSisteBrevbestilling(behandling.id) ?: run {
            // TODO: Dette burde ikke kunne skje: prøv å strukturer koden slik at vi ikke ender opp her
            logger.error("Finner ikke brev selv om brev er bestillt")
            return Fullført
        }

        if (brev.status == no.nav.aap.brev.kontrakt.Status.FERDIGSTILT) {
            // `oppdatert` er det beste vi har tilgjengelig nå. Ideelt sett skulle vi nok brukt
            // `ekspedert` fra dokument-distribusjon, men det har vi ikke tilgjengelig i dag.
            val frist = brev.oppdatert.plusWeeks(3).toLocalDate()

            if (LocalDate.now(clock) <= frist /* TODO: og ikke fått svar (SpesifikkVentebehovEvaluerer) */) {
                return FantVentebehov(
                    Ventebehov(
                        definisjon = FORHÅNDSVARSEL_AKTIVITETSPLIKT,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                        frist = frist,
                    )
                )
            }
        } else {
            /* Brevet er bestilt, men ikke ikke sendt enda. Vi vet derfor ikke fristen, så sjekker igjen i morgen. */
            return FantVentebehov(
                Ventebehov(
                    definisjon = FORHÅNDSVARSEL_AKTIVITETSPLIKT,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING, /* Under antagelsen om at varselet er automatisk, så venter vi på at bestillingen blir utført maskinelt. */
                    frist = LocalDate.now(clock).plusDays(1),
                )
            )
        }

        val effektuer117avklaringsbehov = avklaringsbehov.alle().singleOrNull { it.definisjon == EFFEKTUER_11_7 }

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