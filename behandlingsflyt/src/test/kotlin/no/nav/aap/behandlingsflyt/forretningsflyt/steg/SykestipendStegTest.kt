package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService.Behov
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class SykestipendStegTest {
    @Test
    fun `Skal være frivillig å vurdere sykestipend dersom man har potensielt rett på AAP i en periode`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()

        val standardFørstegangsbehandlingKontekst = FlytKontekstMedPerioder(
            sakId = sak.id,
            behandlingId = behandling.id,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(1 januar 2024, Tid.MAKS),
            vurderingsbehovRelevanteForStegMedPerioder = emptySet()
        )

        val steg = SykestipendSteg(
            inMemoryRepositoryProvider,
            minimalGatewayProvider(),
            FakeTidligereVurderinger().apply { avslagEllerIngenBehandlingsgrunnlag = false })

        val behov = steg.utledBehov(
            standardFørstegangsbehandlingKontekst,
        )

        assertThat(behov).isEqualTo(Behov.FRIVILLIG)
    }

    @Test
    fun `Skal ikke kunne vurdere sykestipend ved ingen periode med rett`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()

        val kontekst = FlytKontekstMedPerioder(
            sakId = sak.id,
            behandlingId = behandling.id,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(1 januar 2024, Tid.MAKS),
            vurderingsbehovRelevanteForStegMedPerioder = emptySet()
        )

        val steg = SykestipendSteg(
            inMemoryRepositoryProvider,
            minimalGatewayProvider(),
            FakeTidligereVurderinger().apply { avslagEllerIngenBehandlingsgrunnlag = true })


        val behov = steg.utledBehov(
            kontekst,
        )

        assertThat(behov).isEqualTo(Behov.INGEN_BEHOV)
    }

    @Test
    fun `Skal være påkrevd å vurdere sykestipend ved oppfylt studentperiode`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()

        val kontekst = FlytKontekstMedPerioder(
            sakId = sak.id,
            behandlingId = behandling.id,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(1 januar 2024, Tid.MAKS),
            vurderingsbehovRelevanteForStegMedPerioder = emptySet()
        )

        InMemoryStudentRepository.lagre(
            behandling.id,
            vurderinger = setOf(
                StudentVurdering(
                    fom = 1 januar 2024,
                    tom = 1 februar 2024,
                    begrunnelse = "...",
                    harAvbruttStudie = true,
                    godkjentStudieAvLånekassen = true,
                    avbruttPgaSykdomEllerSkade = true,
                    harBehovForBehandling = true,
                    avbruttStudieDato = 1 desember 2023,
                    avbruddMerEnn6Måneder = true,
                    vurdertAv = Bruker("Saksbehandler"),
                    vurdertIBehandling = BehandlingId(1),
                )
            )
        )
        
        val steg = SykestipendSteg(
            inMemoryRepositoryProvider,
            minimalGatewayProvider(),
            FakeTidligereVurderinger().apply { avslagEllerIngenBehandlingsgrunnlag = false }
        )

        assertThat(steg.utledBehov(kontekst)).isEqualTo(Behov.PÅKREVD)
    }
}