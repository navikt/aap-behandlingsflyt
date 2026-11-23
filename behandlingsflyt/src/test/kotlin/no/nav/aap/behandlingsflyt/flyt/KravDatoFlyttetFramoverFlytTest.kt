package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Kravdatoen flyttes fram på det samme kravet (samme referanse) i en revurdering.
 *
 * Førstegangsbehandlingen overstyrer «mulig rett fra» bakover til [opprinneligKravdato]. Revurderingen fjerner
 * overstyringen, så kravdatoen blir søknadsdatoen ([nyKravdato]). Fra ny kravdato er kravreferansen uendret, så
 * perioden «behøver ikke vurdering». Sykdomsvurderingen fra førstegangsbehandlingen starter før ny kravdato, så kravet
 * er likevel ikke dekket. Både steget og valideringen skal da kreve en vurdering fra ny kravdato.
 */
class KravDatoFlyttetFramoverFlytTest : AbstraktFlytOrkestratorTest(LokalUnleash::class) {

    private val nyKravdato: LocalDate = LocalDate.now().minusMonths(2)
    private val opprinneligKravdato: LocalDate = nyKravdato.minusMonths(1)
    private val medSykdomsrevurdering =
        listOf(Vurderingsbehov.VURDER_KRAV, Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)

    @Test
    fun `validering avviser sykdomsløsning som ikke dekker kravet når kravdato er flyttet fram i revurdering`() {
        val revurdering = revurderingMedKravdatoFlyttetFram(medSykdomsrevurdering)

        revurdering.medKontekst {
            assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).contains(Definisjon.AVKLAR_SYKDOM)
        }

