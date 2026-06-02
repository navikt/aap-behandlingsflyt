package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetServiceTest {
    @Test
    fun `tom liste av vurderinger gyldig`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )

        val res = service.erVurderingerGyldig(behandling.id, emptyList())

        assertThat(res).isInstanceOf(VirksomhetEtableringGyldig::class.java)
    }

    @Test
    fun `utleder ikke vurderbare perioder`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(LocalDate.of(2026, 1, 1))

        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = LocalDate.of(2026, 1, 1),
                    vurderingenGjelderTil = LocalDate.of(2026, 1, 6),
                    dokumenterBruktIVurdering = emptyList(),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    diagnose = null,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks")
                )
            )
        )

        InMemoryBistandRepository.lagre(
            behandling.id, listOf(
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = "saks",
                    vurderingenGjelderFra = LocalDate.of(2026, 1, 1),
                    tom = LocalDate.of(2026, 1, 3),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                ),
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = "saks",
                    vurderingenGjelderFra = LocalDate.of(2026, 1, 4),
                    tom = LocalDate.of(2026, 1, 6),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )

        val service = EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )

        val ikkeVurderbarePerioder = service.utledIkkeVurderbarePerioder(behandling.id)

        assertThat(ikkeVurderbarePerioder).containsExactly(
            Periode(LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 6)),
            Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
        )
    }

    private fun oppfyllSykdomOgBistand(behandling: Behandling) {
        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = LocalDate.now(),
                    vurderingenGjelderTil = LocalDate.now().plusMonths(6),
                    dokumenterBruktIVurdering = emptyList(),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    diagnose = null,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks")
                )
            )
        )
        InMemoryBistandRepository.lagre(
            behandling.id, listOf(
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = "saks",
                    vurderingenGjelderFra = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )
    }

}