#!/usr/bin/env bash
set -euo pipefail

# Regex for strict timestamp: YYYY-MM-DD HH:MM:SS
TIMESTAMP_RE='^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'

prompt_timestamp() {
  local prompt="$1" val
  while true; do
    read -r -p "$prompt (YYYY-MM-DD HH:MM:SS): " val
    if [[ $val =~ $TIMESTAMP_RE ]]; then
      echo "$val"; return
    else
      echo "  ↳ Invalid format. Please try again."
    fi
  done
}

prompt_int() {
  local prompt="$1" val
  while true; do
    read -r -p "$prompt (integer): " val
    if [[ $val =~ ^[0-9]+$ ]]; then
      echo "$val"; return
    else
      echo "  ↳ Not a valid integer. Please try again."
    fi
  done
}

# ——— Main prompts ———
pickup_datetime=$(prompt_timestamp "Enter pickup datetime")
dropoff_datetime=$(prompt_timestamp "Enter dropoff datetime")
pu_location_id=$(prompt_int      "Enter pickup location ID")
do_location_id=$(prompt_int      "Enter dropoff location ID")

# Group‐by‐payment‐type flag (default “true”)
while true; do
  read -r -p "Group by payment type? (true/false) [true]: " group_by_payment_type
  group_by_payment_type=${group_by_payment_type:-true}
  group_by_payment_type=$(printf '%s' "$group_by_payment_type" | tr '[:upper:]' '[:lower:]')

  case "$group_by_payment_type" in
    true|false)
      break
      ;;
    *)
      echo "  ↳ Please enter true or false."
      ;;
  esac
done

while true; do
  read -r -p "Enter vendor ID (integer, or leave blank for all vendors): " vendor_id
  if [[ -z $vendor_id ]]; then
    vendor_id="*"
    break
  elif [[ $vendor_id =~ ^[0-9]+$ ]]; then
    break
  else
    echo "  ↳ Not a valid integer. Enter a number or leave blank for all."
  fi
done

# Taxi type (default “both”)
while true; do
  read -r -p "Taxi type (yellow, green, both) [both]: " taxi_type
  taxi_type=${taxi_type:-both}
  taxi_type=$(printf '%s' "$taxi_type" | tr '[:upper:]' '[:lower:]')

  case "$taxi_type" in
    yellow|green)
      break
      ;;
    both)
      taxi_type="*"
      break
      ;;
    *)
      echo "  ↳ Must be one of: yellow, green, both."
      ;;
  esac
done

# ——— Summary ———
echo
echo "▶ Parameters:"
echo "  pickup:               $pickup_datetime"
echo "  dropoff:              $dropoff_datetime"
echo "  pickup location ID:   $pu_location_id"
echo "  dropoff location ID:  $do_location_id"
echo "  group by payment type: $group_by_payment_type"
echo "  vendor ID:            $vendor_id"
echo "  taxi type:            $taxi_type"
echo

# ——— Invoke Gradle ETL ———
GRADLE_CMD="./gradlew"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

echo "⏳ Running ETL..."
$GRADLE_CMD executeQuery \
  --args="\"$pickup_datetime\" \"$dropoff_datetime\" $pu_location_id $do_location_id $group_by_payment_type $vendor_id $taxi_type"

echo "✅ Done."
