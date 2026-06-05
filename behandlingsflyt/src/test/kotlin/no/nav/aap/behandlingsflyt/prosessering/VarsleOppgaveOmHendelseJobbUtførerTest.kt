package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingsEtEllerAnnet
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.JobbInput
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class VarsleOppgaveOmHendelseJobbUtførerTest {

    private val behandlingId = BehandlingId(999L)
    private val sakId = 42L

    private val utfører = VarsleOppgaveOmHendelseJobbUtFører.konstruer(
        inMemoryRepositoryProvider,
        createGatewayProvider { register<CapturingOppgavestyringGateway>() }
    )

    @BeforeEach
    fun setUp() {
        CapturingOppgavestyringGateway.instans.hendelseTilOppgave = null
    }

    @AfterEach
    fun tearDown() {
        InMemorySykdomRepository.slett(behandlingId)
        InMemoryStudentRepository.slett(behandlingId)
    }

    @Test
    fun `revurdering med kun avslag etter sykdomsvilkår setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(avslagSykdomsvurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Revurdering))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling med studentvurdering setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(avslagSykdomsvurdering()))
        InMemoryStudentRepository.lagre(behandlingId, setOf(enStudentVurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling uten sykdomsgrunnlag setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling med tom sykdomsliste setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, emptyList())

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling med kun avslag sykdomsvurderinger setter AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(avslagSykdomsvurdering(), avslagSykdomsvurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet)
            .isEqualTo(BehandlingsEtEllerAnnet.AVSLAG_11_5_FØRSTEGANGSBEHANDLING)
    }

    @Test
    fun `førstegangsbehandling med én ordinær oppfylt og én avslag setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(ordinærOppfyltSykdomsvurdering(), avslagSykdomsvurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling med én yrkesskade oppfylt og én avslag setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(yrkesskadeOppfyltSykdomsvurdering(), avslagSykdomsvurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }

    @Test
    fun `førstegangsbehandling med én sykepengeerstatningsvurdering og én avslag setter ikke AVSLAG_11_5_FØRSTEGANGSBEHANDLING`() {
        InMemorySykdomRepository.lagre(behandlingId, listOf(sykepengeerstatningVurdering(), avslagSykdomsvurdering()))

        utfører.utfør(jobbInput(TypeBehandling.Førstegangsbehandling))

        assertThat(CapturingOppgavestyringGateway.instans.hendelseTilOppgave?.behandlingsEtEllerAnnet).isNull()
    }


    private fun jobbInput(behandlingType: TypeBehandling): JobbInput =
        JobbInput(VarsleOppgaveOmHendelseJobbUtFører)
            .medPayload(enHendelse(behandlingType))
            .forBehandling(sakId, behandlingId.id)

    private fun enHendelse(behandlingType: TypeBehandling) = BehandlingFlytStoppetHendelse(
        personIdent = "12345678901",
        saksnummer = Saksnummer("SAK-001"),
        referanse = BehandlingReferanse(),
        behandlingType = behandlingType,
        årsakerTilBehandling = emptyList(),
        vurderingsbehov = emptyList(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        status = Status.OPPRETTET,
        avklaringsbehov = emptyList(),
        erPåVent = false,
        relevanteIdenterPåBehandling = emptyList(),
        mottattDokumenter = emptyList(),
        opprettetTidspunkt = LocalDateTime.now(),
        hendelsesTidspunkt = LocalDateTime.now(),
        versjon = "1",
    )

    private fun avslagSykdomsvurdering() = Sykdomsvurdering(
        begrunnelse = "Ikke oppfylt",
        vurderingenGjelderFra = LocalDate.now(),
        vurderingenGjelderTil = null,
        harSkadeSykdomEllerLyte = false,
        erSkadeSykdomEllerLyteVesentligdel = null,
        erNedsettelseIArbeidsevneMerEnnHalvparten = null,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = null,
        diagnose = null,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("Z00000"),
    )

    private fun ordinærOppfyltSykdomsvurdering() = Sykdomsvurdering(
        begrunnelse = "Oppfylt ordinær",
        vurderingenGjelderFra = LocalDate.now(),
        vurderingenGjelderTil = null,
        harSkadeSykdomEllerLyte = true,
        erSkadeSykdomEllerLyteVesentligdel = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
        diagnose = null,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("Z00000"),
    )

    private fun yrkesskadeOppfyltSykdomsvurdering() = Sykdomsvurdering(
        begrunnelse = "Oppfylt yrkesskade",
        vurderingenGjelderFra = LocalDate.now(),
        vurderingenGjelderTil = null,
        harSkadeSykdomEllerLyte = true,
        erSkadeSykdomEllerLyteVesentligdel = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten = false,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
        diagnose = null,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("Z00000"),
    )

    private fun sykepengeerstatningVurdering() = Sykdomsvurdering(
        begrunnelse = "Sykepengeerstatning",
        vurderingenGjelderFra = LocalDate.now(),
        vurderingenGjelderTil = null,
        harSkadeSykdomEllerLyte = true,
        erSkadeSykdomEllerLyteVesentligdel = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER,
        diagnose = null,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("Z00000"),
    )

    private fun enStudentVurdering() = StudentVurdering(
        fom = LocalDate.now(),
        tom = null,
        begrunnelse = "Student",
        harAvbruttStudie = true,
        godkjentStudieAvLånekassen = true,
        avbruttPgaSykdomEllerSkade = true,
        harBehovForBehandling = true,
        avbruttStudieDato = LocalDate.now(),
        avbruddMerEnn6Måneder = true,
        vurdertAv = "Z00000",
        vurdertIBehandling = behandlingId,
        diagnose = null,
    )
}

internal class CapturingOppgavestyringGateway private constructor() : OppgavestyringGateway {

    var hendelseTilOppgave: BehandlingFlytStoppetHendelse? = null

    override fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        hendelseTilOppgave = hendelse
    }

    override fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse) {}

    override fun finnNayEnhetForPerson(personIdent: String, relevanteIdenter: List<String>): EnhetNrDto =
        EnhetNrDto("1234")

    override fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse =
        OppgaveEnhetResponse(emptyList())

    companion object : Factory<OppgavestyringGateway> {
        val instans = CapturingOppgavestyringGateway()
        override fun konstruer(): OppgavestyringGateway = instans
    }
}
