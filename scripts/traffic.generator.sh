#!/bin/bash

URL="http://localhost:30080/tasks" # adapt the URL

echo "🚀 Starting Chaos Traffic Generator targeting: $URL"
echo "Press [CTRL+C] to stop..."

while true; do

  CHANCE=$((1 + RANDOM % 10))

  if [ $CHANCE -le 7 ]; then
    TITLE="Task_$(date +%s)"
    # Valid Future Date
    DATE="2099-01-01T10:00:00"

    # Send Request
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL" \
      -H "Content-Type: application/json" \
      -d "{\"title\": \"$TITLE\", \"dueDate\": \"$DATE\"}")

    echo "[SUCCESS] Created valid task. Status: $HTTP_CODE"

  # 30% Chance of Failure (BValidation Error)
  else
    # Scenario: User forgets the title (Bad Request)
    TITLE=""
    DATE="2099-01-01T10:00:00"

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL" \
      -H "Content-Type: application/json" \
      -d "{\"title\": \"$TITLE\", \"dueDate\": \"$DATE\"}")

    echo "[ERROR] Sent invalid data. Status: $HTTP_CODE"
  fi

  # Sleep for a random time (0.5s to 2s)
  SLEEP_TIME=$(awk -v min=0.5 -v max=2 'BEGIN{srand(); print min+rand()*(max-min)}')
  sleep $SLEEP_TIME
done