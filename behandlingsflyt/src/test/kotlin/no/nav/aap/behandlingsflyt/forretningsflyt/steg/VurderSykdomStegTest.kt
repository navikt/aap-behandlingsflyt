package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VurderSykdomStegTest {
    @Test
    fun `Sykdom skal vurderes når studentvurderinger går fra oppfylt til ikke-oppfylt`() {
        val sakId = SakId(1)
        val behandlingId = InMemoryBehandlingRepository.opprettBehandling(
            sakId,
            TypeBehandling.Førstegangsbehandling,
            null,
            mockk(relaxed = true)
        ).id
        val revurderingId = InMemoryBehandlingRepository.opprettBehandling(
            sakId,
            TypeBehandling.Revurdering,
            behandlingId,
            mockk(relaxed = true)
        ).id


        val sykdomRepository = mockk<SykdomRepository> {
            every { hentHvisEksisterer(any()) } returns null
        }
        val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        val avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider)
        val vilkårsresultatRepository = InMemoryVilkårsresultatRepository


        val studentRepository = mockk<StudentRepository> {
            every { hentHvisEksisterer(behandlingId) } returns StudentGrunnlag(
                setOf(
                    StudentVurdering(
                        begrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = true,
                        harBehovForBehandling = true,
                        avbruttStudieDato = 1 desember 2024,
                        avbruddMerEnn6Måneder = true,
                        vurdertIBehandling = behandlingId,
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                ),
                oppgittStudent = OppgittStudent(erStudentStatus = ErStudentStatus.AVBRUTT)
            )
            every { hentHvisEksisterer(revurderingId) } returns StudentGrunnlag(
                setOf(
                    StudentVurdering(
                        begrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = false,
                        harBehovForBehandling = true,
                        avbruttStudieDato = 1 desember 2024,
                        avbruddMerEnn6Måneder = true,
                        vurdertIBehandling = revurderingId,
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                ),
                oppgittStudent = OppgittStudent(erStudentStatus = ErStudentStatus.AVBRUTT)
            )
        }

        val steg = VurderSykdomSteg(
            studentRepository,
            sykdomRepository,
            avklaringsbehovRepository,
            FakeTidligereVurderinger(),
            avklaringsbehovService,
            InMemoryBehandlingRepository,
            vilkårsresultatRepository,
            unleashGateway = mockk {
                every { isDisabled(BehandlingsflytFeature.PeriodisertSykdom) } returns false
            },
        )

        lagreNedAlder(vilkårsresultatRepository, behandlingId)


        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Revurdering,
            vurderingType = VurderingType.REVURDERING,
            vurderingsbehovRelevanteForSteg = emptySet(),
            rettighetsperiode = Periode(1 januar 2025, 1 januar 2026)
        )

        steg.utfør(kontekst)

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
        assertThat(avklaringsbehov).isNull()

        steg.utfør(kontekst.copy(behandlingId = revurderingId, forrigeBehandlingId = behandlingId))

        val avklaringsbehovEtterRevurdering = avklaringsbehovRepository.hentAvklaringsbehovene(revurderingId)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
        assertThat(avklaringsbehovEtterRevurdering).isNotNull()

    }

    private fun lagreNedAlder(vilkårsresultatRepository: VilkårsresultatRepository, behandlingId: BehandlingId) {
        // Lagre ned aldervilkåret for å simulere at det var oppfylt i forrige behandling
        vilkårsresultatRepository.lagre(
            behandlingId,
            Vilkårsresultat(
                vilkår = listOf(
                    Vilkår(
                        Vilkårtype.ALDERSVILKÅRET, setOf(
                            Vilkårsperiode(
                                periode = Periode(Tid.MIN, (1 januar 2000).minusDays(1)),
                                utfall = Utfall.IKKE_OPPFYLT,
                                manuellVurdering = false,
                                avslagsårsak = Avslagsårsak.BRUKER_UNDER_18,
                                begrunnelse = null,
                                faktagrunnlag = null,
                            ),
                            Vilkårsperiode(
                                periode = Periode(
                                    1 januar 2000,
                                    (1 januar 2000).plusYears(67).minusYears(18).minusDays(1)
                                ),
                                utfall = Utfall.OPPFYLT,
                                manuellVurdering = false,
                                begrunnelse = null,
                                faktagrunnlag = null,
                            ),
                            Vilkårsperiode(
                                periode = Periode(
                                    (1 januar 2000).plusYears(67).minusYears(18),
                                    Tid.MAKS
                                ),
                                utfall = Utfall.IKKE_OPPFYLT,
                                avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                                manuellVurdering = false,
                                begrunnelse = null,
                                faktagrunnlag = null,
                            )
                        )
                    )
                )
            )
        )
    }
}