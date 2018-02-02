sDo your shopping at https://grafana.com/dashboards and save the json in this folder

Please remove the 

```
  "__inputs": [
    {
      "name": "influxdb",
      "label": "InfluxDB_telegraf",
      "description": "",
      "type": "datasource",
      "pluginId": "influxdb",
      "pluginName": "InfluxDB"
    }
  ],
```

and replace all the `${influxdb}` by your corresponding datasource name