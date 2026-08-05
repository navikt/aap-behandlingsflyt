package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import no.nav.aap.motor.JobbStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Tag("motor")
@ParameterizedClass
@MethodSource("unleashTestDataSource")
class FlytOrkestratorTest(unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {
    @Test
    fun `hopper over foreslå vedtak-steg når revurdering ikke skal innom NAY`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        // Revurdering av sykdom uten 11-13
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = true
        )
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal gå rett til beslutter når ingen avklaringsbehov trenger å løses av NAY"
                }.containsExactly(Definisjon.FATTE_VEDTAK)
            }
    }

    @Test
    fun `revurdering skal innom foreslå vedtak-steg når NAY-saksbehandler har løst avklaringsbehov`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        // Revurdering som krever 11-13-vurdering
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = false
        )
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsSykepengeerstatning(sak.rettighetsperiode.fom to true)
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal innom foreslå vedtak-steg når vurdering av sykepengeerstatning er gjort av NAY"
                }.containsExactly(Definisjon.FORESLÅ_VEDTAK)
            }
    }

    @Test
    fun `kan tilbakeføre behandling til start`() {
        // Given:
        val (_, behandling) = sendInnFørsteSøknad()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov)
                .extracting<Definisjon> { it.definisjon }
                .containsOnly(Definisjon.AVKLAR_SYKDOM)
        }

        val antallKjøringerVurderRettighetsperiode = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }

        // When:
        dataSource.transaction { connection ->
            val driftfunksjoner = Driftfunksjoner(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            driftfunksjoner.kjørFraSteg(behandling, VURDER_RETTIGHETSPERIODE)
        }

        // Then:
        // Har kjørt steget vi rullet tilbake til én gang til
        val antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }
        assertThat(antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring)
            .isEqualTo(antallKjøringerVurderRettighetsperiode + 1)

        // Tilbake til AVKLAR_SYKDOM
        dataSource.transaction { connection ->
            assertThat(BehandlingRepositoryImpl(connection).hent(behandling.id))
                .extracting { it?.aktivtSteg() }
                .isEqualTo(StegType.AVKLAR_SYKDOM)
        }
    }

    @Test
    fun `Skal tilbakeføres ved nytt vurderingsbehov selv om det allerede er køet opp en prosesseringsjobb`() {
        // Flyt frem til avklar sykdom
        val (sak, behandling) = sendInnFørsteSøknad()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov)
                .extracting<Definisjon> { it.definisjon }
                .containsOnly(Definisjon.AVKLAR_SYKDOM)
        }

        // Kø opp prosesser behandling og sett status til feilet, slik at den ikke plukkes umiddelbart
        val jobbHistorikkFørHendelse = dataSource.transaction { connection ->
            val flytJobbRepo = FlytJobbRepositoryImpl(connection)
            
            val jobbHistorikkFør = flytJobbRepo.hentJobberMedHistorikkForSak(sak.id.toLong())
            val jobbInput = JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
                sak.id.toLong(), behandling.id.toLong()
            ).medCallId()

            flytJobbRepo.leggTil(jobbInput)

            val jobbHistorikkEtter = flytJobbRepo.hentJobberMedHistorikkForSak(sak.id.toLong())
            assertThat(jobbHistorikkEtter).hasSize(jobbHistorikkFør.size + 1)

            val prosesseringsjobb = jobbHistorikkEtter.maxBy { it.jobb.jobbId() }
            assertThat(prosesseringsjobb.historikk.single().status).isEqualTo(JobbStatus.KLAR)

            settJobbStatus(connection, prosesseringsjobb.jobb.jobbId(), JobbStatus.FEILET, "Feilet for test")
            flytJobbRepo.hentJobberMedHistorikkForSak(sak.id.toLong())
        }

        // Send inn nytt vurderingsbehov (hendelse) som trigger lovvalg
        sak.opprettManuellRevurdering(Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)

        // Setter feilet jobb til KLAR igjen
        dataSource.transaction { connection ->
            val jobberMedHistorikk = FlytJobbRepositoryImpl(connection).hentJobberMedHistorikkForSak(sak.id.toLong())
            assertThat(jobberMedHistorikk.none { it.jobb.status() == JobbStatus.KLAR })
            val feiletJobb = jobberMedHistorikk.singleOrNull { it.jobb.status() == JobbStatus.FEILET }?.jobb
            assertThat(feiletJobb).isNotNull
            settJobbStatus(connection, feiletJobb!!.jobbId(), JobbStatus.KLAR)
        }
        // Plukker neste, som er den feilede jobben
        motor.kjørJobber()

        // Assert at tilbakeføringen har skjedd
        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov)
                .extracting<Definisjon> { it.definisjon }
                .containsExactly(Definisjon.AVKLAR_SYKDOM, Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }

        // Assert at det ikke ble opprettet en ny prosesseringsjobb, siden denne fantes fra før
        dataSource.transaction { connection ->
            val flytJobbRepo = FlytJobbRepositoryImpl(connection)
            val jobbHistorikk = flytJobbRepo.hentJobberMedHistorikkForSak(sak.id.toLong())
            val antallProsesseringsJobberFør =
                jobbHistorikkFørHendelse.filter { it.jobb.type() == ProsesserBehandlingJobbUtfører.type }.size
            assertThat(jobbHistorikk.filter { it.jobb.type() == ProsesserBehandlingJobbUtfører.type }).hasSize(
                antallProsesseringsJobberFør
            )
        }
    }
}

private fun settJobbStatus(connection: DBConnection, jobbId: Long, status: JobbStatus, feilmelding: String? = null) {
    connection.execute("UPDATE JOBB SET status = ? WHERE id = ?") {
        setParams {
            setEnumName(1, status)
            setLong(2, jobbId)
        }
    }

    connection.execute(
        """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status, feilmelding, opprettet_tid) VALUES (?, ?, ?, ?)
            """.trimIndent()
    ) {
        setParams {
            setLong(1, jobbId)
            setEnumName(2, status)
            setString(3, feilmelding)
            setLocalDateTime(4, LocalDateTime.now())
        }
    }
}
