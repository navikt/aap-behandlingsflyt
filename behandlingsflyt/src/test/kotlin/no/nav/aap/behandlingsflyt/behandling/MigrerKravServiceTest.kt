package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class MigrerKravServiceTest {

    private val service = MigrerKravService(
        mottattDokumentRepository = InMemoryMottattDokumentRepository,
        kravRepository = InMemoryKravRepository,
    )

    @Test
    fun `oppdater 22-13 - ingen eksisterende kravgrunnlag noop`() {
        val (sakId, behandlingId) = opprettSakOgBehandling()

        val vurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere,
            startDato = 1 juni 2023,
        )

        service.oppdaterKravForOverstyrtMuligRett(sakId, behandlingId, vurdering)

        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandlingId)).isNull()
    }

    @Test
    fun `reverser 22-13 - ingen eksisterende kravgrunnlag noop`() {
        val (_, behandlingId) = opprettSakOgBehandling()

        val vurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.Nei,
            startDato = null,
        )

        service.reverserKravForOverstyrtMuligRett(behandlingId, vurdering)

        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandlingId)).isNull()
    }

    @Test
    fun `reverser - kravgrunnlag uten overstyrt krav returnerer stille`() {
        val (_, behandlingId) = opprettSakOgBehandling()

        val krav = lagNyttKrav(behandlingId, overstyrMuligRettFra = null)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))

        val vurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.Nei,
            startDato = null,
        )

        val antallFør = InMemoryKravRepository.hent(behandlingId).vurderinger.size
        service.reverserKravForOverstyrtMuligRett(behandlingId, vurdering)

        assertThat(InMemoryKravRepository.hent(behandlingId).vurderinger).hasSize(antallFør)
        assertThat(
            InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>()
                .single().overstyrMuligRettFra
        ).isNull()
    }


    @Test
    fun `oppdater og reverser - full round-trip setter og nullstiller overstyrMuligRettFra`() {
        val (sakId, behandlingId) = opprettSakOgBehandling()
        val krav = lagNyttKrav(behandlingId)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))

        val overstyrtDato = 1 juni 2023
        val oppdaterVurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere,
            startDato = overstyrtDato,
        )

        service.oppdaterKravForOverstyrtMuligRett(sakId, behandlingId, oppdaterVurdering)

        val etterOppdatering =
            InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>().single()
        assertThat(etterOppdatering.overstyrMuligRettFra).isNotNull
        assertThat(etterOppdatering.overstyrMuligRettFra!!.dato).isEqualTo(overstyrtDato)
        assertThat(etterOppdatering.overstyrMuligRettFra!!.årsak).isEqualTo(OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere)

        val reverserVurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.Nei,
            startDato = null,
        )

        service.reverserKravForOverstyrtMuligRett(behandlingId, reverserVurdering)

        val etterReversering =
            InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>().single()
        assertThat(etterReversering.overstyrMuligRettFra).isNull()
        assertThat(etterReversering.muligRettFra).isEqualTo(overstyrtDato)
    }

    @Test
    fun `oppdater to ganger med samme referanse og reverser - ingen duplikater, riktig krav fjernes`() {
        val (sakId, behandlingId) = opprettSakOgBehandling()
        val krav = lagNyttKrav(behandlingId)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))

        val oppdaterVurdering = lagRettighetsperiodeVurdering(
            harRett = RettighetsperiodeHarRett.HarRettMisvisendeOpplysninger,
            startDato = 1 juni 2023,
        )

        service.oppdaterKravForOverstyrtMuligRett(sakId, behandlingId, oppdaterVurdering)
        service.oppdaterKravForOverstyrtMuligRett(sakId, behandlingId, oppdaterVurdering)

        assertThat(InMemoryKravRepository.hent(behandlingId).vurderinger).hasSize(1)

        service.reverserKravForOverstyrtMuligRett(
            behandlingId,
            lagRettighetsperiodeVurdering(harRett = RettighetsperiodeHarRett.Nei, startDato = null)
        )

        val etterReversering =
            InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>().single()
        assertThat(etterReversering.overstyrMuligRettFra).isNull()
    }

    @Test
    fun `reverser isolerer til inneværende behandling, krav fra annen behandling bevares`() {
        val (sakId, behandlingIdA) = opprettSakOgBehandling()
        val (_, behandlingIdB) = opprettRevurdering(sakId)
        
        val vedtattOverstyrtKrav = lagNyttKrav(
            behandlingIdA,
            søknadsdato = 15 april 2024,
            overstyrMuligRettFra = OverstyrMuligRettFra(
                1 mars 2023,
                OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere
            )
        )
        InMemoryKravRepository.lagre(behandlingIdB, setOf(vedtattOverstyrtKrav))

        val rettighetsperiodeVurdering = lagRettighetsperiodeVurdering(harRett = RettighetsperiodeHarRett.Nei, startDato = null)
        service.reverserKravForOverstyrtMuligRett(
            behandlingIdB,
            rettighetsperiodeVurdering
        )
        
        val forventetReversertVurdering = RelevantKrav(
            referanse = vedtattOverstyrtKrav.referanse,
            journalpostId = vedtattOverstyrtKrav.journalpostId,
            vurdertAv = rettighetsperiodeVurdering.vurdertAv,
            vurdertIBehandling = behandlingIdB,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(
                ZoneOffset.UTC),
            søknadsdato =  vedtattOverstyrtKrav.søknadsdato,
            overstyrMuligRettFra = null,
            muligRettFra = vedtattOverstyrtKrav.søknadsdato.dato,
            begrunnelse = rettighetsperiodeVurdering.begrunnelse
        )
            
        val kravIBEtterReversering = InMemoryKravRepository.hent(behandlingIdB)
        assertThat(kravIBEtterReversering.vurderinger)
            .describedAs { "Skal bevare vedtatt krav" }
            .hasSize(2).containsExactlyInAnyOrder(
                vedtattOverstyrtKrav,
                forventetReversertVurdering
            )
        assertThat(kravIBEtterReversering.gjeldendeVurderinger())
            .describedAs { "Vedtatt krav er overstyrt" }
            .hasSize(1)

    }

    @Test
    fun `oppdater og reverser - reversert krav beholder muligRettFra fra overstyrt krav`() {
        val (sakId, behandlingId) = opprettSakOgBehandling()
        val overstyrtDato = 1 juni 2023
        val krav = lagNyttKrav(behandlingId, søknadsdato = 15 januar 2024, muligRettFra = 15 januar 2024)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))

        service.oppdaterKravForOverstyrtMuligRett(
            sakId, behandlingId,
            lagRettighetsperiodeVurdering(
                harRett = RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere,
                startDato = overstyrtDato,
            )
        )

        // etter oppdater: muligRettFra = overstyrtDato
        val etterOppdater = InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>().single()
        assertThat(etterOppdater.muligRettFra).isEqualTo(overstyrtDato)

        service.reverserKravForOverstyrtMuligRett(
            behandlingId,
            lagRettighetsperiodeVurdering(harRett = RettighetsperiodeHarRett.Nei, startDato = null)
        )

        // etter reverser: muligRettFra bevares fra det overstyrt kravet (ikke tilbakestilt til original søknadsdato)
        val reversert = InMemoryKravRepository.hent(behandlingId).vurderinger.filterIsInstance<RelevantKrav>().single()
        assertThat(reversert.overstyrMuligRettFra).isNull()
        assertThat(reversert.muligRettFra).isEqualTo(overstyrtDato)
    }

    // --- Hjelpefunksjoner ---

    private fun opprettSakOgBehandling(): Pair<SakId, BehandlingId> {
        val sak = opprettInMemorySak()
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )
        return sak.id to behandling.id
    }
    private fun opprettRevurdering(sakId: SakId): Pair<SakId, BehandlingId> {
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )
        return sakId to behandling.id
    }

    private fun lagNyttKrav(
        behandlingId: BehandlingId,
        søknadsdato: java.time.LocalDate = 15 januar 2024,
        muligRettFra: java.time.LocalDate = søknadsdato,
        overstyrMuligRettFra: OverstyrMuligRettFra? = null,
    ) = RelevantKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = JournalpostId("JP-${behandlingId.id}"),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(søknadsdato, SøknadsdatoÅrsak.SøknadMottatt),
        overstyrMuligRettFra = overstyrMuligRettFra,
        muligRettFra = muligRettFra,
    )

    private fun lagRettighetsperiodeVurdering(
        harRett: RettighetsperiodeHarRett,
        startDato: java.time.LocalDate?,
    ) = RettighetsperiodeVurdering(
        startDato = startDato,
        begrunnelse = "Testbegrunnelse",
        harRettUtoverSøknadsdato = harRett,
        vurdertAv = Bruker("Z999999"),
        vurdertDato = LocalDateTime.of(2024, 6, 1, 12, 0),
    )
}
