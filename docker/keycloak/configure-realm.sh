#!/usr/bin/env bash
# Configures the local Keycloak realm for the web UI. Safe to run repeatedly:
# every object is looked up first and only created when missing.
#
#   docker/keycloak/configure-realm.sh --dry-run   show what would change
#   docker/keycloak/configure-realm.sh             apply it
#
# Reads KEYCLOAK_ISSUER_URI, KEYCLOAK_ADMIN_USERNAME, KEYCLOAK_ADMIN_PASSWORD,
# QUOTE_JWT_AUDIENCE and CUSTOMER_JWT_AUDIENCE from the environment, falling back
# to the repository .env. Optional: UI_CLIENT_ID (insure-ui), UI_ORIGIN
# (http://localhost:3174), AGENT_USERNAME (agent1), AGENT_PASSWORD (generated and
# printed once when the user is created).
set -euo pipefail

DRY_RUN=0
[ "${1:-}" = "--dry-run" ] && DRY_RUN=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

env_value() {
    local name="$1"
    if [ -n "${!name:-}" ]; then
        printf '%s' "${!name}"
    elif [ -f "$ROOT/.env" ]; then
        grep -m1 "^${name}=" "$ROOT/.env" | cut -d= -f2- || true
    fi
}

ISSUER="$(env_value KEYCLOAK_ISSUER_URI)"
ADMIN_USER="$(env_value KEYCLOAK_ADMIN_USERNAME)"
ADMIN_PASSWORD="$(env_value KEYCLOAK_ADMIN_PASSWORD)"
QUOTE_AUDIENCE="$(env_value QUOTE_JWT_AUDIENCE)"
CUSTOMER_AUDIENCE="$(env_value CUSTOMER_JWT_AUDIENCE)"
UI_CLIENT_ID="${UI_CLIENT_ID:-insure-ui}"
UI_ORIGIN="${UI_ORIGIN:-http://localhost:3174}"
AGENT_USERNAME="${AGENT_USERNAME:-agent1}"

for name in ISSUER ADMIN_USER ADMIN_PASSWORD QUOTE_AUDIENCE CUSTOMER_AUDIENCE; do
    [ -n "${!name}" ] || { echo "Missing setting: $name" >&2; exit 1; }
done

BASE="${ISSUER%/realms/*}"
REALM="${ISSUER##*/}"
ADMIN="$BASE/admin/realms/$REALM"
SCOPES=(quote:create quote:read customer:create customer:read)
ROLES=(agent customer)

say() { printf '%s\n' "$*"; }

# The password travels on stdin, never on the command line.
TOKEN="$(
    printf 'grant_type=password&client_id=admin-cli&username=%s&password=%s' \
        "$(jq -rn --arg v "$ADMIN_USER" '$v|@uri')" "$(jq -rn --arg v "$ADMIN_PASSWORD" '$v|@uri')" |
        curl -fsS -X POST "$BASE/realms/master/protocol/openid-connect/token" \
            -H 'Content-Type: application/x-www-form-urlencoded' --data @- | jq -r .access_token
)"
[ -n "$TOKEN" ] && [ "$TOKEN" != null ] || { echo "Could not sign in to Keycloak as the admin user" >&2; exit 1; }

# Authorization header goes through a curl config on stdin, keeping the token out of ps.
kc() {
    local method="$1" path="$2"; shift 2
    printf 'header = "Authorization: Bearer %s"\n' "$TOKEN" |
        curl -fsS -K - -X "$method" "$ADMIN$path" -H 'Content-Type: application/json' "$@"
}

mutate() {
    local description="$1"; shift
    if [ "$DRY_RUN" = 1 ]; then
        say "would $description"
    else
        "$@" >/dev/null
        say "done: $description"
    fi
}

# --- realm branding ---
REALM_BRANDING='{"displayName":"Insure AI","loginTheme":"insure-ai"}'
if [ "$(kc GET "" | jq -c '{displayName, loginTheme}')" = "$REALM_BRANDING" ]; then
    say "exists: realm display name and login theme"
else
    mutate "set realm display name Insure AI and login theme insure-ai" kc PUT "" -d "$REALM_BRANDING"
fi

# --- realm roles ---
for role in "${ROLES[@]}"; do
    if kc GET "/roles/$role" >/dev/null 2>&1; then
        say "exists: realm role $role"
    else
        mutate "create realm role $role" kc POST /roles -d "$(jq -n --arg n "$role" '{name:$n}')"
    fi
done

# --- client scopes ---
for scope in "${SCOPES[@]}"; do
    if [ -n "$(kc GET /client-scopes | jq -r --arg n "$scope" '.[]|select(.name==$n)|.id')" ]; then
        say "exists: client scope $scope"
    else
        mutate "create client scope $scope" kc POST /client-scopes -d "$(jq -n --arg n "$scope" '{
            name:$n, protocol:"openid-connect",
            attributes:{"include.in.token.scope":"true","display.on.consent.screen":"false"}}')"
    fi
