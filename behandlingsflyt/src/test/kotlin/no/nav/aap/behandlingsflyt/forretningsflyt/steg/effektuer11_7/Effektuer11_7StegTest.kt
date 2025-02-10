package no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryEffektuer117Repository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySakRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.EFFEKTUER_11_7
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AdjustableClock
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*

class Effektuer11_7StegTest {
    @Test
    fun `ny sak uten brudd er alltid fullført`() {
        val inMemoryUnderveisRepository = InMemoryUnderveisRepository()
        val steg = effektuer11_7steg(inMemoryUnderveisRepository)

        val sak = nySak()

        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        inMemoryUnderveisRepository.lagre(
            behandling.id,
            underveisperioder = listOf(
                underveisperiode(sak)
            ),
            input = tomUnderveisInput,
        )

        val kontekst = kontekst(sak, behandling.id, TypeBehandling.Førstegangsbehandling)
        val resultat = steg.utfør(kontekst)

        assertInstanceOf<Fullført>(resultat)
    }

    @Test
    fun `ny sak med ikke-relevante brudd er alltid fullført`() {
        val inMemoryUnderveisRepository = InMemoryUnderveisRepository()
        val steg = effektuer11_7steg(inMemoryUnderveisRepository)

        val sak = nySak()

        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        inMemoryUnderveisRepository.lagre(
            behandling.id,
            underveisperioder = listOf(
                underveisperiode(sak).copy(
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = UnderveisÅrsak.FRAVÆR_FASTSATT_AKTIVITET,
                )
            ),
            input = tomUnderveisInput,
        )

        val kontekst = kontekst(sak, behandling.id, TypeBehandling.Førstegangsbehandling)
        val resultat = steg.utfør(kontekst)

        assertInstanceOf<Fullført>(resultat)
    }

