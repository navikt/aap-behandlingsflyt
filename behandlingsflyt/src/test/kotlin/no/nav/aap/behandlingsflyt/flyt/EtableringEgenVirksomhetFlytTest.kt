package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepositoryImpl
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EtableringEgenVirksomhetFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `Skal kunne løse normalt og få oppfylte perioder`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val oppdatertBehandling = behandling
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsAvklaringsBehov(
                EtableringEgenVirksomhetLøsning(
                    listOf(
                        EtableringEgenVirksomhetLøsningDto(
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusDays(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            virksomhetErNy = true,
                            brukerEierVirksomheten = true,
                            kanFøreTilSelvforsørget = true,
                            utviklingsPerioder = listOf(
                                Periode(
                                    sak.rettighetsperiode.fom.plusDays(1),
                                    sak.rettighetsperiode.fom.plusDays(4)
                                ),
                                Periode(
                                    sak.rettighetsperiode.fom.plusDays(2),
                                    sak.rettighetsperiode.fom.plusDays(3)
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
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.ETABLERING_EGEN_VIRKSOMHET }
            }
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val etableringGrunnlag = dataSource.transaction {
            EtableringEgenVirksomhetRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assert(etableringGrunnlag?.vurderinger?.size == 1)
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Kan revurdere med nye perioder`() {
        // asserts på når kvoten trekkes
        TODO()
    }

    @Test
    fun `Ikke oppfylt om ikke 11-5 & 11-6b er oppfylt`() {
        // asserts på når kvoten trekkes
        TODO()
    }

    @Test
    fun `Skal ikke kunne overstige utviklingsperiodens kvote på 131 dager`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<IllegalArgumentException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
                .løsAvklaringsBehov(
                    EtableringEgenVirksomhetLøsning(
                        listOf(
                            EtableringEgenVirksomhetLøsningDto(
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom.plusDays(1),
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = true,
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
                )
        }
        assertThat(feil.message).contains("Oppsatte utviklingsdager overstiger gjenværende dager:")
    }

    @Test
    fun `Skal ikke kunne overstige oppstartsperiodens kvote på 66 dager`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<IllegalArgumentException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
                .løsAvklaringsBehov(
                    EtableringEgenVirksomhetLøsning(
                        listOf(
                            EtableringEgenVirksomhetLøsningDto(
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom.plusDays(1),
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = true,
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
                )
        }
        assertThat(feil.message).contains("Oppsatte oppstartsdager overstiger gjenværende dager:")
    }

    @Test
    fun `Skal ikke kunne legge oppstartsperioder før utviklingsperioden`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<IllegalArgumentException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
                .løsAvklaringsBehov(
                    EtableringEgenVirksomhetLøsning(
                        listOf(
                            EtableringEgenVirksomhetLøsningDto(
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom.plusDays(1),
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = true,
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
                )
        }
        assertThat(feil.message).contains("Oppstartsperioder kan ikke ligge før en utviklingsperiode")
    }

    @Test
    fun `Oppstart må være minst én dag etter første dag med innvilget AAP`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<IllegalArgumentException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
                .løsAvklaringsBehov(
                    EtableringEgenVirksomhetLøsning(
                        listOf(
                            EtableringEgenVirksomhetLøsningDto(
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom,
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = true,
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
                )
        }
        assertThat(feil.message).contains("vurderingenGjelderFra må være minst én dag etter første mulige dag med AAP")
    }

    @Test
    fun `Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<IllegalArgumentException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
                .løsAvklaringsBehov(
                    EtableringEgenVirksomhetLøsning(
                        listOf(
                            EtableringEgenVirksomhetLøsningDto(
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom.plusDays(1),
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = true,
                                kanFøreTilSelvforsørget = true,
                                utviklingsPerioder = listOf(),
                                oppstartsPerioder = listOf()
                            )
                        )
                    )
                )
        }
        assertThat(feil.message).contains("Må ha definert minst én periode i tidsplanen dersom vilkåret er oppfylt for en periode")
    }
}