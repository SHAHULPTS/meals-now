#!/usr/bin/env bash
# live-demo.sh — creates the fixed customer + a vendor/menu, PAUSES so you can
# connect the browser, then places an order and advances it slowly so every
# push lands live in track.html.
#
# 1) app running   2) run this script   3) when it pauses, open
#    http://localhost:8080/track.html and click "Log in & connect"
#    then come back and press Enter.
set -u
BASE="http://localhost:8080"; JSON="Content-Type: application/json"
SUF="$(date +%s)"; PW="pass1234"
CUST="live@test.com"                      # <-- matches track.html's default
ADMIN="admin+$SUF@test.com"; VEND="vendor+$SUF@test.com"

reg(){ curl -s -o /dev/null -X POST "$BASE/auth/register" -H "$JSON" -d "{\"email\":\"$1\",\"password\":\"$2\",\"role\":\"$3\"}"; }
login(){ curl -s -X POST "$BASE/auth/login" -H "$JSON" -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.token'; }

echo "Setting up admin, vendor, and the fixed customer ($CUST)…"
reg "$ADMIN" "$PW" ADMIN; reg "$VEND" "$PW" VENDOR; reg "$CUST" "$PW" CUSTOMER
AT=$(login "$ADMIN" "$PW"); VT=$(login "$VEND" "$PW"); CT=$(login "$CUST" "$PW")
[ -n "$CT" ] && [ "$CT" != null ] || { echo "login failed — app up on :8080?"; exit 1; }

VID=$(curl -s -X POST "$BASE/vendors" -H "$JSON" -H "Authorization: Bearer $VT" -d "{\"name\":\"Live Kitchen $SUF\",\"address\":\"1 Live St\"}" | jq -r '.id')
curl -s -o /dev/null -X POST "$BASE/admin/vendors/$VID/approve" -H "Authorization: Bearer $AT"
IID=$(curl -s -X POST "$BASE/vendors/$VID/menu-items" -H "$JSON" -H "Authorization: Bearer $VT" -d "{\"name\":\"Live Burger\",\"price\":9.99,\"description\":\"x\",\"category\":\"main\"}" | jq -r '.id')

echo ""
echo "============================================================"
echo " Customer '$CUST' now exists. Open the browser:"
echo "   http://localhost:8080/track.html  ->  Log in & connect"
echo " Wait for the green 'Connected' status, then come back here."
echo "============================================================"
read -r -p "Press Enter once the browser shows Connected… "

echo "Placing order as $CUST — watch the browser…"
OID=$(curl -s -X POST "$BASE/orders" -H "$JSON" -H "Authorization: Bearer $CT" -d "{\"vendorId\":\"$VID\",\"items\":[{\"menuItemId\":\"$IID\",\"quantity\":1}]}" | jq -r '.id')
echo "orderId=$OID (should appear as PLACED in the browser)"

for S in ACCEPTED PREPARING READY OUT_FOR_DELIVERY DELIVERED; do
  sleep 3
  echo "advancing -> $S"
  curl -s -o /dev/null -X POST "$BASE/orders/$OID/status" -H "$JSON" -H "Authorization: Bearer $VT" -d "{\"target\":\"$S\"}"
done
echo "Done — browser should have shown PLACED -> ACCEPTED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED, live."
