package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepositoryImpl
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
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
                            versjon = EtableringVersjon.V1,
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusDays(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            virksomhetErNy = true,
                            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                            kanFøreTilSelvforsørget = true,
                            fase = EtableringFase.UTVIKLING,
                            erRegistrertINødvendigeOffentligeRegister = null,
                            jobberBrukerAktivMedVirksomheten = true
                        ),
                        // Ikke oppfylt vurdering
                        EtableringEgenVirksomhetLøsningDto(
                            versjon = EtableringVersjon.V1,
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusMonths(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            virksomhetErNy = false,
                            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                            kanFøreTilSelvforsørget = true,
                            erRegistrertINødvendigeOffentligeRegister = true,
                            jobberBrukerAktivMedVirksomheten = true,
                            fase = null
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(avklaringsbehovSomMåLøses).noneMatch { it.definisjon == Definisjon.ETABLERING_EGEN_VIRKSOMHET }
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

        val oppfyltVurdering = requireNotNull(etableringGrunnlag?.vurderinger)
            .single { it.virksomhetErNy == true }
        val ikkeOppfyltVurdering = requireNotNull(etableringGrunnlag.vurderinger)
            .single { it.virksomhetErNy == false }

        assertThat(oppfyltVurdering.evaluerVirksomhetVurdering()).isEqualTo(true)
        assertThat(ikkeOppfyltVurdering.evaluerVirksomhetVurdering()).isEqualTo(false)
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
                        // Oppfylt
                        EtableringEgenVirksomhetLøsningDto(
                            versjon = EtableringVersjon.V1,
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusDays(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            fase = EtableringFase.UTVIKLING,
                            jobberBrukerAktivMedVirksomheten = true,
                            virksomhetErNy = true,
                            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                            kanFøreTilSelvforsørget = true,
                        ),
                        // Ikke oppfylt
                        EtableringEgenVirksomhetLøsningDto(
                            versjon = EtableringVersjon.V1,
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusMonths(1),
                            tom = null,
                            virksomhetNavn = "peppas peppers",
                            orgNr = null,
                            foreliggerFagligVurdering = true,
                            fase = null,
                            virksomhetErNy = false,
                            jobberBrukerAktivMedVirksomheten = true,
                            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                            kanFøreTilSelvforsørget = true,
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
                                versjon = EtableringVersjon.V1,
                                begrunnelse = "meee",
                                fom = sak.rettighetsperiode.fom.plusDays(1),
                                tom = null,
                                virksomhetNavn = "peppas peppers",
                                orgNr = null,
                                foreliggerFagligVurdering = true,
                                virksomhetErNy = true,
                                brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                                kanFøreTilSelvforsørget = true,
                                fase = EtableringFase.UTVIKLING,
                                jobberBrukerAktivMedVirksomheten = true,
                                erRegistrertINødvendigeOffentligeRegister = true
                            )
                        )
                    )
                )
        }
        assertThat(feil.message).contains("11-5 & 11-6b må være oppfylt i minst én periode")
    }
}
