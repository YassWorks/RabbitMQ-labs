# TP1 RabbitMQ - Docker Setup

This setup replaces the old local installation flow (Erlang + RabbitMQ `.exe`) with Docker.

## What matches TP1 requirements

- RabbitMQ broker running for producers/consumers
- Management web interface enabled
- Default credentials to access UI and AMQP

## 1) Start RabbitMQ with Docker

```bash
docker compose -f compose.yml up -d
```

RabbitMQ management UI: http://localhost:15672  
Login: `student` / `student`

AMQP endpoint used by Java code: `localhost:5672`

## 2) Build Java examples

```bash
mvn -q compile
```

Optional: watch broker logs while working.

```bash
docker compose -f compose.yml logs -f rabbitmq
```

## 3) TP step: send hello message

```bash
mvn -q exec:java '-Dexec.mainClass=tp1.Send' '-Dexec.args=Hello from TP1'
```

## 4) TP step: receive/consume message

In a second terminal:

```bash
mvn -q exec:java '-Dexec.mainClass=tp1.Receive'
```

## 5) TP step: direct exchange + routing

Start a consumer that listens to selected severities:

```bash
mvn -q exec:java '-Dexec.mainClass=tp1.ReceiveLogsDirect' '-Dexec.args=error warning'
```

Publish messages:

```bash
mvn -q exec:java '-Dexec.mainClass=tp1.EmitLogDirect' '-Dexec.args=error Disk full'
mvn -q exec:java '-Dexec.mainClass=tp1.EmitLogDirect' '-Dexec.args=info All good'
```

## 6) Stop stack

```bash
docker compose -f compose.yml down
```

To remove persisted RabbitMQ data too:

```bash
docker compose -f compose.yml down -v
```

If you start with `docker compose up` (without `-d`) and then press Ctrl+C, RabbitMQ will stop and can show an exit code like 137. For lab work, prefer detached mode (`up -d`).
