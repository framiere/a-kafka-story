FROM everpeace/curl-jq

ENV SERVICE_GRAFANA_HOST grafana
ENV SERVICE_GRAFANA_PORT 3000
ENV SERVICE_GRAFANA_USERNAME "admin"
ENV SERVICE_GRAFANA_PASSWORD "admin"

COPY ./startup.sh /usr/bin/startup.sh
RUN chmod +x /usr/bin/startup.sh

RUN mkdir /datasources
COPY ./datasources/*.json /datasources/

RUN mkdir /alert-channels
COPY ./alert-channels/*.json /alert-channels/

RUN mkdir /dashboards
COPY ./dashboards/*.json /dashboards/

CMD ["/usr/bin/startup.sh"]