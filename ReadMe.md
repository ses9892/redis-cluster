# Redis Cluster with Spring Boot Architecture

```mermaid
graph TB
    subgraph "Load Balanced Spring Boot Apps"
        LB[Load Balancer]
        APP1[Spring Boot App 1:8090]
        APP2[Spring Boot App 2:8090]
        LB --> APP1
        LB --> APP2
    end

    subgraph "Redis Cluster (Docker Network: 172.30.0.0/16)"
        subgraph "Master Nodes"
            M1[Master 1\n172.29.3.164:6379\n172.30.0.2]
            M2[Master 2\n172.29.3.164:6380\n172.30.0.3]
            M3[Master 3\n172.29.3.164:6381\n172.30.0.4]
        end
        
        subgraph "Slave Nodes"
            S1[Slave 1\n172.29.3.164:6382\n172.30.0.5]
            S2[Slave 2\n172.29.3.164:6383\n172.30.0.6]
            S3[Slave 3\n172.29.3.164:6384\n172.30.0.7]
        end

        M1 --- S1
        M2 --- S2
        M3 --- S3
        
        M1 -.-> M2
        M2 -.-> M3
        M3 -.-> M1
    end

    APP1 --> M1
    APP1 --> M2
    APP1 --> M3
    APP2 --> M1
    APP2 --> M2
    APP2 --> M3
```

```mermaid
sequenceDiagram
    participant App as Spring Boot App
    participant M1 as Master Node
    participant S1 as Slave Node
    
    App->>M1: Normal Operation
    Note over M1: Node Failure
    S1->>S1: Promotion to Master
    Note over App: Auto-reconnect
    App->>S1: Continue Operation
```

## Key Features
- Automatic failover between master and slave nodes
- Load balanced Spring Boot applications
- Docker containerized Redis nodes
- Cluster-aware Spring Boot configuration
- High availability with master-slave replication

## Node Mapping
| Role | Port | Container IP | External IP |
|------|------|-------------|-------------|
| Master 1 | 6379 | 172.30.0.2 | 172.29.3.164 |
| Master 2 | 6380 | 172.30.0.3 | 172.29.3.164 |
| Master 3 | 6381 | 172.30.0.4 | 172.29.3.164 |
| Slave 1 | 6382 | 172.30.0.5 | 172.29.3.164 |
| Slave 2 | 6383 | 172.30.0.6 | 172.29.3.164 |
| Slave 3 | 6384 | 172.30.0.7 | 172.29.3.164 |
```