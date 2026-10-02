package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgMedlemskapVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonopplysningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VurderLovvalgStegFaseATest {
    private val repo = InMemoryMedlemskapArbeidInntektRepository
    private val gatewayProvider = createGatewayProvider { register<AlleAvskruddUnleash>() }

    private val steg = VurderLovvalgSteg(
        vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
        personopplysningRepository = InMemoryPersonopplysningRepository,
        medlemskapArbeidInntektRepository = repo,
        tidligereVurderinger = FakeTidligereVurderinger(),
        avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, gatewayProvider),
        unleashGateway = AlleAvskruddUnleash,
    )

    @Test
    fun `lagrer automatisk vurdering for hele rettighetsperioden når kanBehandlesAutomatisk`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        lagGrunnlag(behandling, kanBehandlesAutomatisk = true)

        steg.utfør(kontekst(sak, behandling))

        val vurderinger = repo.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        with(vurderinger.single()) {
            assertThat(vurdertAv).isEqualTo(SYSTEMBRUKER)
            assertThat(overstyrt).isFalse
            assertThat(lovvalg.lovvalgsEØSLandEllerLandMedAvtale).isEqualTo(EØSLandEllerLandMedAvtale.NOR)
            assertThat(medlemskap?.varMedlemIFolketrygd).isTrue
            assertThat(fom).isEqualTo(sak.rettighetsperiode.fom)
            assertThat(tom).isEqualTo(sak.rettighetsperiode.tom.takeIf { it != Tid.MAKS })
            assertThat(vurdertIBehandling).isEqualTo(behandling.id)
        }
    }

    @Test
    fun `lagrer ingenting når kanBehandlesAutomatisk er false`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        lagGrunnlag(behandling, kanBehandlesAutomatisk = false)

        steg.utfør(kontekst(sak, behandling))

        assertThat(repo.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()).isEmpty()
    }

    @Test
    fun `lagrer ikke på nytt når automatisk vurdering allerede dekker hele perioden`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        lagGrunnlag(behandling, kanBehandlesAutomatisk = true)

        steg.utfør(kontekst(sak, behandling))
        steg.utfør(kontekst(sak, behandling))

        assertThat(repo.hentHvisEksisterer(behandling.id)!!.vurderinger).hasSize(1)
    }

    @Test
    fun `skriver ikke automatisk vurdering når manuell vurdering dekker hele perioden`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val manuell = vurdering(Bruker("Z000000"), sak.rettighetsperiode.fom, null, behandling.id)
        lagGrunnlag(behandling, kanBehandlesAutomatisk = true, vurderinger = listOf(manuell))

        steg.utfør(kontekst(sak, behandling))

        assertThat(repo.hentHvisEksisterer(behandling.id)!!.vurderinger).containsExactly(manuell)
    }

    @Test
    fun `skriver ikke automatisk vurdering og løfter behovet når manuell vurdering ikke dekker hele perioden`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val fom = sak.rettighetsperiode.fom
        val delvisManuell = vurdering(Bruker("Z000000"), fom, fom.plusDays(30), behandling.id)
        lagGrunnlag(behandling, kanBehandlesAutomatisk = true, vurderinger = listOf(delvisManuell))

        steg.utfør(kontekst(sak, behandling))

        assertThat(repo.hentHvisEksisterer(behandling.id)!!.vurderinger).containsExactly(delvisManuell)
        val behov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        assertThat(behov?.erÅpent()).isTrue
    }

    @Test
    fun `rører ikke arvet automatisk vurdering som ikke lenger stemmer`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val arvet = vurdering(SYSTEMBRUKER, sak.rettighetsperiode.fom, null, BehandlingId(Long.MAX_VALUE))
        lagGrunnlag(behandling, kanBehandlesAutomatisk = false, vurderinger = listOf(arvet))

        steg.utfør(kontekst(sak, behandling))

        assertThat(repo.hentHvisEksisterer(behandling.id)!!.vurderinger).containsExactly(arvet)
    }

    private fun kontekst(sak: Sak, behandling: Behandling): FlytKontekstMedPerioder =
        flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = VurderingType.FØRSTEGANGSBEHANDLING
            this.rettighetsperiode = sak.rettighetsperiode
        }

    private fun lagGrunnlag(
        behandling: Behandling,
        kanBehandlesAutomatisk: Boolean,
        vurderinger: List<LovvalgMedlemskapVurdering> = emptyList(),
    ) {
        InMemoryPersonopplysningRepository.lagre(
            behandling.id,
            Personopplysning(
                Fødselsdato(LocalDate.of(2000, 1, 1)),
                null,
                PersonStatus.bosatt,
                listOf(Statsborgerskap("NOR")),
            )
        )
        repo.lagreOppgittUtenlandsOppplysninger(
            behandling.id,
            JournalpostId("JP001"),
            UtenlandsOppholdData(
                harBoddINorgeSiste5År = true,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = null,
            )
        )
        repo.settGrunnlag(
            behandling.id,
            MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = if (kanBehandlesAutomatisk) medlemskapGrunnlagMedMedlem() else null,
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = vurderinger,
            )
        )
    }

    private fun medlemskapGrunnlagMedMedlem() = MedlemskapUnntakGrunnlag(
        unntak = listOf(
            Segment(
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)),
                verdi = Unntak(
                    "unntak",
                    "statusaarsak",
                    true,
                    "grunnlag",
                    "lovvalg",
                    false,
                    EØSLandEllerLandMedAvtale.NOR.toString(),
                    null
                )
            )
        )
    )

    private fun vurdering(vurdertAv: Bruker, fom: LocalDate, tom: LocalDate?, behandlingId: BehandlingId) =
        LovvalgMedlemskapVurdering(
            lovvalg = LovvalgDto("Begrunnelse", EØSLandEllerLandMedAvtale.NOR),
            medlemskap = MedlemskapDto("Begrunnelse", true),
            vurdertAv = vurdertAv,
            vurdertDato = LocalDateTime.of(2026, 1, 15, 12, 0),
            fom = fom,
            tom = tom,
            vurdertIBehandling = behandlingId,
        )
}
