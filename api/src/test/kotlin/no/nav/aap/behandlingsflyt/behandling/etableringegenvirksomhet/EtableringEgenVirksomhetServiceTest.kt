package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

class EtableringEgenVirksomhetServiceTest {
    @Test
    fun `tom liste av vurderinger gyldig`() {
        val behandling = opprettBehandling(LocalDate.now())
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

    private fun opprettBehandling(periode: LocalDate): Behandling {
        val person = Person(
            PersonId(Random.nextLong()),
            UUID.randomUUID(),
            listOf(genererIdent(LocalDate.now().minusYears(23)))
        )
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        return InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
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
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseMinstHalvparten = ErNedsettelseMinstHalvpartenValg.JA,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    erNedsettelseMerEnnYrkesskadegrense = ErNedsettelseMerEnnYrkesskadegrenseValg.JA,
                    yrkesskadeBegrunnelse = null,
                    erArbeidsevnenNedsatt = true,
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