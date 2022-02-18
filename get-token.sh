#!/usr/bin/env bash

curl -X POST -H "Content-Type: text/json" \
  -d '{"token":"1234567","principal":"object-store","permissions":[{"resourceType":"tax-fraud-reporting","resourceLocation":"~/workspace/tax-fraud-reporting","actions":["READ"]}]}' \
  http://localhost:8470/test-only/token