export NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_HOST=localhost # Må her sette host og port fra ENV som service container setter
export NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PORT=35123
export NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_DATABASE=test
export NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_USERNAME=test
export NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PASSWORD=test

export AZURE_OPENID_CONFIG_TOKEN_ENDPOINT=http://localhost
export AZURE_APP_CLIENT_ID=behandlingsflyt
export AZURE_APP_CLIENT_SECRET=""
export AZURE_OPENID_CONFIG_JWKS_URI=http://localhost
export AZURE_OPENID_CONFIG_ISSUER=behandlingsflyt

gradle run runTestApp &
FOO_PID=$!

timeout 30 bash -c 'while [[ "$(curl --insecure -s -o /dev/null -w ''%{http_code}'' http://localhost:8080/actuator/live)" != "200" ]]; do sleep 2; done' || false
wget http://localhost:8080/openapi.json

kill $FOO_PID