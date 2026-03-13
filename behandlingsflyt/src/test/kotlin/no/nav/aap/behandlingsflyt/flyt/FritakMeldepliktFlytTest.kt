package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertFritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.PeriodisertFritaksvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RevurderFritakMeldepliktFlytTest : AbstraktFlytOrkestratorTest(RevurderFritakMeldepliktUnleash::class) {
    @Test
    fun `Skal kunne revurdere uten tidligere vurdering`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())

        behandling
            .løsSykdom(vurderingGjelderFra = sak.rettighetsperiode.fom, erOppfylt = true)
            .løsBistand(fom = sak.rettighetsperiode.fom, erOppfylt = true, erBehovForArbeidsrettetTiltak = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()

        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.REVURDER_FRITAK_MELDEPLIKT)
        )
            .løsAvklaringsBehov(
                PeriodisertFritakMeldepliktLøsning(
                    AvklaringsbehovKode.`5005`,
                    listOf(
                        PeriodisertFritaksvurderingDto(
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusDays(1),
                            tom = null,
                            harFritak = true
                        ),
                        PeriodisertFritaksvurderingDto(
                            begrunnelse = "meee",
                            fom = sak.rettighetsperiode.fom.plusMonths(1),
                            tom = null,
                            harFritak = false
                        )
                    )
                )
            )
            .fattVedtak()

        val meldepliktGrunnlag = dataSource.transaction {
            MeldepliktRepositoryImpl(it).hentHvisEksisterer(revurdering.id)
        }

        assertThat(meldepliktGrunnlag?.vurderinger?.size).isEqualTo(2)
        assertThat(revurdering.status()).isEqualTo(Status.IVERKSETTES)
    }


    object RevurderFritakMeldepliktUnleash : FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(
            BehandlingsflytFeature.RevurderFritakMeldeplikt
        )
    )
}