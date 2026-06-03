package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class EtableringEgenVirksomhetFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `Skal kunne løse normalt og få gyldige perioder`() {
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
                            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
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
                                    sak.rettighetsperiode.fom.plusMonths(5),
                                    sak.rettighetsperiode.fom.plusMonths(6)
                                )
                            )
                        ),
                        EtableringEgenVirksomhetLøsningDto(
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusMonths(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            virksomhetErNy = false,
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
                                    sak.rettighetsperiode.fom.plusMonths(3),
                                    sak.rettighetsperiode.fom.plusMonths(4)
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
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val etableringGrunnlag = dataSource.transaction {
            EtableringEgenVirksomhetRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(etableringGrunnlag?.vurderinger?.size).isEqualTo(2)
        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        dataSource.transaction {
            val service = EtableringEgenVirksomhetService(
                postgresRepositoryRegistry.provider(it)
            )
            val oppfylt = service.evaluerVirksomhetVurdering(etableringGrunnlag?.vurderinger?.first()!!)
            val ikkeOppfyltVurdering = service.evaluerVirksomhetVurdering(etableringGrunnlag.vurderinger.last())

            assertThat(oppfylt).isEqualTo(true)
            assertThat(ikkeOppfyltVurdering).isEqualTo(false)
        }
    }

    @Test
    fun `Skal kunne revurdere uten tidligere vurdering`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        behandling
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET)
        )
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
                                    sak.rettighetsperiode.fom.plusMonths(5),
                                    sak.rettighetsperiode.fom.plusMonths(6)
                                )
                            )
                        ),
                        EtableringEgenVirksomhetLøsningDto(
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusMonths(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            virksomhetErNy = false,
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
                                    sak.rettighetsperiode.fom.plusMonths(3),
                                    sak.rettighetsperiode.fom.plusMonths(4)
                                )
                            )
                        )
                    )
                )
            )
            .bekreftVurderinger()
            .fattVedtak()

        val etableringGrunnlag = dataSource.transaction {
            EtableringEgenVirksomhetRepositoryImpl(it).hentHvisEksisterer(revurdering.id)
        }

        assertThat(etableringGrunnlag?.vurderinger?.size).isEqualTo(2)
        assertThat(revurdering.status()).isEqualTo(Status.IVERKSETTES)
    }

    @Test
    fun `Skal kunne sende inn tom etablering-vurdering i revurdering`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        behandling
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        assertDoesNotThrow {
            sak.opprettManuellRevurdering(
                listOf(Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET)
            )
                .løsAvklaringsBehov(EtableringEgenVirksomhetLøsning(emptyList()))
        }
    }

    @Test
    fun `Ikke oppfylt om ikke 11-5 & 11-6b er oppfylt`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        val feil = assertThrows<UgyldigForespørselException> {
            behandling
                .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
                .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = false)
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
                )
        }
        assertThat(feil.message).contains("11-5 & 11-6b må være oppfylt i minst én periode")
    }
}
