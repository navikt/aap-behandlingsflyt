package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MigreringFraArenaV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class MigreringFraArenaFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    @Test
    fun `skal opprette og kunne løse manuelt avklaringsbehov for vedtakslengde ved migrering fra Arena`() {
        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom
        val sluttdatoArena = startDato.plusMonths(9)
        val begrunnelse = "Vedtakslengde satt likt som i Arena"

        val behandlingPåVedtakslengde = migreringsbehandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(behandling.vurderingsbehov().map { it.type })
                    .contains(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRERING_FRA_ARENA)
            }
            .løsLovvalg(startDato)
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsFastsettManuellInntekt()
            .løsForutgåendeMedlemskap(startDato)
            .løsOppholdskrav(startDato)
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()

        // Migreringen skal alltid løfte et manuelt avklaringsbehov for vedtakslengde for de første sakene
        behandlingPåVedtakslengde.medKontekst {
            assertThat(behandling.aktivtSteg()).isEqualTo(StegType.FASTSETT_VEDTAKSLENGDE)
            assertStatusForDefinisjon(
                åpneAvklaringsbehov,
                Definisjon.AVKLAR_VEDTAKSLENGDE,
                Status.OPPRETTET
            )

            // Ingen automatisk vedtakslengde lagres ved migrering - behovet må løses manuelt
            val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
            assertThat(vedtakslengdeRepository.hentHvisEksisterer(behandling.id)).isNull()
        }

        // Behovet skal kunne løses ved å legge inn en vurdering
        behandlingPåVedtakslengde
            .løsAvklaringsBehov(
                AvklarVedtakslengdeLøsning(
                    løsningerForPerioder = listOf(
                        VedtakslengdeVurderingDto(
                            fom = startDato,
                            tom = sluttdatoArena,
                            årsaker = listOf(VedtakslengdeÅrsak.MAKS_ETT_ÅR),
                            sluttdato = sluttdatoArena,
                            begrunnelse = begrunnelse
                        )
                    )
                )
            )
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandling.id)

                assertThat(vedtakslengdeGrunnlag).isNotNull
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(sluttdatoArena)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo(begrunnelse)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertManuelt).isTrue

                // Behovet skal ikke løftes på nytt når vurderingen er lagret
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .doesNotContain(Definisjon.AVKLAR_VEDTAKSLENGDE)
                assertThat(behandling.aktivtSteg()).isNotEqualTo(StegType.FASTSETT_VEDTAKSLENGDE)
            }
    }

    private fun migrerFraArena(
        person: TestPerson = TestPersoner.STANDARD_PERSON(),
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(clock),
        saksnummerArena: String = "2016-123456",
    ): Pair<Sak, Behandling> {
        val sak = dataSource.transaction { connection ->
            PersonOgSakService(
                gatewayProvider,
                postgresRepositoryRegistry.provider(connection)
            ).opprettSakMedArenaMigrering(
                ident = person.aktivIdent(),
                søknadsdato = mottattTidspunkt.toLocalDate(),
                saksnummerArena = saksnummerArena,
            )
        }

        sak.sendInn(
            referanse = InnsendingReferanse(
                InnsendingReferanse.Type.MIGRERING_FRA_ARENA,
                UUID.randomUUID().toString(),
            ),
            type = InnsendingType.MIGRERING_FRA_ARENA,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = MigreringFraArenaV0("Migrering av Arenasak $saksnummerArena"),
        )

        return hentSak(sak.saksnummer) to hentSisteOpprettedeBehandlingForSak(sak.id)
    }

}