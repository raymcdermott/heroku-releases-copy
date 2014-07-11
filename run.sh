#!/bin/bash

export HEROKU_API_TOKEN=689aaf7e-b9e2-48f4-9816-39288b67cbc8
export HEROKU_RELEASE_TARGET_ORG=tme-web-dev
export HEROKU_RELEASE_SOURCE_APP=ngx-prod-clone-pagespeed
export HEROKU_RELEASE_TARGET_APPS=bamboo-deploy-target2

lein run -m heroku-releases.copy-slugs


