package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

class EtableringEgenVirksomhetLøserTest {

    @Test
    fun `Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode`() {
        val (sak, behandling) = opprettBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)


        val løser = EtableringEgenVirksomhetLøser(
            InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, EtableringEgenVirksomhetService(
                InMemoryEtableringEgenVirksomRepository,
                InMemoryBehandlingRepository,
                InMemoryBistandRepository,
                InMemorySykdomRepository
            )
        )
        val kontekst = AvklaringsbehovKontekst(
            Bruker("dd"),
            FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "meee",
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    tom = null,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = listOf(),
                    oppstartsPerioder = listOf()
                )
            )
        )


        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }
        assertThat(feil.message).contains("Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode")
    }

    @Test
    fun `Oppstart må være minst én dag etter første dag med innvilget AAP`() {
        val (sak, behandling) = opprettBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)


        val løser = EtableringEgenVirksomhetLøser(
            InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, EtableringEgenVirksomhetService(
                InMemoryEtableringEgenVirksomRepository,
                InMemoryBehandlingRepository,
                InMemoryBistandRepository,
                InMemorySykdomRepository
            )
        )
        val kontekst = AvklaringsbehovKontekst(
            Bruker("dd"),
            FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "meee",
                    fom = sak.rettighetsperiode.fom,
                    tom = null,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusDays(1),
                            sak.rettighetsperiode.fom.plusDays(4)
                        )
                    ),
                    oppstartsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(1),
                            sak.rettighetsperiode.fom.plusMonths(2)
                        )
                    )
                )
            )
        )
        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }

        assertThat(feil.message).contains("vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP")
    }

    @Test
    fun `Skal ikke kunne legge oppstartsperioder før utviklingsperioden`() {
        val (sak, behandling) = opprettBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)


        val løser = EtableringEgenVirksomhetLøser(
            InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, EtableringEgenVirksomhetService(
                InMemoryEtableringEgenVirksomRepository,
                InMemoryBehandlingRepository,
                InMemoryBistandRepository,
                InMemorySykdomRepository
            )
        )
        val kontekst = AvklaringsbehovKontekst(
            Bruker("dd"),
            FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "meee",
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    tom = null,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(1),
                            sak.rettighetsperiode.fom.plusMonths(2)
                        )
                    ),
                    oppstartsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusDays(1),
                            sak.rettighetsperiode.fom.plusDays(4)
                        )
                    )
                )
            )
        )
        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }

        assertThat(feil.message).contains("Oppstartsperioder kan ikke ligge før en utviklingsperiode")
    }

    @Test
    fun `Skal ikke kunne overstige oppstartsperiodens kvote på 66 dager`() {
        val (sak, behandling) = opprettBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)


        val løser = EtableringEgenVirksomhetLøser(
            InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, EtableringEgenVirksomhetService(
                InMemoryEtableringEgenVirksomRepository,
                InMemoryBehandlingRepository,
                InMemoryBistandRepository,
                InMemorySykdomRepository
            )
        )
        val kontekst = AvklaringsbehovKontekst(
            Bruker("dd"),
            FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "meee",
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    tom = null,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusDays(1),
                            sak.rettighetsperiode.fom.plusDays(4)
                        )
                    ),
                    oppstartsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(1),
                            sak.rettighetsperiode.fom.plusMonths(2)
                        ),
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(2),
                            sak.rettighetsperiode.fom.plusMonths(5)
                        )
                    )
                )
            )
        )
        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }

        assertThat(feil.message).contains("Oppsatte oppstartsdager overstiger gjenværende dager:")
    }

    @Test
    fun `Skal ikke kunne overstige utviklingsperiodens kvote på 131 dager`() {
        val (sak, behandling) = opprettBehandling(LocalDate.now())
        oppfyllSykdomOgBistand(behandling)


        val løser = EtableringEgenVirksomhetLøser(
            InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, EtableringEgenVirksomhetService(
                InMemoryEtableringEgenVirksomRepository,
                InMemoryBehandlingRepository,
                InMemoryBistandRepository,
                InMemorySykdomRepository
            )
        )
        val kontekst = AvklaringsbehovKontekst(
            Bruker("dd"),
            FlytKontekst(sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling())
        )

        val løsning = EtableringEgenVirksomhetLøsning(
            listOf(
                EtableringEgenVirksomhetLøsningDto(
                    begrunnelse = "meee",
                    fom = sak.rettighetsperiode.fom.plusDays(1),
                    tom = null,
                    virksomhetNavn = "peppas peppers",
                    orgNr = null,
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusDays(1),
                            sak.rettighetsperiode.fom.plusDays(4)
                        ),
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(1),
                            sak.rettighetsperiode.fom.plusMonths(10)
                        )
                    ),
                    oppstartsPerioder = listOf(
                        Periode(
                            sak.rettighetsperiode.fom.plusMonths(11),
                            sak.rettighetsperiode.fom.plusMonths(12)
                        )
                    )
                )
            )
        )
        val feil = assertThrows<UgyldigForespørselException> { løser.løs(kontekst, løsning) }

        assertThat(feil.message).contains("Oppsatte utviklingsdager overstiger gjenværende dager:")
    }


    private fun opprettBehandling(periode: LocalDate): Pair<Sak, Behandling> {
        val person =
            Person(
                PersonId(Random.nextLong()),
                UUID.randomUUID(),
                listOf(genererIdent(LocalDate.now().minusYears(23)))
            )
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )

        return sak to behandling
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