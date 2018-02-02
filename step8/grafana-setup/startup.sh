#!/usr/bin/env bash
set -x

if [ -z "${SERVICE_GRAFANA_USERNAME}" ] ; then
  GRAFANA_AUTH=""
else
  GRAFANA_AUTH="${SERVICE_GRAFANA_USERNAME}:${SERVICE_GRAFANA_PASSWORD}@"
fi
GRAFANA_URL=http://${GRAFANA_AUTH}${SERVICE_GRAFANA_HOST}:${SERVICE_GRAFANA_PORT}

function waitForGrafana {
    while : ;
    do
        curl ${GRAFANA_URL} --output /dev/null
        if [ $? -eq 0 ] ; then
            break;
        fi
        sleep 1
    done
}

waitForGrafana

mkdir /grafana

for datasource in /datasources/*json ; do
    curl -XPOST ${GRAFANA_URL}/api/datasources/ -H 'Content-Type: application/json;charset=UTF-8' --output /dev/null -d @${datasource}
done

for alertChannel in /alert-channels/*json ; do
    curl -XPOST ${GRAFANA_URL}/api/alert-notifications/ -H 'Content-Type: application/json;charset=UTF-8' --output /dev/null -d @${alertChannel}
done

for dashboard in /dashboards/*json ; do
    curl -XPOST ${GRAFANA_URL}/api/dashboards/db/ -H 'Content-Type: application/json;charset=UTF-8' --output /dev/null -d @${dashboard}
done