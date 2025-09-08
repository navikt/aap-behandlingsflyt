package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import javax.sql.DataSource
import kotlin.test.Test

class AvklarSykdomLøserTest {
    @TestDatabase
    lateinit var dataSource: DataSource

    @Test
    fun `Skal utlede hvilken vurdering som ble gjort i nåværende behandling`() {

        val (førstegangsbehandling, revurdering) = dataSource.transaction { connection ->
            val sak = opprettSak(connection, Periode(1 januar 2020, 1 januar 2023))
            val førstegang = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id, TypeBehandling.Førstegangsbehandling, null, VurderingsbehovOgÅrsak(
                    listOf(
                        VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)
                    ),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                    opprettet = (1 januar 2020).atStartOfDay(),
                )
            )
            val revurdering = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id, TypeBehandling.Revurdering, førstegang.id, VurderingsbehovOgÅrsak(
                    listOf(
                        VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
                    ),
                    årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                    opprettet = (1 januar 2021).atStartOfDay(),
                )
            )
            Pair(førstegang, revurdering)
        }

        dataSource.transaction { connection ->
            val løsning1 = løser(connection).løs(
                AvklaringsbehovKontekst(
                    Bruker("Saksbehandler 1"),
                    FlytKontekst(
                        førstegangsbehandling.sakId,
                        førstegangsbehandling.id,
                        forrigeBehandlingId = null,
                        TypeBehandling.Førstegangsbehandling
                    )
                ),
                løsning = AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("12312983")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                )
            )
            assertThat(løsning1).isEqualTo(LøsningsResultat("Vurdering av § 11-5"))
        }

        assertThat(grunnlag(førstegangsbehandling.id)).isNotNull
        
        dataSource.transaction{ connection -> GrunnlagKopierer(connection).overfør(førstegangsbehandling.id, revurdering.id)}
        assertThat(grunnlag(revurdering.id)).isNotNull


        dataSource.transaction { connection ->
            val løsning2 = løser(connection).løs(
                AvklaringsbehovKontekst(
                    Bruker("Saksbehandler 2"),
                    FlytKontekst(
                        revurdering.sakId,
                        revurdering.id,
                        forrigeBehandlingId = førstegangsbehandling.id,
                        TypeBehandling.Revurdering
                    )
                ),
                løsning = AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Mindre enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("12312983")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = 1 januar 2021,
                        )
                    )
                )
            )
            assertThat(løsning2).isEqualTo(LøsningsResultat("Vurdering av § 11-5"))
        }
        
        val sykdomsGrunnlag = grunnlag(revurdering.id)
        val nåTilstand = sykdomsGrunnlag!!.sykdomsvurderinger
        assertThat(nåTilstand).hasSize(2)
        val vedtatte = grunnlag(førstegangsbehandling.id)!!.sykdomsvurderinger

        val vedtatteSykdomsvurderingerIder = vedtatte.map { it.id }
        val sykdomsvurderinger = nåTilstand.filterNot { it.id in vedtatteSykdomsvurderingerIder }
        
        // Denne funker ikke fordi vi lager ny vurdering ved innsending, og vil dermed ha ulik id
        assertThat(sykdomsvurderinger).hasSize(1)
    }

    private fun løser(connection: DBConnection): AvklarSykdomLøser {
        return AvklarSykdomLøser(
            BehandlingRepositoryImpl(connection), SykdomRepositoryImpl(connection),
            YrkesskadeRepositoryImpl(connection)
        )
    }

    private fun grunnlag(behandlingId: BehandlingId): SykdomGrunnlag? {
        return dataSource.transaction { connection ->
            SykdomRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }
    }
}