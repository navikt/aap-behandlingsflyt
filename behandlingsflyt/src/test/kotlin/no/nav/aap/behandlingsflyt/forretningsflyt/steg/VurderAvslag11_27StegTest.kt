package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvslag11_27Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class VurderAvslag11_27StegTest {

    private val gatewayProvider = createGatewayProvider { register<AlleAvskruddUnleash>() }

    private fun steg() = VurderAvslag11_27Steg(
        samordningService = SamordningService(
            samordningVurderingRepository = InMemorySamordningVurderingRepository,
            samordningYtelseRepository = InMemorySamordningYtelseRepository),
        uføreRepository = InMemoryUføreRepository,
        samordningUføreRepository = InMemorySamordningUføreRepository,
        avslag11_27repository = InMemoryAvslag11_27Repository,
        kravRepository = InMemoryKravRepository,
        avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, gatewayProvider),
        tidligereVurderinger = FakeTidligereVurderinger(),
        vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
    )

    @BeforeEach
    fun reset() {
        InMemoryAvslag11_27Repository.reset()
    }

    private fun lagKravOgVurdering(
        behandlingId: BehandlingId,
        skalAvslås: Boolean,
    ): Kravreferanse {
        val ref = Kravreferanse(UUID.randomUUID())
        InMemoryKravRepository.lagre(
            behandlingId, setOf(
                NyttKrav(
                    referanse = ref,
                    journalpostId = JournalpostId("jp-$ref"),
                    vurdertAv = Bruker("testBruker"),
                    begrunnelse = "",
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                    søknadsdato = Søknadsdato(1 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    overstyrMuligRettFra = null,
                    muligRettFra = 1 januar 2026,
                )
            )
        )
        InMemoryAvslag11_27Repository.lagre(
            behandlingId, listOf(
                Avslag11_27Vurdering(
                    referanse = ref,
                    begrunnelse = "begrunnelse",
                    harAnnenFullYtelse = skalAvslås,
                    brukersYtelse = if (skalAvslås) Ytelse.SYKEPENGER else null,
                    harSykepengegrunnlagOver2G = null,
                    skalAvslås1127 = skalAvslås,
                    vurdertIBehandling = behandlingId,
                    vurdertTidspunkt = Instant.now(),
                    vurdertAv = Bruker("testBruker"),
                )
            )
        )
        return ref
    }

    @Test
    fun `ingen grunnlag - vilkårsresultat settes ikke`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder(behandling)

        steg().utfør(kontekst)

        assertThat(InMemoryVilkårsresultatRepository.hent(behandling.id))
    }

    @Test
    fun `avslag satt til true - vilkår AVSLAG_11_27 er IKKE_OPPFYLT`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder(behandling)

        lagKravOgVurdering(behandling.id, skalAvslås = true)

        steg().utfør(kontekst)

        val vilkår = InMemoryVilkårsresultatRepository.hent(behandling.id)
            .finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkår.segmenter()).hasSize(1)
        assertThat(vilkår.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `avslag satt til false - vilkår AVSLAG_11_27 er OPPFYLT`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder(behandling)
        lagKravOgVurdering(behandling.id, skalAvslås = false)

        steg().utfør(kontekst)

        val vilkår = InMemoryVilkårsresultatRepository.hent(behandling.id)
            .finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkår.segmenter()).hasSize(1)
        assertThat(vilkår.segmenter().first().verdi.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    fun `nyeste vurdering per krav brukes - siste endring vinner`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder(behandling)
        val ref = Kravreferanse(UUID.randomUUID())

        InMemoryKravRepository.lagre(
            behandling.id, setOf(
                NyttKrav(
                    referanse = ref,
                    journalpostId = JournalpostId("jp"),
                    vurdertAv = Bruker("test"),
                    begrunnelse = "",
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    søknadsdato = Søknadsdato(1 januar 2026, SøknadsdatoÅrsak.SøknadMottatt),
                    overstyrMuligRettFra = null,
                    muligRettFra = 1 januar 2026,
                )
            )
        )
        InMemoryAvslag11_27Repository.lagre(
            behandling.id, listOf(
                Avslag11_27Vurdering(
                    referanse = ref,
                    begrunnelse = "gammel",
                    harAnnenFullYtelse = true,
                    brukersYtelse = Ytelse.SYKEPENGER,
                    harSykepengegrunnlagOver2G = null,
                    skalAvslås1127 = true,
                    vurdertIBehandling = behandling.id,
                    vurdertTidspunkt = Instant.now().minusSeconds(100),
                    vurdertAv = Bruker("test"),
                ),
                Avslag11_27Vurdering(
                    referanse = ref,
                    begrunnelse = "ny",
                    harAnnenFullYtelse = false,
                    brukersYtelse = null,
                    harSykepengegrunnlagOver2G = null,
                    skalAvslås1127 = false,
                    vurdertIBehandling = behandling.id,
                    vurdertTidspunkt = Instant.now(),
                    vurdertAv = Bruker("test"),
                ),
            )
        )

        steg().utfør(kontekst)

        val vilkår = InMemoryVilkårsresultatRepository.hent(behandling.id)
            .finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkår.segmenter().first().verdi.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    private fun flytKontekstMedPerioder(behandling: Behandling): FlytKontekstMedPerioder =
        flytKontekstMedPerioder {
            this.behandling = behandling
        }
}