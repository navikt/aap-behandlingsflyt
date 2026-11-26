package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_9Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class VurderBrudd11_9LøserTest {
    private val aktivitetsplikt11_9Repository = InMemoryAktivitetsplikt11_9Repository
    private val sakRepository = InMemorySakRepository

    @Test
    fun `Det nye grunnlaget skal inneholde iverksatte brudd pluss nye brudd`() {
        val (_, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(søknadsdato = 1 januar 2019)

        InMemoryAktivitetsplikt11_9Repository.lagre(
            førstegangsbehandling.id, setOf(
                Aktivitetsplikt11_9Vurdering(
                    begrunnelse = "Begrunnet",
                    dato = 1 januar 2020,
                    grunn = Grunn.IKKE_RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                    vurdertAv = Bruker("123"),
                    opprettet = ((10 januar 2020).atStartOfDay()).toInstant(ZoneOffset.UTC),
                    vurdertIBehandling = førstegangsbehandling.id,
                )
            )
        )

        val løser = VurderBrudd11_9Løser(aktivitetsplikt11_9Repository, sakRepository)
        val kontekst = avklaringsbehovKontekst { this.behandling = revurdering }

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

        val uthentet = aktivitetsplikt11_9Repository.hentHvisEksisterer(revurdering.id)

        assertThat(uthentet!!.vurderinger).hasSize(3)
        assertThat(uthentet.vurderinger).containsExactlyInAnyOrder(
            Aktivitetsplikt11_9Vurdering(
                begrunnelse = "Begrunnet",
                dato = 1 januar 2020,
                grunn = Grunn.IKKE_RIMELIG_GRUNN,
                brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                vurdertAv = Bruker("123"),
                opprettet = ((10 januar 2020).atStartOfDay()).toInstant(ZoneOffset.UTC),
                vurdertIBehandling = førstegangsbehandling.id,
            ),
            Aktivitetsplikt11_9Vurdering(
                begrunnelse = "Var ikke allikevel",
                dato = 1 januar 2020,
                grunn = Grunn.RIMELIG_GRUNN,
                brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                vurdertAv = Bruker("SAKSBEHANDLER"),
                opprettet = uthentet.vurderinger.find { it.begrunnelse == "Var ikke allikevel" }!!.opprettet,
                vurdertIBehandling = revurdering.id
            ),
            Aktivitetsplikt11_9Vurdering(
                begrunnelse = "Men det var det denne dagen",
                dato = 2 januar 2020,
                grunn = Grunn.IKKE_RIMELIG_GRUNN,
                brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                vurdertAv = Bruker("SAKSBEHANDLER"),
                opprettet = uthentet.vurderinger.find { it.begrunnelse == "Men det var det denne dagen" }!!.opprettet,
                vurdertIBehandling = revurdering.id
            )
        )
    }
}