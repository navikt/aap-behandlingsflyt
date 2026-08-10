-- Skal ha index på neste kjøring for jobber som allerede er skjedulert og har status KLAR
-- for å effektivt ekskludere disse ved skjedulering av jobber
create index IDX_JOBB_PLUKKBAR
    on jobb(neste_kjoring) where jobb.kjorbar and jobb.status = 'KLAR';

--Både indeks og sikkerhetsnett for at man ikke har to kjørbare jobber som kolliderer med hverandre
CREATE UNIQUE INDEX UX_JOBB_EKSKLUSIV_AKTIV ON JOBB (COALESCE(SAK_ID, -1), COALESCE(BEHANDLING_ID, -1), TYPE)
    WHERE (STATUS = 'FEILET' OR (STATUS = 'KLAR' AND KJORBAR))
        AND (SAK_ID IS NOT NULL OR BEHANDLING_ID IS NOT NULL);
