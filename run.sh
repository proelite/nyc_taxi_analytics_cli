#!/usr/bin/env bash
set -euo pipefail

# Regex for strict timestamp: YYYY-MM-DD HH:MM:SS
TIMESTAMP_RE='^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'

# ——— Main prompts ———
while true; do
  read -r -p "Enter pickup datetime (YYYY-MM-DD HH:MM:SS) [no pickup time lower bound]:" pickup_datetime

  # Trim leading/trailing whitespace
  pickup_datetime="${pickup_datetime#"${pickup_datetime%%[![:space:]]*}"}"
  pickup_datetime="${pickup_datetime%"${pickup_datetime##*[![:space:]]}"}"

  if [[ -z $pickup_datetime ]]; then
    pickup_datetime="*"
    break
  elif [[ $pickup_datetime =~ $TIMESTAMP_RE ]]; then
    break
  else
    echo "  ↳ Not a valid timestamp. Try again or leave blank."
  fi
done

while true; do
  read -r -p "Enter dropoff datetime (YYYY-MM-DD HH:MM:SS) [no dropoff time uppper bound]:" dropoff_datetime

  # Trim leading/trailing whitespace
  dropoff_datetime="${dropoff_datetime#"${dropoff_datetime%%[![:space:]]*}"}"
  dropoff_datetime="${dropoff_datetime%"${dropoff_datetime##*[![:space:]]}"}"

  if [[ -z $dropoff_datetime ]]; then
    dropoff_datetime="*"
    break
  elif [[ $dropoff_datetime =~ $TIMESTAMP_RE ]]; then
    break
  else
    echo "  ↳ Not a valid timestamp. Try again or leave blank."
  fi
done

while true; do
  read -r -p "Enter pickup location ID (integer) [no pickup specified]:" pu_location_id

  # Trim leading/trailing whitespace
  pu_location_id="${pu_location_id#"${pu_location_id%%[![:space:]]*}"}"
  pu_location_id="${pu_location_id%"${pu_location_id##*[![:space:]]}"}"

  if [[ -z $pu_location_id ]]; then
    pu_location_id="*"
    break
  elif [[ $pu_location_id =~ ^[0-9]+$ ]]; then
    break
  else
    echo "  ↳ Not a valid integer. Enter a number or leave blank."
  fi
done

while true; do
  read -r -p "Enter dropoff location ID (integer) [no dropoff specified]:" do_location_id

  # Trim leading/trailing whitespace
  do_location_id="${do_location_id#"${do_location_id%%[![:space:]]*}"}"
  do_location_id="${do_location_id%"${do_location_id##*[![:space:]]}"}"

  if [[ -z $do_location_id ]]; then
    do_location_id="*"
    break
  elif [[ $do_location_id =~ ^[0-9]+$ ]]; then
    break
  else
    echo "  ↳ Not a valid integer. Enter a number or leave blank."
  fi
done

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
  read -r -p "Enter vendor ID (integer) [all vendors]:" vendor_id

  # Trim leading/trailing whitespace
  vendor_id="${vendor_id#"${vendor_id%%[![:space:]]*}"}"
  vendor_id="${vendor_id%"${vendor_id##*[![:space:]]}"}"

  if [[ -z $vendor_id ]]; then
    vendor_id="*"
    break
  elif [[ $vendor_id =~ ^[0-9]+$ ]]; then
    break
  else
    echo "  ↳ Not a valid integer. Enter a number or leave blank."
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
