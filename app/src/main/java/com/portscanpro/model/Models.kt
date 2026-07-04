package com.portscanpro.model

data class ScanTarget(
    val ip: String,
    val hostname: String = "",
    val mac: String = "",
    val vendor: String = "",
    val isAlive: Boolean = false,
    val pingTime: Long = 0,
    val openPorts: List<PortInfo> = emptyList(),
    val osGuess: String = ""
)

data class PortInfo(
    val port: Int,
    val protocol: String = "tcp",
    val state: String = "closed",
    val service: String = "",
    val version: String = ""
)

data class DeviceInfo(
    val ip: String,
    val hostname: String,
    val mac: String,
    val vendor: String,
    val rssi: Int = 0,
    val isOnline: Boolean = false
)

data class VulnResult(
    val url: String,
    val statusCode: Int,
    val contentType: String = "",
    val contentLength: Long = 0,
    val isInteresting: Boolean = false,
    val title: String = ""
)

data class ScanSession(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String,
    val target: String,
    val duration: Long = 0,
    val hostsFound: Int = 0,
    val portsFound: Int = 0,
    val results: String = ""
)

data class ScanState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val currentTarget: String = "",
    val results: List<Any> = emptyList(),
    val message: String = ""
)

val SERVICE_PORT_MAP = mapOf(
    21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
    53 to "DNS", 80 to "HTTP", 110 to "POP3", 111 to "RPC",
    135 to "MSRPC", 139 to "NetBIOS", 143 to "IMAP",
    389 to "LDAP", 443 to "HTTPS", 445 to "SMB",
    464 to "Kerberos", 465 to "SMTPS", 514 to "Syslog",
    587 to "SMTP", 636 to "LDAPS", 993 to "IMAPS",
    995 to "POP3S", 1080 to "SOCKS", 1194 to "OpenVPN",
    1352 to "Lotus Notes", 1433 to "MSSQL", 1521 to "Oracle DB",
    2049 to "NFS", 2082 to "cPanel", 2083 to "cPanel SSL",
    2181 to "ZooKeeper", 2222 to "SSH Alt", 2375 to "Docker",
    2376 to "Docker TLS", 2424 to "OrientDB", 3128 to "Squid",
    3306 to "MySQL", 3389 to "RDP", 3690 to "SVN",
    4369 to "Erlang Port Mapper", 4444 to "Metasploit",
    4489 to "VNC", 4560 to "Default", 4786 to "Cisco Smart Install",
    4848 to "GlassFish", 5000 to "Flask/Synology", 5001 to "Synology",
    5002 to "Azure", 5003 to "FileMaker", 5004 to "RTP",
    5005 to "VNC", 5006 to "WSMAN", 5044 to "Logstash",
    5048 to "Telnet", 5222 to "XMPP", 5223 to "XMPP SSL",
    5342 to "MS SQL", 5432 to "PostgreSQL", 5555 to "ADB",
    5601 to "Kibana", 5672 to "RabbitMQ", 5800 to "VNC HTTP",
    5900 to "VNC", 5901 to "VNC", 5984 to "CouchDB",
    5985 to "WinRM HTTP", 5986 to "WinRM HTTPS",
    6000 to "X11", 6001 to "X11", 6379 to "Redis",
    6443 to "Kubernetes API", 6666 to "IRC", 6667 to "IRC",
    7001 to "WebLogic", 7070 to "WebSphere",
    7077 to "Spark", 8080 to "HTTP-Alt", 8081 to "HTTP Proxy",
    8082 to "HTTP", 8083 to "HTTP", 8084 to "HTTP",
    8085 to "HTTP", 8086 to "InfluxDB", 8087 to "HTTP",
    8088 to "HTTP", 8089 to "Splunk", 8090 to "HTTP",
    8091 to "Couchbase", 8123 to "Polipo", 8139 to "Puppet",
    8140 to "Puppet SSL", 8443 to "HTTPS-Alt", 8484 to "HTTP",
    8500 to "Consul", 8530 to "HTTP", 8531 to "HTTPS",
    8649 to "Ganglia", 8686 to "HTTP", 8761 to "Eureka",
    8787 to "HTTP", 8800 to "HTTP", 8834 to "Nessus",
    8843 to "HTTPS", 8880 to "HTTP", 8888 to "HTTP",
    8889 to "HTTP", 8983 to "Solr", 8990 to "HTTP",
    8991 to "HTTP", 9000 to "Portainer", 9001 to "Tor",
    9042 to "Cassandra", 9050 to "Tor", 9060 to "HTTP",
    9080 to "HTTP", 9090 to "Prometheus", 9092 to "Kafka",
    9099 to "HTTP", 9100 to "Printer", 9200 to "Elasticsearch",
    9300 to "Elasticsearch", 9418 to "Git", 9443 to "HTTPS",
    9600 to "HTTP", 9876 to "HTTP", 9999 to "HTTP",
    10000 to "Webmin", 10001 to "HTTP", 10002 to "HTTP",
    11211 to "Memcached", 11214 to "Memcached", 12000 to "HTTP",
    12345 to "Telnet", 13579 to "HTTP", 15000 to "HTTP",
    15672 to "RabbitMQ", 16010 to "HBase", 16200 to "HTTP",
    16379 to "Redis", 16579 to "HTTP", 17000 to "HTTP",
    18080 to "HTTP", 18081 to "HTTP", 18082 to "HTTP",
    18083 to "HTTP", 18084 to "HTTP", 18085 to "HTTP",
    18086 to "HTTP", 18087 to "HTTP", 18088 to "HTTP",
    18089 to "HTTP", 18090 to "HTTP", 18888 to "HTTP",
    19000 to "HTTP", 19100 to "HTTP", 19200 to "HTTP",
    19283 to "HTTP", 19300 to "HTTP", 20000 to "HTTP",
    20001 to "HTTP", 20002 to "HTTP", 20003 to "HTTP",
    20004 to "HTTP", 20005 to "HTTP", 20006 to "HTTP",
    20007 to "HTTP", 20008 to "HTTP", 20009 to "HTTP",
    20010 to "HTTP", 25565 to "Minecraft", 27017 to "MongoDB",
    27018 to "MongoDB", 27019 to "MongoDB", 28015 to "RethinkDB",
    28017 to "MongoDB", 30718 to "HTTP", 31337 to "BackOrifice",
    32400 to "Plex", 33434 to "traceroute", 37777 to "HTTP",
    40000 to "HTTP", 45000 to "HTTP", 47808 to "BACnet",
    49152 to "Windows RPC", 50000 to "HTTP", 50001 to "HTTP",
    50002 to "HTTP", 50003 to "HTTP", 50004 to "HTTP",
    50005 to "HTTP", 50006 to "HTTP", 50007 to "HTTP",
    50008 to "HTTP", 50009 to "HTTP", 50010 to "HTTP",
    61616 to "ActiveMQ", 62078 to "iPhone Sync"
)
