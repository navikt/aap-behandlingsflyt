package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_9Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class VurderBrudd11_9LøserTest {
    private val aktivitetsplikt11_9Repository = mockk<Aktivitetsplikt11_9Repository>()

    @Test
    fun `Det nye grunnlaget skal inneholde iverksatte brudd pluss nye brudd`() {
        every { aktivitetsplikt11_9Repository.lagre(any(), any()) } returns Unit
        every { aktivitetsplikt11_9Repository.hentHvisEksisterer(BehandlingId(1)) } returns Aktivitetsplikt11_9Grunnlag(
            setOf(
                Aktivitetsplikt11_9Vurdering(
                    begrunnelse = "Begrunnet",
                    dato = 1 januar 2020,
                    grunn = Grunn.IKKE_RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                    vurdertAv = "123",
                    opprettet = ((10 januar 2020).atStartOfDay()).toInstant(ZoneOffset.UTC),
                    vurdertIBehandling = BehandlingId(1),
                )
            )
        )

        val løser = VurderBrudd11_9Løser(aktivitetsplikt11_9Repository)
        val behandlingId = BehandlingId(2)
        val kontekst = lagAvklaringsvehovKontekst(behandlingId, BehandlingId(1))

        løser.løs(
            kontekst, VurderBrudd11_9Løsning(
                aktivitetsplikt11_9Vurderinger = setOf(
                    Aktivitetsplikt11_9LøsningDto(
                        begrunnelse = "Var ikke allikevel",
                        dato = 1 januar 2020,
                        grunn = Grunn.RIMELIG_GRUNN,
                        brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                    ),
                    Aktivitetsplikt11_9LøsningDto(
                        begrunnelse = "Men det var det denne dagen",
                        dato = 2 januar 2020,
                        grunn = Grunn.IKKE_RIMELIG_GRUNN,
                        brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                    )
                )
            )
        )

        verify(exactly = 1) {
            aktivitetsplikt11_9Repository.lagre(BehandlingId(2), withArg {
                assertThat(it).hasSize(3)
                assertThat(it).containsExactlyInAnyOrder(
                    Aktivitetsplikt11_9Vurdering(
                        begrunnelse = "Begrunnet",
                        dato = 1 januar 2020,
                        grunn = Grunn.IKKE_RIMELIG_GRUNN,
                        brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                        vurdertAv = "123",
                        opprettet = ((10 januar 2020).atStartOfDay()).toInstant(ZoneOffset.UTC),
                        vurdertIBehandling = BehandlingId(1),
                    ),
                    Aktivitetsplikt11_9Vurdering(
                        begrunnelse = "Var ikke allikevel",
                        dato = 1 januar 2020,
                        grunn = Grunn.RIMELIG_GRUNN,
                        brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                        vurdertAv = "12345678901",
                        opprettet = it.find { it.begrunnelse == "Var ikke allikevel" }!!.opprettet,
                        vurdertIBehandling = BehandlingId(2),
                    ),
                    Aktivitetsplikt11_9Vurdering(
                        begrunnelse = "Men det var det denne dagen",
                        dato = 2 januar 2020,
                        grunn = Grunn.IKKE_RIMELIG_GRUNN,
                        brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                        vurdertAv = "12345678901",
                        opprettet = it.find { it.begrunnelse == "Men det var det denne dagen" }!!.opprettet,
                        vurdertIBehandling = BehandlingId(2),
                    )
                )
            })
        }
    }


    private fun lagAvklaringsvehovKontekst(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId
    ): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            bruker = Bruker("12345678901"),
            kontekst = FlytKontekst(
                sakId = SakId(1L),
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
                behandlingType = TypeBehandling.Aktivitetsplikt
            )
        )

}