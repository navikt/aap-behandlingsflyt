package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
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
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

class MigreringFraArenaFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    // Overstyrbar per test - default matcher konstruktørparameteren over
    private var unleash: KClass<out UnleashGateway> = AlleAvskruddUnleash::class
    override fun unleashGateway() = unleash

    @Test
    fun `skal kunne gjennomføre en migrering hvor stegene i en ordinær flyt må settes manuelt`() {
        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(behandling.vurderingsbehov().map { it.type })
                    .contains(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRERING_FRA_ARENA)
            }
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
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
            .løsVedtakslengde(startDato, startDato.plusMonths(6))
            .løsForeslåVedtak()
            .fattVedtak()
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(behandling.vurderingsbehov().map { it.type })
                    .contains(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRERING_FRA_ARENA)
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(behandling.status()).isEqualTo(AVSLUTTET)
                behandling.assertRettighetstype(
                    Periode(startDato, startDato.plusMonths(6)) to RettighetsType.BISTANDSBEHOV
                )
            }
    }

    @Test
    fun `skal automatisk legge inn sykdom- og bistandsvurdering basert på sykdomsvurdering fra Arena i behandlingen`() {
        unleash = MigreringFraArenaMedAutomatiskeVurderingerFlytTestUnleash::class

        val (sak, migreringsbehandling) = migrerFraArena()
        val startDato = sak.rettighetsperiode.fom

        migreringsbehandling
            .løsAvklaringsBehov(
                VurderKravLøsning(
                    kravVurderinger = setOf(
                        MigrertKravLøsningDto(
                            begrunnelse = "en begrunnelse",
                            virkningstidspunktArena = startDato.minusMonths(5),
                            muligRettFra = startDato,
                            arenaSaksnummer = "2016-123456",
                            rettighetstype = MigrertRettighetstype.ORDINÆR,
                            resterendeKvoteOrdinær = 300,
                        )
                    )
                )
            )
            .løsLovvalg(startDato, medlem = true)
            .medKontekst {
                val medlemskapArbeidInntektRepository: SykdomRepository = repositoryProvider.provide()
                val vurderinger = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger

                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.REFUSJON_KRAV)

                assertThat(vurderinger).hasSize(1)
                val vurdering = vurderinger!!.single()
                assertThat(vurdering.erSkadeSykdomEllerLyteVesentligdel).isTrue
                assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten).isTrue
                assertThat(vurdering.harSkadeSykdomEllerLyte).isTrue
                assertThat(vurdering.harNedsattArbeidsevne).isEqualTo(ArbeidsevneNedsattValg.JA)
                assertThat(vurdering.fom).isEqualTo(sak.rettighetsperiode.fom)
                assertThat(vurdering.tom).isNull()
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

object MigreringFraArenaMedAutomatiskeVurderingerFlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk,
    )
)