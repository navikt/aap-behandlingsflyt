#!/usr/bin/env bash
# agentStop-hook: når agenten vil avslutte turen, kompiler modulene med endrede Kotlin-filer. Feiler kompileringen,
# svarer hooken «block» med feilmeldingen, så agenten retter feilene før den sier seg ferdig. stop_hook_active
# hindrer løkke. Krever jq. Avslutter alltid med 0: feil i miljøet (ingen Java, ingen jq) skal ikke blokkere.
command -v jq >/dev/null 2>&1 || exit 0
input=$(cat)
if [[ "$(jq -r '.stop_hook_active // .stopHookActive // false' <<<"$input" 2>/dev/null)" == "true" ]]; then exit 0; fi
dir=$(jq -r '.cwd // empty' <<<"$input" 2>/dev/null)
[[ -n "$dir" ]] && cd "$dir" 2>/dev/null
rot=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$rot" && [[ -x ./gradlew ]] || exit 0
mods=$(git -c core.quotePath=false status --porcelain -- '*.kt' '*.kts' 2>/dev/null | awk '{print $NF}' | cut -d/ -f1 | sort -u | grep -E '^(app|api|behandlingsflyt|repository|lib-test|kontrakt)$' || true)
[[ -z "$mods" ]] && exit 0
# Uten Java kan ikke Gradle kjøre; det er miljøet, ikke koden. Ikke blokker.
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then exit 0; fi
tasks=""
for m in $mods; do tasks="$tasks :$m:compileKotlin :$m:compileTestKotlin"; done
out=$(./gradlew -q $tasks 2>&1) && exit 0
grunn="Kompilering feilet for endrede moduler ($(echo $mods)). Rett feilene før du avslutter:
$(tail -n 40 <<<"$out")"
jq -n --arg r "$grunn" '{decision: "block", reason: $r, hookSpecificOutput: {hookEventName: "Stop", decision: "block", reason: $r}}'
exit 0
