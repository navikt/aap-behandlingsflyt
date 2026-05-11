package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetLøserTest {

    private val løser = EtableringEgenVirksomhetLøser(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        EtableringEgenVirksomhetService(
            InMemoryEtableringEgenVirksomRepository,
            InMemoryBehandlingRepository,
            InMemoryBistandRepository,
            InMemorySykdomRepository
        )
    )

    private fun opprettKontekst(sak: Sak, behandling: Behandling) = AvklaringsbehovKontekst(
        Bruker("dd"),
        FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
    )

    private fun oppfyltVurdering(
        fom: LocalDate,
        utviklingsPerioder: List<Periode> = emptyList(),
        oppstartsPerioder: List<Periode> = emptyList()
    ) = EtableringEgenVirksomhetLøsningDto(
        begrunnelse = "meee",
        fom = fom,
        tom = null,
        virksomhetNavn = "peppas peppers",
        orgNr = null,
        foreliggerFagligVurdering = true,
        virksomhetErNy = true,
        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
        kanFøreTilSelvforsørget = true,
        utviklingsPerioder = utviklingsPerioder,
        oppstartsPerioder = oppstartsPerioder
    )

    @Test
    fun `Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = opprettKontekst(sak, behandling)
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(oppfyltVurdering(fom = sak.rettighetsperiode.fom.plusDays(1)))
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode")
    }

    @Test
    fun `Oppstart må være minst én dag etter første dag med innvilget AAP`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = opprettKontekst(sak, behandling)
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom,
                    utviklingsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusDays(1), sak.rettighetsperiode.fom.plusDays(4))
                    ),
                    oppstartsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusMonths(1), sak.rettighetsperiode.fom.plusMonths(2))
                    )
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP")
    }

    @Test
    fun `Skal ikke kunne legge oppstartsperioder før utviklingsperioden`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = opprettKontekst(sak, behandling)
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    utviklingsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusMonths(1), sak.rettighetsperiode.fom.plusMonths(2))
                    ),
                    oppstartsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusDays(1), sak.rettighetsperiode.fom.plusDays(4))
                    )
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Oppstartsperioder kan ikke ligge før en utviklingsperiode")
    }

    @Test
    fun `Skal ikke kunne overstige oppstartsperiodens kvote på 66 dager`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = opprettKontekst(sak, behandling)
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    utviklingsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusDays(1), sak.rettighetsperiode.fom.plusDays(4))
                    ),
                    oppstartsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusMonths(1), sak.rettighetsperiode.fom.plusMonths(2)),
                        Periode(sak.rettighetsperiode.fom.plusMonths(2), sak.rettighetsperiode.fom.plusMonths(5))
                    )
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Oppsatte oppstartsdager overstiger gjenværende dager:")
    }

    @Test
    fun `Skal ikke kunne overstige utviklingsperiodens kvote på 131 dager`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)

        val kontekst = opprettKontekst(sak, behandling)
        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                oppfyltVurdering(
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    utviklingsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusDays(1), sak.rettighetsperiode.fom.plusDays(4)),
                        Periode(sak.rettighetsperiode.fom.plusMonths(1), sak.rettighetsperiode.fom.plusMonths(10))
                    ),
                    oppstartsPerioder = listOf(
                        Periode(sak.rettighetsperiode.fom.plusMonths(11), sak.rettighetsperiode.fom.plusMonths(12))
                    )
                )
            )
        )

        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Oppsatte utviklingsdager overstiger gjenværende dager:")
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
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
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