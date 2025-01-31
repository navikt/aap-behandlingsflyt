package no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Forhåndsvarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
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
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.VENTE_PÅ_FRIST_EFFEKTUER_11_7
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
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
import java.time.LocalDateTime

class Effektuer11_7Steg(
    private val underveisRepository: UnderveisRepository,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val effektuer117repository: Effektuer11_7Repository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(Effektuer11_7Steg::class.java)
    private val typeBrev = TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val bruddSomSkalSanksjoneres = bruddSomSkalSanksjoneres(kontekst, behandling)

        if (bruddSomSkalSanksjoneres.isEmpty()) {
            return Fullført
        }

        val eksisterendeBrevBestilling = brevbestillingService.hentBestillinger(kontekst.behandlingId, typeBrev)
            .maxByOrNull { it.opprettet }

        val effektuer117grunnlag = effektuer117repository.hentHvisEksisterer(behandling.id)
        if (eksisterendeBrevBestilling == null || eksisterendeVarselErIkkeDekkende(
                bruddSomSkalSanksjoneres,
                effektuer117grunnlag
            )
        ) {

            effektuer117repository.lagreVarsel(
                behandling.id,
                varsel = Effektuer11_7Forhåndsvarsel(
                    datoVarslet = LocalDate.now(),
                    underveisperioder = bruddSomSkalSanksjoneres.toList().map { it.verdi },
                ),
            )

            val unikReferanse = "${behandling.referanse}-$typeBrev-${effektuer117grunnlag?.varslinger?.size ?: 0}"

            // XXX: skulle gjerne "avbrutt" tidligere bestilling av brev, men det er ikke mulig i dag.
            brevbestillingService.bestill(
                kontekst.behandlingId,
                typeBrev,
                unikReferanse
            )

            return FantVentebehov(
                Ventebehov(
                    definisjon = BESTILL_BREV,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
                )
            )
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        if (avklaringsbehov.åpne().any { it.definisjon == SKRIV_BREV && it.skalStoppeHer(type()) }) {
            return FantAvklaringsbehov(SKRIV_BREV)
        }
        check(eksisterendeBrevBestilling.status == FULLFØRT) {
            "Brevet er ikke fullført, men brev-service har ikke opprettet SKRIV_BREV-behov"
        }

        val brev = brevbestillingService.hentBrevbestilling(eksisterendeBrevBestilling.referanse)
        val frist = brev.oppdatert.plusWeeks(3).toLocalDate()
        val venteBehov = avklaringsbehov.hentBehovForDefinisjon(VENTE_PÅ_FRIST_EFFEKTUER_11_7)

        if (skalVentePåSvar(venteBehov, brev.opprettet) && LocalDate.now(clock) <= frist) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = VENTE_PÅ_FRIST_EFFEKTUER_11_7,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    frist = frist,
                )
            )
        }

        val effektuer117avklaringsbehov = avklaringsbehov.hentBehovForDefinisjon(EFFEKTUER_11_7)

        if (effektuer117avklaringsbehov == null || effektuer117avklaringsbehov.erÅpent()) {
            return FantAvklaringsbehov(EFFEKTUER_11_7)
        }

        return Fullført
    }

    private fun skalVentePåSvar(venteBehov: Avklaringsbehov?, sisteVarsel: LocalDateTime): Boolean {
        if (venteBehov == null) {
            return true
        }

        val sisteVarselTattAvVent = venteBehov.historikk.any { it.status == AVSLUTTET && it.tidsstempel > sisteVarsel }
        return !sisteVarselTattAvVent
    }

    private fun eksisterendeVarselErIkkeDekkende(
        perioderSomSkalSanksjoneres: Tidslinje<Underveisperiode>,
        effektuer117grunnlag: Effektuer11_7Grunnlag?
    ): Boolean {
        val perioderAlleredeVarslet = effektuer117grunnlag
            ?.varslinger
            ?.lastOrNull()
            ?.underveisperioder
            .orEmpty()
            .map { Segment(it.periode, it) }
            .let { Tidslinje(it) }

        val dagerIkkeVarslet = perioderSomSkalSanksjoneres.kombiner(perioderAlleredeVarslet, StandardSammenslåere.minus())
        return dagerIkkeVarslet.isNotEmpty()
    }

    private fun bruddSomSkalSanksjoneres(
        kontekst: FlytKontekstMedPerioder,
        behandling: Behandling
    ): Tidslinje<Underveisperiode> {
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val relevanteBrudd =
            underveisGrunnlag.perioder.filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
                .let { underveisperioder -> Tidslinje(underveisperioder.map { Segment(it.periode, it) }) }

        val forrigeUnderveisGrunnlag = behandling.forrigeBehandlingId?.let { underveisRepository.hent(it) }
        val effektuerteBrudd = forrigeUnderveisGrunnlag?.perioder.orEmpty()
            .filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }
            .let { underveisperioder -> Tidslinje(underveisperioder.map { Segment(it.periode, it) }) }

        return relevanteBrudd.kombiner(effektuerteBrudd, StandardSammenslåere.minus())
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val brevbestillingRepository =
                repositoryProvider.provide<BrevbestillingRepository>()
            val effektuer117repository = repositoryProvider.provide<Effektuer11_7Repository>()

            val brevbestillingService =
                BrevbestillingService(
                    brevbestillingGateway = BrevGateway(),
                    brevbestillingRepository = brevbestillingRepository,
                    behandlingRepository = behandlingRepository,
                    sakRepository = sakRepository
                )

            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            val avklaringsbehovRepository =
                repositoryProvider.provide<AvklaringsbehovRepository>()
            return Effektuer11_7Steg(
                underveisRepository,
                brevbestillingService,
                behandlingRepository = behandlingRepository,
                avklaringsbehovRepository = avklaringsbehovRepository,
                effektuer117repository = effektuer117repository
            )
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_11_7
        }
    }
}