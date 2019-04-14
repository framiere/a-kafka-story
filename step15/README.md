# Objective 

Create Streams, query streams from UI

Access the Control Center UI at http://localhost:9021

On the left menu click on KSQL
![ksql](./ksql-ui.png "ksql")

Click `streams` tab, then on `Add a stream` Button

![create-streams](./create-streams.png "Create Streams")`

Select the source topic for the stream

![streams-topic](./streams-topic.png "Choose topic")

Configure the Stream

**IMPORTANT: Don't forget to fill `How are your message encoded?``**

Note that Control Center discovered all the field !

![streams-field](./streams-field.png "Configure stream")

Let's query the result !

![run-query](./streams-result.png "Run a query")