        assertThatThrownBy { revurdering.løsSykdom(vurderingGjelderFra = opprinneligKravdato) }
            .describedAs("Valideringen skal avvise løsningen, ikke la steget løfte behovet på nytt")
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("på grunn av krav")
    }

    @Test
    fun `sykdomsløsning fra ny kravdato lukker behovet`() {
        val revurdering = revurderingMedKravdatoFlyttetFram(medSykdomsrevurdering)

        revurdering
            .løsSykdom(vurderingGjelderFra = nyKravdato)
            .medKontekst {
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .describedAs("Vurdering fra ny kravdato dekker kravet")
                    .doesNotContain(Definisjon.AVKLAR_SYKDOM)
            }
    }

    @Test
    fun `dagens oppførsel - bare VURDER_KRAV løfter ikke sykdom, og kravet står udekket`() {
        val revurdering = revurderingMedKravdatoFlyttetFram(listOf(Vurderingsbehov.VURDER_KRAV))

        revurdering.medKontekst {
            assertThat(hentSak(behandling).rettighetsperiode.fom)
                .describedAs("Rettighetsperioden starter på søknadsdatoen, ikke på overstyrt mulig rett fra")
                .isEqualTo(nyKravdato)
            assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                .describedAs("Kravet er bare endret før rettighetsperioden, så sykdom behøver ikke vurdering")
                .doesNotContain(Definisjon.AVKLAR_SYKDOM)
            val sykdomsvurderinger = repositoryProvider.provide<SykdomRepository>().hent(behandling.id)
                .somSykdomsvurderingstidslinje()
            assertThat(sykdomsvurderinger.segmenter().first().periode.fom)
                .describedAs("Gjeldende sykdomsvurdering starter før ny kravdato, så kravet er ikke dekket")
                .isEqualTo(opprinneligKravdato)
        }
    }

    /**
     * Revurdering der saksbehandler fjerner overstyringen av «mulig rett fra» på det samme kravet. Kravdatoen flyttes
     * da fra [opprinneligKravdato] til søknadsdatoen [nyKravdato].
     */
    private fun revurderingMedKravdatoFlyttetFram(vurderingsbehov: List<Vurderingsbehov>): Behandling {
        val sak = førstegangsbehandlingMedOverstyrtKravdato()
        val kravFraFørstegangsbehandling = hentGjeldendeRelevantKrav(sak)

        val revurdering = sak.opprettManuellRevurdering(vurderingsbehov)
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).contains(Definisjon.VURDER_KRAV)
            }
            .løsKrav(kravFraFørstegangsbehandling, overstyrMuligRettFra = null)

        revurdering.medKontekst {
            val krav = hentGjeldendeRelevantKrav(behandling)
            assertThat(krav.referanse)
                .describedAs("Fjerning av overstyringen skal beholde referansen til kravet")
                .isEqualTo(kravFraFørstegangsbehandling.referanse)
            assertThat(krav.muligRettFra).isEqualTo(nyKravdato)
        }

        return revurdering.løsLovvalgHvisÅpent(opprinneligKravdato)
    }

    /**
     * Søknaden mottas [nyKravdato]. Saksbehandler overstyrer «mulig rett fra» bakover til [opprinneligKravdato], og alle
     * vurderinger gjelder fra den datoen. Saken blir vedtatt.
     */
    private fun førstegangsbehandlingMedOverstyrtKravdato(): Sak {
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = nyKravdato.atStartOfDay())

        // Søknaden vurderes automatisk når den er alene i behandlingen, så kravbehovet må løftes eksplisitt.
        val kravFraSøknad = hentGjeldendeRelevantKrav(førstegangsbehandling)
        val behandling = førstegangsbehandling
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_KRAV)
            .medKontekst {
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).contains(Definisjon.VURDER_KRAV)
            }
            .løsKrav(
                kravFraSøknad,
                overstyrMuligRettFra = OverstyrMuligRettFra(
                    dato = opprinneligKravdato,
                    årsak = OverstyrMuligRettFraÅrsak.MisvisendeOpplysninger,
                    begrunnelse = "Skulle fått beskjed om å søke tidligere"
                )
            )
            .medKontekst {
                val krav = hentGjeldendeRelevantKrav(behandling)
                assertThat(krav.referanse).isEqualTo(kravFraSøknad.referanse)
                assertThat(krav.muligRettFra).isEqualTo(opprinneligKravdato)
                assertThat(hentSak(behandling).rettighetsperiode.fom)
                    .describedAs("Rettighetsperioden følger ikke overstyrt mulig rett fra")
                    .isEqualTo(nyKravdato)
            }
            .løsLovvalgHvisÅpent(opprinneligKravdato)
            .løsSykdom(opprinneligKravdato)
            .løsBistand(opprinneligKravdato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(opprinneligKravdato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        return hentSak(behandling)
    }

    private fun Behandling.løsKrav(krav: RelevantKrav, overstyrMuligRettFra: OverstyrMuligRettFra?): Behandling =
        løsAvklaringsBehov(
            VurderKravLøsning(
                kravVurderinger = setOf(
                    RelevantKravLøsningDto(
                        referanse = krav.referanse.verdi,
                        journalpostId = krav.journalpostId,
                        begrunnelse = "Vurderer kravet",
                        søknadsdato = krav.søknadsdato,
                        overstyrMuligRettFra = overstyrMuligRettFra,
                    )
                )
            )
        )

    private fun Behandling.løsLovvalgHvisÅpent(gjelderFra: LocalDate): Behandling {
        val lovvalgErÅpent = hentAvklaringsbehovSomMåLøses(id)
            .any { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP }
        return if (lovvalgErÅpent) løsLovvalg(gjelderFra) else this
    }

    private fun hentGjeldendeRelevantKrav(sak: Sak): RelevantKrav =
        hentGjeldendeRelevantKrav(hentSisteOpprettedeBehandlingForSak(sak.id))

    private fun hentGjeldendeRelevantKrav(behandling: Behandling): RelevantKrav {
        lateinit var krav: RelevantKrav
        behandling.medKontekst {
            krav = repositoryProvider.provide<KravRepository>().hentHvisEksisterer(this.behandling.id)!!
                .gjeldendeRelevanteKrav().single()
        }
        return krav
    }
}