done

# --- UI client ---
CLIENT_JSON="$(jq -n --arg id "$UI_CLIENT_ID" --arg origin "$UI_ORIGIN" '{
    clientId:$id, name:"Insure AI web UI", protocol:"openid-connect", enabled:true,
    publicClient:true, standardFlowEnabled:true, directAccessGrantsEnabled:false,
    implicitFlowEnabled:false, serviceAccountsEnabled:false,
    redirectUris:[($origin + "/auth/callback")], webOrigins:[$origin],
    attributes:{"pkce.code.challenge.method":"S256","post.logout.redirect.uris":($origin + "/")}}')"
CLIENT_UUID="$(kc GET "/clients?clientId=$UI_CLIENT_ID" | jq -r '.[0].id // empty')"
if [ -n "$CLIENT_UUID" ]; then
    CURRENT_ORIGINS="$(kc GET "/clients/$CLIENT_UUID" | jq -c '[.redirectUris, .webOrigins]')"
    WANTED_ORIGINS="$(jq -nc --arg origin "$UI_ORIGIN" '[[($origin + "/auth/callback")], [$origin]]')"
    if [ "$CURRENT_ORIGINS" = "$WANTED_ORIGINS" ]; then
        say "exists: client $UI_CLIENT_ID"
    else
        mutate "update client $UI_CLIENT_ID redirect and web origin to $UI_ORIGIN" kc PUT "/clients/$CLIENT_UUID" -d "$CLIENT_JSON"
    fi
else
    mutate "create public client $UI_CLIENT_ID (PKCE S256, redirect $UI_ORIGIN/auth/callback)" kc POST /clients -d "$CLIENT_JSON"
    CLIENT_UUID="$(kc GET "/clients?clientId=$UI_CLIENT_ID" | jq -r '.[0].id // empty')"
fi

if [ -n "$CLIENT_UUID" ]; then
    # default client scopes
    ASSIGNED="$(kc GET "/clients/$CLIENT_UUID/default-client-scopes" | jq -r '.[].name')"
    for scope in "${SCOPES[@]}"; do
        if printf '%s\n' "$ASSIGNED" | grep -qx "$scope"; then
            say "exists: $scope assigned to $UI_CLIENT_ID"
        else
            SCOPE_ID="$(kc GET /client-scopes | jq -r --arg n "$scope" '.[]|select(.name==$n)|.id')"
            mutate "assign scope $scope to $UI_CLIENT_ID" kc PUT "/clients/$CLIENT_UUID/default-client-scopes/$SCOPE_ID"
        fi
    done

    # audience mappers
    MAPPERS="$(kc GET "/clients/$CLIENT_UUID" | jq -r '.protocolMappers[]?.name')"
    for audience in "$QUOTE_AUDIENCE" "$CUSTOMER_AUDIENCE"; do
        name="audience-$audience"
        if printf '%s\n' "$MAPPERS" | grep -qx "$name"; then
            say "exists: audience mapper $name"
        else
            mutate "add audience mapper $name" kc POST "/clients/$CLIENT_UUID/protocol-mappers/models" -d "$(jq -n --arg n "$name" --arg a "$audience" '{
                name:$n, protocol:"openid-connect", protocolMapper:"oidc-audience-mapper",
                config:{"included.custom.audience":$a,"access.token.claim":"true","id.token.claim":"false","introspection.token.claim":"true"}}')"
        fi
    done
fi

# --- test agent ---
create_agent() {
    kc POST /users -d "$(jq -n --arg u "$AGENT_USERNAME" --arg p "$AGENT_PASSWORD" '{
        username:$u, enabled:true, emailVerified:true, firstName:"Local", lastName:"Agent",
        email:($u + "@example.test"), credentials:[{type:"password", value:$p, temporary:false}]}')"
    local id
    id="$(kc GET "/users?username=$AGENT_USERNAME&exact=true" | jq -r '.[0].id')"
    kc POST "/users/$id/role-mappings/realm" -d "[$(kc GET /roles/agent)]"
}

AGENT_ID="$(kc GET "/users?username=$AGENT_USERNAME&exact=true" | jq -r '.[0].id // empty')"
if [ -n "$AGENT_ID" ]; then
    say "exists: user $AGENT_USERNAME"
else
    GENERATED=0
    if [ -z "${AGENT_PASSWORD:-}" ]; then
        AGENT_PASSWORD="$(openssl rand -hex 16)"
        GENERATED=1
    fi
    mutate "create user $AGENT_USERNAME with role agent" create_agent
    if [ "$DRY_RUN" = 0 ] && [ "$GENERATED" = 1 ]; then
        say "password for $AGENT_USERNAME (shown once): $AGENT_PASSWORD"
    fi
fi
