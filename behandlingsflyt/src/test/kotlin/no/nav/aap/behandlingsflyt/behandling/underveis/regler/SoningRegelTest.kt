package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdInput
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
        val periode = Periode(1 januar 2024, 1 desember 2024)
        val utlederInput = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(6 januar 2024, 15 februar 2024),
                    Institusjon(
                        Institusjonstype.FO,
                        Oppholdstype.S,
                        "123123123",
                        "test fengsel"
                    )
                )
            ),
            soningsvurderinger = Soningsvurderinger(
                vurderinger = listOf(
                    Soningsvurdering(
                        skalOpphøre = true,
                        begrunnelse = "Formue under forvaring",
                        fraDato = 6 januar 2024
                    ),
                    Soningsvurdering(
                        skalOpphøre = true,
                        begrunnelse = "Soner i fengsel",
                        fraDato = 11 januar 2024
                    ),
                    Soningsvurdering(
                        skalOpphøre = false,
                        begrunnelse = "Jobber utenfor anstalten",
                        fraDato = 16 januar 2024
                    ),
                    Soningsvurdering(
                        skalOpphøre = false,
                        begrunnelse = "Fotlenke",
                        fraDato = 6 februar 2024
                    ),
                ),
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            ),
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = periode,
        )

        val delresultat = utlederService.utledBehov(utlederInput)

        val straffegjennomføringVilkårsvurderinger = StraffegjennomføringVilkår.vurder(StraffegjennomføringGrunnlag(
            institusjonsopphold = MapInstitusjonoppholdTilRegel.map(delresultat),
            vurderFra = periode.fom,
        ))

        assertTidslinje(
            straffegjennomføringVilkårsvurderinger,

            //Soner ikke
            Periode(1 januar 2024, 5 januar 2024) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(false)
            },

            //Formue under forvaltning og soner i fengsel
            Periode(6 januar 2024, 15 januar 2024) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(it.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING)
                assertThat(it.manuellVurdering).isEqualTo(true)
            },

            // Arbeider utenfor anstalten og soner i ved frigang
            Periode(16 januar 2024, 1 desember 2024) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(true)
            },

            // Automatisk oppfylt i fravær av soningsopphold
            Periode(2 desember 2024, Tid.MAKS) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(it.manuellVurdering).isEqualTo(false)
            }
        )
    }
}