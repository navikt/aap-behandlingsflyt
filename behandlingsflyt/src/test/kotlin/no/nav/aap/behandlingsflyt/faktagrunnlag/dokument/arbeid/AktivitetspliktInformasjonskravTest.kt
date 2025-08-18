package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.innsendingType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AktivitetspliktInformasjonskravTest {
    private val repositoryRegistry = RepositoryRegistry()
        .register<MottattDokumentRepositoryImpl>()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<TrukketSøknadRepositoryImpl>()
        .register<VilkårsresultatRepositoryImpl>()
        .register<AktivitetspliktRepositoryImpl>()

    @Test
    fun `detekterer nye dokumenter og legger dem til i grunnlaget`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = nySak(connection)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                TypeBehandling.Førstegangsbehandling,
                null,
                VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.SØKNAD),
            )
            val aktivitetspliktInformasjonskrav = AktivitetspliktInformasjonskrav.konstruer(
                repositoryRegistry.provider(connection),
                createGatewayProvider { },
            )
            val flytKontekst = flytKontekstMedPerioder(behandling)

            nyeBrudd(
                connection, sak,
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Orket ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            ).first().also {
                mottattDokument(connection, it, sak)
            }


            // Før vi oppdaterer kravinformasjonen, så finnes det ingen grunnlag
            AktivitetspliktRepositoryImpl(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertNull(it)
            }

            // Etter første oppdatering av kravinformasjonen, skal bruddet vi la inn over dukke opp
            aktivitetspliktInformasjonskrav.oppdater(flytKontekst)
            AktivitetspliktRepositoryImpl(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertEquals(1, it?.bruddene?.size)
            }

            // Ved oppdatering av kravinformasjonen uten ny brudd, skal grunnlaget være uendret
            aktivitetspliktInformasjonskrav.oppdater(flytKontekst)
            AktivitetspliktRepositoryImpl(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertEquals(1, it?.bruddene?.size)
            }
        }
    }

    private fun mottattDokument(
        connection: DBConnection,
        brudd: AktivitetspliktDokument,
        sak: Sak
    ) {
        val dokument = StrukturertDokument(
            AktivitetskortV0(
                fraOgMed = brudd.brudd.periode.fom,
                tilOgMed = brudd.brudd.periode.tom,
            )
        )
        MottaDokumentService(MottattDokumentRepositoryImpl(connection)).mottattDokument(
            InnsendingReferanse(brudd.metadata.innsendingId),
            sak.id,
            LocalDateTime.ofInstant(brudd.metadata.opprettetTid, ZoneId.of("Europe/Oslo")),
            dokument.data.innsendingType(),
            kanal = Kanal.PAPIR,
            dokument,
        )
    }

    private fun flytKontekstMedPerioder(behandling: Behandling) =
        FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )
}