    @Test
    fun `vanlig flyt - Bestill brev, Vente på brev skal ferdigstilles, venter på svar fra bruker, frist utløper`() {
        val brevbestillingGateway = FakeBrevbestillingGateway()
        val clock = AdjustableClock(Instant.now())

        val inMemoryUnderveisRepository = InMemoryUnderveisRepository()
        val steg = Effektuer11_7Steg(
            underveisRepository = inMemoryUnderveisRepository,
            brevbestillingService = BrevbestillingService(
                brevbestillingGateway = brevbestillingGateway,
                brevbestillingRepository = InMemoryBrevbestillingRepository,
                behandlingRepository = InMemoryBehandlingRepository,
                sakRepository = InMemorySakRepository,
            ),
            behandlingRepository = InMemoryBehandlingRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            effektuer117repository = InMemoryEffektuer117Repository,
            clock = clock,
        )

        val sak = nySak()

        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        inMemoryUnderveisRepository.lagre(
            behandling.id,
            underveisperioder = listOf(
                underveisperiode(sak).copy(
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT,
                    bruddAktivitetspliktId = BruddAktivitetspliktId(0),
                )
            ),
            input = tomUnderveisInput,
        )

        val kontekst = kontekst(sak, behandling.id, TypeBehandling.Førstegangsbehandling)

        steg.utfør(kontekst).also {
            assertEquals(
                FantVentebehov(
                    Ventebehov(definisjon = Definisjon.BESTILL_BREV, grunn = VENTER_PÅ_MASKINELL_AVKLARING)
                ), it
            )
        }

        BrevbestillingReferanse(brevbestillingGateway.brevbestillingResponse!!.referanse).let { ref ->
            brevbestillingGateway.ferdigstill(ref)
            InMemoryBrevbestillingRepository.oppdaterStatus(behandling.id, ref, Status.FULLFØRT)
        }


        steg.utfør(kontekst).also {
            assertEquals(
                FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.VENTE_PÅ_FRIST_EFFEKTUER_11_7,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                        frist = LocalDate.now().plusWeeks(3),
                    )
                ), it
            )
        }

        //3 uker + en dag
        clock.gåFremITid(Duration.ofDays(22))
        steg.utfør(kontekst).also {
            assertEquals(FantAvklaringsbehov(EFFEKTUER_11_7), it)
        }
    }

    @Test
    fun `Åpent Avklaringsbehov finnes fra før`() {
        val brevbestillingGateway = FakeBrevbestillingGateway()
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val clock = AdjustableClock(Instant.now().plus(Duration.ofDays(22)))
        val kontekst = kontekst(sak, behandling.id, TypeBehandling.Førstegangsbehandling)

        val inMemoryUnderveisRepository = InMemoryUnderveisRepository()
        val steg = Effektuer11_7Steg(
            underveisRepository = inMemoryUnderveisRepository,
            brevbestillingService = BrevbestillingService(
                brevbestillingGateway = brevbestillingGateway,
                brevbestillingRepository = InMemoryBrevbestillingRepository,
                behandlingRepository = InMemoryBehandlingRepository,
                sakRepository = InMemorySakRepository,
            ),
            behandlingRepository = InMemoryBehandlingRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            effektuer117repository = InMemoryEffektuer117Repository,
            clock = clock,
        )

        steg.bestillBrev(
            behandling = behandling,
            sak = sak,
            kontekst = kontekst,
            brevbestillingGateway = brevbestillingGateway,
            inMemoryUnderveisRepository = inMemoryUnderveisRepository
        )

        InMemoryAvklaringsbehovRepository.opprett(
            behandlingId = behandling.id,
            definisjon = EFFEKTUER_11_7,
            funnetISteg = StegType.EFFEKTUER_11_7,
            begrunnelse = "",
            endretAv = "",
        )

        steg.utfør(kontekst).also {
            assertEquals(FantAvklaringsbehov(EFFEKTUER_11_7), it)
        }
    }

    @Test
    fun `brev er bestillt trenger manuell skriving av veileder`() {
        val inMemoryUnderveisRepository = InMemoryUnderveisRepository()
        val steg = effektuer11_7steg(inMemoryUnderveisRepository)

        val sak = nySak()

        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        inMemoryUnderveisRepository.lagre(
            behandling.id,
            underveisperioder = listOf(
                underveisperiode(sak).copy(
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT,
                    bruddAktivitetspliktId = BruddAktivitetspliktId(0),
                )
            ),
            input = tomUnderveisInput,
        )

        val kontekst = kontekst(sak, behandling.id, TypeBehandling.Førstegangsbehandling)

        // bestiller brev
        steg.utfør(kontekst)

        InMemoryAvklaringsbehovRepository.opprett(
            behandlingId = behandling.id,
            definisjon = Definisjon.SKRIV_BREV,
            funnetISteg = StegType.EFFEKTUER_11_7,
            begrunnelse = "",
            endretAv = "",
        )
        steg.utfør(kontekst).also {
            assertEquals(FantAvklaringsbehov(avklaringsbehov=listOf(Definisjon.SKRIV_BREV)), it)
        }
    }

    private fun opprettBehandling(sak: Sak, typeBehandling: TypeBehandling) =
        InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            årsaker = listOf(),
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
        )

    private fun nySak() = InMemorySakRepository.finnEllerOpprett(
        person = Person(
            id = 0,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    )

    private fun effektuer11_7steg(inMemoryUnderveisRepository: InMemoryUnderveisRepository) = Effektuer11_7Steg(
        underveisRepository = inMemoryUnderveisRepository,
        brevbestillingService = BrevbestillingService(
            brevbestillingGateway = FakeBrevbestillingGateway(),
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository = InMemoryBehandlingRepository,
            sakRepository = InMemorySakRepository,
        ),
        behandlingRepository = InMemoryBehandlingRepository,
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        effektuer117repository = InMemoryEffektuer117Repository,
    )

    private fun underveisperiode(sak: Sak) = Underveisperiode(
        periode = sak.rettighetsperiode,
        meldePeriode = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusDays(14)),
        utfall = Utfall.OPPFYLT,
        avslagsårsak = null,
        grenseverdi = Prosent.`100_PROSENT`,
        gradering = Gradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = Prosent.`100_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOf(),
        bruddAktivitetspliktId = null,
    )

    private fun kontekst(
        sak: Sak,
        behandlingId: BehandlingId,
        typeBehandling: TypeBehandling
    ): FlytKontekstMedPerioder {
        val vurderingType = when (typeBehandling) {
            TypeBehandling.Førstegangsbehandling -> VurderingType.FØRSTEGANGSBEHANDLING
            TypeBehandling.Revurdering -> VurderingType.REVURDERING
            TypeBehandling.Tilbakekreving -> VurderingType.REVURDERING //Skal nok være noe annet
            TypeBehandling.Klage -> VurderingType.REVURDERING
        }
        return FlytKontekstMedPerioder(
            sakId = sak.id,
            behandlingId = behandlingId,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            perioderTilVurdering = setOf(
                Vurdering(
                    type = vurderingType,
                    årsaker = listOf(),
                    periode = sak.rettighetsperiode,
                )
            ),
        )
    }

    private fun Effektuer11_7Steg.bestillBrev(
        behandling: Behandling,
        sak: Sak,
        kontekst: FlytKontekstMedPerioder,
        brevbestillingGateway: FakeBrevbestillingGateway,
        inMemoryUnderveisRepository: InMemoryUnderveisRepository
    ) {
        inMemoryUnderveisRepository.lagre(
            behandling.id,
            underveisperioder = listOf(
                underveisperiode(sak).copy(
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT,
                    bruddAktivitetspliktId = BruddAktivitetspliktId(0),
                )
            ),
            input = tomUnderveisInput,
        )

        this.utfør(kontekst)

        val referanse = BrevbestillingReferanse(brevbestillingGateway.brevbestillingResponse!!.referanse)
        brevbestillingGateway.ferdigstill(referanse)
        InMemoryBrevbestillingRepository.oppdaterStatus(behandling.id, referanse, Status.FULLFØRT)
    }
}