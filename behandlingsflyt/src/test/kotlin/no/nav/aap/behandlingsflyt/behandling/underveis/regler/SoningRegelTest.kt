package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdInput
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SoningRegelTest {

    private val mockConnection = MockConnection().toDBConnection()
    val utlederService = InstitusjonsoppholdUtlederService(
        BarnetilleggRepositoryImpl(mockConnection),
        InstitusjonsoppholdRepositoryImpl(mockConnection),
        SakRepositoryImpl(mockConnection),
        BehandlingRepositoryImpl(mockConnection)
    )

    @Test
    fun vurder() {
        val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1))
        val utlederInput = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 2, 15))),
                    Institusjon(
                        Institusjonstype.FO,
                        Oppholdstype.S,
                        "123123123",
                        "test fengsel"
                    )
                )
            ),
            soningsvurderinger = listOf(
                Soningsvurdering(
                    skalOpphøre = true,
                    begrunnelse = "Formue under forvaring",
                    fraDato = LocalDate.of(2024, 1, 6)
                ),
                Soningsvurdering(
                    skalOpphøre = true,
                    begrunnelse = "Soner i fengsel",
                    fraDato = LocalDate.of(2024, 1, 11)
                ),
                Soningsvurdering(
                    skalOpphøre = false,
                    begrunnelse = "Jobber utenfor anstalten",
                    fraDato = LocalDate.of(2024, 1, 16)
                ),
                Soningsvurdering(
                    skalOpphøre = false,
                    begrunnelse = "Fotlenke",
                    fraDato = LocalDate.of(2024, 2, 6)
                ),
            ),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
            rettighetsperiode = periode,
        )

        val delresultat = utlederService.utledBehov(utlederInput)

        val vilkårsresultat = Vilkårsresultat()
        StraffegjennomføringVilkår(vilkårsresultat).vurder(
            StraffegjennomføringGrunnlag(
                institusjonsopphold = MapInstitusjonoppholdTilRegel.map(delresultat),
                vurderFra = periode.fom,
            )
        )
        val straffegjennomføringVilkårsvurderinger =
            vilkårsresultat.finnVilkår(Vilkårtype.STRAFFEGJENNOMFØRING).tidslinje()
        assertTidslinje(
            straffegjennomføringVilkårsvurderinger,

            //Soner ikke
            Periode(LocalDate.of(2024, 1, 1), (LocalDate.of(2024, 1, 5))) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(false)
            },

            //Formue under forvaltning og soner i fengsel
            Periode(LocalDate.of(2024, 1, 6), (LocalDate.of(2024, 1, 15))) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING)
                assertThat(it.manuellVurdering).isEqualTo(true)
            },

            // Arbeider utenfor anstalten og soner i ved frigang
            Periode(LocalDate.of(2024, 1, 16), (LocalDate.of(2024, 12, 1))) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(true)
            },

            // Automatisk oppfylt i fravær av soningsopphold
            Periode(LocalDate.of(2024, 12, 2), Tid.MAKS) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(false)
            }
        )
    }
}