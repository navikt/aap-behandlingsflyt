package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStønadsperiodeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StansEllerOpphørDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StansOpphørVurderingTypeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals

class AvklarStønadsperiodeLøserTest {
    private val løser = AvklarStønadsperiodeLøser(InMemoryKravRepository, InMemoryStønadsperiodeRepository)

    @Test
    fun `Skal kaste feil hvis man ikke oppgir stans- eller opphør-årsak dersom man skal tre inn i eksisterende stønadsperiode`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(krav(behandlingId, kravreferanse, 15 januar 2026))
        )

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = true,
                    harGjenværendeKvote = false,
                    stansOpphør = null,
                    startDato = 15 januar 2026,
                )
            )
        )

        val exception = assertThrows<UgyldigForespørselException> { løser.løs(kontekst(sakId, behandlingId), løsning) }
        assertEquals("Stans/opphør-årsak er påkrevd ved gjenopptak/gjeninntreden", exception.message)
    }


    @Test
    fun `Skal kaste feil hvis man mangler løsning for relevante krav`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse1 = Kravreferanse.ny()
        val kravreferanse2 = Kravreferanse.ny()
        val kravreferanse3 = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(
                krav(behandlingId, kravreferanse1, 15 januar 2020),
                krav(behandlingId, kravreferanse2, 15 januar 2023),
                krav(behandlingId, kravreferanse3, 15 januar 2026)
            )
        )

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse1,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = false,
                    harGjenværendeKvote = false,
                    stansOpphør = null,
                    startDato = 15 januar 2020,
                ))
        )

        val exception = assertThrows<UgyldigForespørselException> { løser.løs(kontekst(sakId, behandlingId), løsning) }
        assertEquals("Det finnes krav som mangler vurdering: ${kravreferanse2}, ${kravreferanse3}", exception.message)
    }

    @Test
    fun `Skal kaste feil hvis man prøver å knytte vurdering mot ikke-eksisterende krav`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(krav(behandlingId, kravreferanse, 15 januar 2026))
        )

        val løsningForPåkrevdKrav = StønadsperiodeLøsningDto(
            referanse = kravreferanse,
            begrunnelse = "test",
            harHattOrdinærSiste52Uker = false,
            harGjenværendeKvote = false,
            stansOpphør = null,
            startDato = 15 januar 2026,
        )
        
        val ikkeRelevantReferanse = Kravreferanse.ny()
        val løsningForKravSomIkkeEksisterer = StønadsperiodeLøsningDto(
            referanse = ikkeRelevantReferanse,
            begrunnelse = "test",
            harHattOrdinærSiste52Uker = false,
            harGjenværendeKvote = false,
            stansOpphør = null,
            startDato = 15 januar 2028,
        )

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(løsningForPåkrevdKrav, løsningForKravSomIkkeEksisterer)
        )

        val exception = assertThrows<UgyldigForespørselException> { løser.løs(kontekst(sakId, behandlingId), løsning) }
        assertEquals("Det finnes vurderinger for krav som ikke er relevante: ${ikkeRelevantReferanse}", exception.message)
    }

    @Test
    fun `Skal kaste feil hvis man prøver å knytte stønadsperiode mot ikke-relevant krav`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(tilleggsopplysning(behandlingId, kravreferanse))
        )

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = false,
                    harGjenværendeKvote = false,
                    stansOpphør = null,
                    startDato = 15 januar 2026,
                )
            )
        )

        val exception = assertThrows<UgyldigForespørselException> { løser.løs(kontekst(sakId, behandlingId), løsning) }
        assertEquals("Det finnes vurderinger for krav som ikke er relevante: ${kravreferanse}", exception.message)
    }
    
    @Test 
    fun `Skal kunne lagre ned vurdering og utlede ny stønadsperiode`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(
                krav(behandlingId, kravreferanse, 15 januar 2020),
            )
        )

        val løsning1 = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = false,
                    harGjenværendeKvote = false,
                    stansOpphør = null,
                    startDato = 15 januar 2020,
                ))
        )
        løser.løs(kontekst(sakId, behandlingId), løsning1)
        val grunnlag1 = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandlingId)
        assertEquals(1, grunnlag1?.vurderinger?.size)
        assertEquals(RelevantKravType.NY_STØNADSPERIODE, grunnlag1?.vurderinger?.first()?.relevantKravType)

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = true,
                    harGjenværendeKvote = false,
                    stansOpphør = StansEllerOpphørDto(
                        type = StansOpphørVurderingTypeDto.STANS,
                        årsaker = listOf(Avslagsårsak.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS)
                    ),
                    startDato = 15 januar 2020,
                ))
        )
        løser.løs(kontekst(sakId, behandlingId), løsning)
        val grunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandlingId)
        assertEquals(1, grunnlag?.vurderinger?.size)
        assertEquals(RelevantKravType.GJENOPPTAK_ETTER_STANS, grunnlag?.vurderinger?.first()?.relevantKravType)

    }
    
    @Test
    fun `Skal kunne lagre ned vurdering og utlede gjenopptak etter stans`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(
                krav(behandlingId, kravreferanse, 15 januar 2020),
            )
        )
        
        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = true,
                    harGjenværendeKvote = false,
                    stansOpphør = StansEllerOpphørDto(
                        type = StansOpphørVurderingTypeDto.STANS,
                        årsaker = listOf(Avslagsårsak.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS)
                    ),
                    startDato = 15 januar 2020,
                ))
        )
        løser.løs(kontekst(sakId, behandlingId), løsning)
        val grunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandlingId)
        assertEquals(1, grunnlag?.vurderinger?.size)
        assertEquals(RelevantKravType.GJENOPPTAK_ETTER_STANS, grunnlag?.vurderinger?.first()?.relevantKravType)
    }

    @Test
    fun `Skal kunne lagre ned vurdering og utlede gjeninntreden etter opphør`() {
        val sakId = SakId(Random.nextLong())
        val behandlingId = BehandlingId(Random.nextLong())
        val kravreferanse = Kravreferanse.ny()
        InMemoryKravRepository.lagre(
            behandlingId, setOf(
                krav(behandlingId, kravreferanse, 15 januar 2020),
            )
        )

        val løsning = AvklarStønadsperiodeLøsning(
            stønadsperiodeVurderinger = setOf(
                StønadsperiodeLøsningDto(
                    referanse = kravreferanse,
                    begrunnelse = "test",
                    harHattOrdinærSiste52Uker = false,
                    harGjenværendeKvote = true,
                    stansOpphør = StansEllerOpphørDto(
                        type = StansOpphørVurderingTypeDto.OPPHØR,
                        årsaker = listOf(Avslagsårsak.ANNEN_FULL_YTELSE)
                    ),
                    startDato = 15 januar 2020,
                ))
        )
        løser.løs(kontekst(sakId, behandlingId), løsning)
        val grunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandlingId)
        assertEquals(1, grunnlag?.vurderinger?.size)
        assertEquals(RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR, grunnlag?.vurderinger?.first()?.relevantKravType)
    }


    private fun krav(behandlingId: BehandlingId, referanse: Kravreferanse, søknadsdato: LocalDate): RelevantKrav {
        return RelevantKrav(
            referanse = referanse,
            journalpostId = JournalpostId("JP123"),
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Test",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            søknadsdato = Søknadsdato(søknadsdato, SøknadsdatoÅrsak.SøknadMottatt),
            overstyrMuligRettFra = null,
            muligRettFra = søknadsdato,
        )
    }

    private fun tilleggsopplysning(behandlingId: BehandlingId, referanse: Kravreferanse): Tilleggsopplysning {
        return Tilleggsopplysning(
            referanse = referanse,
            journalpostId = JournalpostId("JP123"),
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Test",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }

    private fun kontekst(sakId: SakId, behandlingId: BehandlingId): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            Bruker("Z123456"),
            FlytKontekst(sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling)
        )
}