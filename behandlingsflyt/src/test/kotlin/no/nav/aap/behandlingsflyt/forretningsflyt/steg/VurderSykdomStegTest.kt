package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VurderSykdomStegTest {
    @Test
    fun `Sykdom skal vurderes når studentvurderinger går fra oppfylt til ikke-oppfylt`() {
        val behandlingId = BehandlingId(1)
        val revurderingId = BehandlingId(2)

        val behandlingRepository = mockk<BehandlingRepository>()
        val studentRepository = mockk<StudentRepository> {
            every { hentHvisEksisterer(behandlingId) } returns StudentGrunnlag(
                StudentVurdering(
                    begrunnelse = "begrunnelse",
                    vurdertAv = "saksbehandler",
                    harAvbruttStudie = true,
                    godkjentStudieAvLånekassen = true,
                    avbruttPgaSykdomEllerSkade = true,
                    harBehovForBehandling = true,
                    avbruttStudieDato = 1 desember 2024,
                    avbruddMerEnn6Måneder = true,
                ),
                oppgittStudent = OppgittStudent(erStudentStatus = ErStudentStatus.AVBRUTT)
            )
            every { hentHvisEksisterer(revurderingId) } returns StudentGrunnlag(
                StudentVurdering(
                    begrunnelse = "begrunnelse",
                    vurdertAv = "saksbehandler",
                    harAvbruttStudie = true,
                    godkjentStudieAvLånekassen = true,
                    avbruttPgaSykdomEllerSkade = false,
                    harBehovForBehandling = true,
                    avbruttStudieDato = 1 desember 2024,
                    avbruddMerEnn6Måneder = true,
                ),
                oppgittStudent = OppgittStudent(erStudentStatus = ErStudentStatus.AVBRUTT)
            )
        }

        val sykdomRepository = mockk<SykdomRepository> {
            every { hentHvisEksisterer(any()) } returns null
        }
        val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        val avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider)
        val vilkårsresultatRepository = InMemoryVilkårsresultatRepository

        val steg = VurderSykdomSteg(
            studentRepository,
            sykdomRepository,
            avklaringsbehovRepository,
            FakeTidligereVurderinger(),
            avklaringsbehovService,
            behandlingRepository,
            vilkårsresultatRepository,
        )

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

        steg.utfør(kontekst.copy(behandlingId = revurderingId))

        val avklaringsbehovEtterRevurdering = avklaringsbehovRepository.hentAvklaringsbehovene(revurderingId)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
        assertThat(avklaringsbehovEtterRevurdering).isNotNull()

    }
}