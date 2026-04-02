# Signomix Collector

Signomix Collector is a software component that collects data from various sources and sends it to the Signomix platform for processing and analysis. It is designed to be flexible and can be configured to collect data from a wide range of sources

Collector subscribes to signomix-broker MQTT topics and runs methods collecting data in ressponse to received messages. Several methods are implemented:
- receiving hotel reservation data
- store Signomix twin object representing a hotel room (twin is a digital representation of a physical object, in this case, a hotel room).
