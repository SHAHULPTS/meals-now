#!/usr/bin/env bash
#
# smoke-test.sh — drives a full order flow so you can watch Kafka events fire.
#
# Prereqs:
#   1. docker compose up -d   (postgres + kafka healthy)
#   2. ./mvnw spring-boot:run  (app on :8080)
#   3. In a SEPARATE terminal, start the console consumer so you can watch events:
#        docker exec -it mealsnow-kafka /opt/kafka/bin/kafka-console-consumer.sh \
#          --bootstrap-server localhost:9092 --topic order-events --from-beginning
#   4. Then run:  bash smoke-test.sh
#
# It registers an admin, a vendor, and a customer; creates + approves a vendor;
# adds a menu item; places an order (PLACED event) and advances it
# ACCEPTED -> PREPARING (two more events).

set -u
BASE="http://localhost:8080"
JSON="Content-Type: application/json"

# unique-ish suffix so re-runs don't collide on email/vendor
SUF="$(date +%s)"

say() { printf "\n\033[1;36m== %s\033[0m\n" "$1"; }

register() { # email password role
  curl -s -o /dev/null -w "  register %s -> HTTP %{http_code}\n" "$3-$1" \
    -X POST "$BASE/auth/register" -H "$JSON" \
    -d "{\"email\":\"$1\",\"password\":\"$2\",\"role\":\"$3\"}"
}

login() { # email password  -> prints token
  curl -s -X POST "$BASE/auth/login" -H "$JSON" \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.token'
}

ADMIN_EMAIL="admin+$SUF@test.com"
VENDOR_EMAIL="vendor+$SUF@test.com"
CUST_EMAIL="cust+$SUF@test.com"
PW="pass1234"

say "1. Register users (admin / vendor / customer)"
register "$ADMIN_EMAIL"  "$PW" ADMIN
register "$VENDOR_EMAIL" "$PW" VENDOR
register "$CUST_EMAIL"   "$PW" CUSTOMER

say "2. Log in, capture JWTs"
ADMIN_TOKEN="$(login "$ADMIN_EMAIL" "$PW")"
VENDOR_TOKEN="$(login "$VENDOR_EMAIL" "$PW")"
CUST_TOKEN="$(login "$CUST_EMAIL" "$PW")"
echo "  admin  token: ${ADMIN_TOKEN:0:20}..."
echo "  vendor token: ${VENDOR_TOKEN:0:20}..."
echo "  cust   token: ${CUST_TOKEN:0:20}..."
[ -n "$VENDOR_TOKEN" ] && [ "$VENDOR_TOKEN" != "null" ] || { echo "LOGIN FAILED — is the app up on :8080?"; exit 1; }

say "3. Vendor applies to onboard a new vendor"
VENDOR_ID="$(curl -s -X POST "$BASE/vendors" -H "$JSON" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d "{\"name\":\"Smoke Kitchen $SUF\",\"address\":\"1 Test St\"}" | jq -r '.id')"
echo "  vendorId = $VENDOR_ID"

say "4. Admin approves the vendor"
curl -s -o /dev/null -w "  approve -> HTTP %{http_code}\n" \
  -X POST "$BASE/admin/vendors/$VENDOR_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

say "5. Vendor adds a menu item (available=true by default)"
ITEM_ID="$(curl -s -X POST "$BASE/vendors/$VENDOR_ID/menu-items" -H "$JSON" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d "{\"name\":\"Test Burger\",\"price\":9.99,\"description\":\"smoke\",\"category\":\"main\"}" | jq -r '.id')"
echo "  menuItemId = $ITEM_ID"

say "6. Customer places an order  ->  expect a PLACED event (oldStatus:null)"
ORDER_JSON="$(curl -s -X POST "$BASE/orders" -H "$JSON" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -d "{\"vendorId\":\"$VENDOR_ID\",\"items\":[{\"menuItemId\":\"$ITEM_ID\",\"quantity\":2}]}")"
ORDER_ID="$(echo "$ORDER_JSON" | jq -r '.id')"
echo "  orderId = $ORDER_ID  status = $(echo "$ORDER_JSON" | jq -r '.status')"

say "7. Vendor advances PLACED -> ACCEPTED  ->  expect an ACCEPTED event"
curl -s -X POST "$BASE/orders/$ORDER_ID/status" -H "$JSON" \
  -H "Authorization: Bearer $VENDOR_TOKEN" -d '{"target":"ACCEPTED"}' | jq -r '"  now: " + .status'

sleep 1
say "8. Vendor advances ACCEPTED -> PREPARING  ->  expect a PREPARING event"
curl -s -X POST "$BASE/orders/$ORDER_ID/status" -H "$JSON" \
  -H "Authorization: Bearer $VENDOR_TOKEN" -d '{"target":"PREPARING"}' | jq -r '"  now: " + .status'

say "Done. Check your console-consumer terminal — you should see 3 JSON events:"
echo "  PLACED (oldStatus:null) -> ACCEPTED -> PREPARING, all keyed by orderId $ORDER_ID"
